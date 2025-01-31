/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.browser

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.ui.controls.LabeledSwitch
import de.lukaspieper.truvark.ui.controls.MaterialDialog
import de.lukaspieper.truvark.ui.preview.PagePreviews
import de.lukaspieper.truvark.ui.preview.PreviewHost
import de.lukaspieper.truvark.ui.views.ActivityResultContracts
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

enum class BrowserDialogs {
    NONE,
    NEW_FOLDER,
    RENAME_FOLDER,
    ENCRYPT,
    DELETE_SELECTION
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun NewFolderDialog(
    hideDialog: () -> Unit,
    createFolder: suspend (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var folderName by rememberSaveable { mutableStateOf("") }
    var isInputValid by rememberSaveable { mutableStateOf(true) }

    MaterialDialog(
        modifier = modifier,
        onDismissRequest = { hideDialog() },
        confirmButton = {
            Button(onClick = {
                GlobalScope.launch {
                    isInputValid = createFolder(folderName)
                    if (isInputValid) hideDialog()
                }
            }) {
                Text(text = stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = { hideDialog() }) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = R.string.create_new_folder
    ) {
        OutlinedTextField(
            value = folderName,
            onValueChange = { folderName = it },
            label = { Text(stringResource(R.string.folder_name)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                GlobalScope.launch {
                    isInputValid = createFolder(folderName)
                    if (isInputValid) hideDialog()
                }
            }),
            isError = isInputValid.not(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@PagePreviews
@Composable
private fun NewFolderDialogPreview() = PreviewHost {
    NewFolderDialog(
        hideDialog = {},
        createFolder = { true }
    )
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun RenameFolderDialog(
    hideDialog: () -> Unit,
    folderName: String,
    renameFolder: suspend (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var editableFolderName by rememberSaveable(folderName) { mutableStateOf(folderName) }
    var isInputValid by rememberSaveable { mutableStateOf(true) }

    MaterialDialog(
        modifier = modifier,
        onDismissRequest = { hideDialog() },
        confirmButton = {
            Button(onClick = {
                GlobalScope.launch {
                    isInputValid = renameFolder(editableFolderName)
                    if (isInputValid) hideDialog()
                }
            }) {
                Text(text = stringResource(R.string.rename))
            }
        },
        dismissButton = {
            TextButton(onClick = { hideDialog() }) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = R.string.rename_folder
    ) {
        OutlinedTextField(
            value = editableFolderName,
            onValueChange = { editableFolderName = it },
            label = { Text(stringResource(R.string.folder_name)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                GlobalScope.launch {
                    isInputValid = renameFolder(editableFolderName)
                    if (isInputValid) hideDialog()
                }
            }),
            isError = isInputValid.not(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@PagePreviews
@Composable
private fun RenameFolderDialogPreview() = PreviewHost {
    RenameFolderDialog(
        hideDialog = {},
        folderName = "Preview Folder",
        renameFolder = { true }
    )
}

@Composable
fun EncryptFilesDialog(
    hideDialog: () -> Unit,
    encryptUris: (List<Uri>, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var deleteSourceFiles by rememberSaveable { mutableStateOf(false) }
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocumentsWithFlags(),
        onResult = { uris ->
            hideDialog()
            encryptUris(uris, deleteSourceFiles)
        }
    )

    MaterialDialog(
        modifier = modifier,
        onDismissRequest = { hideDialog() },
        title = R.string.encrypt_files,
        confirmButton = {
            Button(
                onClick = { openDocumentTreeLauncher.launch(emptyArray()) },
                content = { Text(stringResource(R.string.select)) }
            )
        },
        dismissButton = {
            OutlinedButton(
                onClick = { hideDialog() },
                content = { Text(stringResource(R.string.cancel)) }
            )
        }
    ) {
        LabeledSwitch(
            text = stringResource(R.string.delete_source_files),
            checked = deleteSourceFiles,
            onCheckedChange = { deleteSourceFiles = it },
            switchColors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onErrorContainer,
                checkedTrackColor = MaterialTheme.colorScheme.errorContainer
            )
        )
    }
}

@PagePreviews
@Composable
private fun EncryptFilesDialogPreview() = PreviewHost {
    EncryptFilesDialog(
        hideDialog = {},
        encryptUris = { _, _ -> }
    )
}

@Composable
fun DeleteSelectionDialog(
    hideDialog: () -> Unit,
    deleteSelectedCipherEntities: () -> Unit,
    modifier: Modifier = Modifier
) {
    MaterialDialog(
        modifier = modifier,
        onDismissRequest = { hideDialog() },
        title = R.string.confirm_deletion,
        confirmButton = {
            Button(
                onClick = {
                    deleteSelectedCipherEntities()
                    hideDialog()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                content = { Text(stringResource(R.string.delete)) }
            )
        },
        dismissButton = {
            OutlinedButton(
                onClick = { hideDialog() },
                content = { Text(stringResource(R.string.cancel)) }
            )
        }
    ) {
        Text(stringResource(R.string.delete_warning))
    }
}

@PagePreviews
@Composable
private fun DeleteSelectionDialogPreview() = PreviewHost {
    DeleteSelectionDialog(
        hideDialog = {},
        deleteSelectedCipherEntities = {}
    )
}
