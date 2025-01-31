/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.data.io

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document.*
import de.lukaspieper.truvark.common.data.io.DirectoryInfo
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.data.io.FileSystem
import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.asLog
import de.lukaspieper.truvark.common.logging.logcat
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A [FileSystem] implementation for Android's Storage Access Framework (content:// URIs).
 */
class AndroidFileSystem(private val context: Context) : FileSystem() {

    fun appFilesDir(): File {
        return context.filesDir
    }

    fun takePersistableUriPermission(uri: Uri) {
        require(uri != Uri.EMPTY) { "uri must not be empty" }

        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    fun fileInfo(uri: Uri): FileInfo {
        require(uri != Uri.EMPTY) { "uri must not be empty" }

        context.contentResolver.query(
            uri,
            arrayOf(
                COLUMN_DISPLAY_NAME,
                COLUMN_MIME_TYPE,
                COLUMN_SIZE
            )
        ) { cursor ->
            if (cursor?.moveToFirst() == true && cursor.getString(COLUMN_MIME_TYPE) != MIME_TYPE_DIR) {
                val mimeType = cursor.getString(COLUMN_MIME_TYPE)
                var mediaDuration: Duration? = null

                if (mimeType.startsWith("video/") || mimeType.startsWith("audio/")) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        mediaDuration = retriever.extractMetadata(METADATA_KEY_DURATION)?.toLongOrNull()?.milliseconds
                    } catch (e: Exception) {
                        logcat(LogPriority.WARN) { e.asLog() }
                    } finally {
                        retriever.release()
                    }
                }

                return FileInfo(
                    uri = uri,
                    fullName = cursor.getString(COLUMN_DISPLAY_NAME),
                    mimeType = cursor.getString(COLUMN_MIME_TYPE),
                    size = cursor.getLong(COLUMN_SIZE),
                    mediaDuration = mediaDuration
                )
            }
        }

