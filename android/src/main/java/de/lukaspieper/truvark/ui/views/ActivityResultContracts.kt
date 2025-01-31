/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

interface ActivityResultContracts {

    class OpenMultipleDocumentsWithFlags : ActivityResultContracts.OpenMultipleDocuments() {

        override fun createIntent(context: Context, input: Array<String>): Intent {
            val intent = super.createIntent(context, input)

            intent.removeExtra(Intent.EXTRA_MIME_TYPES)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            return intent
        }
    }

    class OpenDocumentTreeWithFlags : ActivityResultContracts.OpenDocumentTree() {

        override fun createIntent(context: Context, input: Uri?): Intent {
            val intent = super.createIntent(context, input)

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )

            return intent
        }
    }
}