        throw FileNotFoundException()
    }

    @Throws(Exception::class)
    fun directoryInfo(treeUri: Uri): DirectoryInfo {
        require(treeUri != Uri.EMPTY) { "uri must not be empty" }

        val uri = convertTreeUriToDocumentUri(treeUri)

        context.contentResolver.query(
            uri,
            arrayOf(
                COLUMN_DOCUMENT_ID,
                COLUMN_DISPLAY_NAME,
                COLUMN_MIME_TYPE
            )
        ) { cursor ->
            if (cursor?.moveToFirst() == true && cursor.getString(COLUMN_MIME_TYPE) == MIME_TYPE_DIR) {
                return DirectoryInfo(
                    uri = DocumentsContract.buildDocumentUriUsingTree(
                        uri,
                        cursor.getString(COLUMN_DOCUMENT_ID)
                    ),
                    name = cursor.getString(COLUMN_DISPLAY_NAME)
                )
            }
        }

        throw FileNotFoundException()
    }

    private fun convertTreeUriToDocumentUri(uri: Uri): Uri {
        val documentId = when {
            DocumentsContract.isDocumentUri(context, uri) -> DocumentsContract.getDocumentId(uri)
            else -> DocumentsContract.getTreeDocumentId(uri)
        }

        return DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
    }

    @Throws(Exception::class)
    override fun createFile(directoryInfo: DirectoryInfo, name: String, mimeType: String): FileInfo {
        val uri = directoryInfo.uri as Uri
        if (findFileOrNull(directoryInfo, name) != null) throw IOException("File already exists")

        val fileUri = DocumentsContract.createDocument(context.contentResolver, uri, mimeType, name)
            ?: throw IOException("Could not create file")

        return FileInfo(fileUri, name, 0, mimeType)
    }

    @Throws(Exception::class)
    override fun findOrCreateDirectory(directoryInfo: DirectoryInfo, name: String): DirectoryInfo {
        return findDirectoryOrNull(directoryInfo, name) ?: createDirectory(directoryInfo, name)
    }

    @Throws(Exception::class)
    private fun createDirectory(directoryInfo: DirectoryInfo, name: String): DirectoryInfo {
        val uri = directoryInfo.uri as Uri

        val directoryUri = DocumentsContract.createDocument(
            context.contentResolver,
            uri,
            MIME_TYPE_DIR,
            name
        ) ?: throw IOException()

        // In case a directory with that name already exists, a number is appended. The method is private and only used
        // from findOrCreateDirectory(), for now this should be safe.
        return DirectoryInfo(directoryUri, name)
    }

    override fun listFiles(directoryInfo: DirectoryInfo): List<FileInfo> {
        val uri = directoryInfo.uri as Uri
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                COLUMN_DOCUMENT_ID,
                COLUMN_DISPLAY_NAME,
                COLUMN_SIZE,
                COLUMN_MIME_TYPE
            ),
        ) { cursor ->
            if (cursor == null) return emptyList()

            // cursor also contains directories, however overhead should be small enough to use it for initialization.
            return ArrayList<FileInfo>(cursor.count).apply {
                while (cursor.moveToNext()) {
                    val documentType = cursor.getString(COLUMN_MIME_TYPE)

                    if (documentType != MIME_TYPE_DIR) {
                        add(
                            FileInfo(
                                uri = DocumentsContract.buildDocumentUriUsingTree(
                                    uri,
                                    cursor.getString(COLUMN_DOCUMENT_ID)
                                ),
                                fullName = cursor.getString(COLUMN_DISPLAY_NAME),
                                size = cursor.getLong(COLUMN_SIZE),
                                mimeType = documentType
                            )
                        )
                    }
                }
            }
        }
    }

    override fun listDirectories(directoryInfo: DirectoryInfo): List<DirectoryInfo> {
        val uri = directoryInfo.uri as Uri
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                COLUMN_DOCUMENT_ID,
                COLUMN_DISPLAY_NAME,
                COLUMN_MIME_TYPE
            )
        ) { cursor ->
            // Not initializing the ArrayList with the cursor count, because we expect the number of directories to be
            // much smaller than the number of files.
            return ArrayList<DirectoryInfo>().apply {
                while (cursor?.moveToNext() == true) {
                    if (cursor.getString(COLUMN_MIME_TYPE) == MIME_TYPE_DIR) {
                        add(
                            DirectoryInfo(
                                uri = DocumentsContract.buildDocumentUriUsingTree(
                                    uri,
                                    cursor.getString(COLUMN_DOCUMENT_ID)
                                ),
                                name = cursor.getString(COLUMN_DISPLAY_NAME)
                            )
                        )
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun delete(fileInfo: FileInfo) {
        val uri = fileInfo.uri as Uri
        deleteDocument(uri)
    }

    @Throws(Exception::class)
    override fun delete(directoryInfo: DirectoryInfo) {
        val uri = directoryInfo.uri as Uri
        deleteDocument(uri)
    }

    @Throws(Exception::class)
    private fun deleteDocument(uri: Uri) {
        try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: IllegalArgumentException) {
            // For some reason, the FileNotFound exception is wrapped in an IllegalArgumentException.
            if (e.message?.contains("java.io.FileNotFoundException") == true) return
            throw e
        }
    }

    override fun relocate(
        sourceFileInfo: FileInfo,
        sourceParentDirectoryInfo: DirectoryInfo,
        targetDirectoryInfo: DirectoryInfo
    ) {
        DocumentsContract.moveDocument(
            context.contentResolver,
            sourceFileInfo.uri as Uri,
            sourceParentDirectoryInfo.uri as Uri,
            targetDirectoryInfo.uri as Uri
        )
    }

    @Throws(FileNotFoundException::class)
    override fun openInputStream(fileInfo: FileInfo): InputStream {
        val uri = fileInfo.uri as Uri
        return context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException()
    }

    @Throws(FileNotFoundException::class)
    override fun openOutputStream(fileInfo: FileInfo): OutputStream {
        val uri = fileInfo.uri as Uri
        return context.contentResolver.openOutputStream(uri, "rwt") ?: throw FileNotFoundException()
    }

    //region Extension methods

    /**
     * A simple wrapper around [ContentResolver.query] that automatically closes the cursor. Note that `selection` is
     * not available because [Android's FileSystemProvider does not support it](https://stackoverflow.com/a/61214849).
     */
    inline fun <R> ContentResolver.query(uri: Uri, projection: Array<String>, block: (Cursor?) -> R): R {
        return query(uri, projection, null, null, null).use(block)
    }

    @Throws(IllegalArgumentException::class)
    fun Cursor.getString(columnName: String): String {
        return getString(getColumnIndexOrThrow(columnName))
    }

    @Throws(IllegalArgumentException::class)
    fun Cursor.getLong(columnName: String): Long {
        return getLong(getColumnIndexOrThrow(columnName))
    }

    //endregion
}
