/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.controls

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.ui.preview.PagePreviews
import de.lukaspieper.truvark.ui.preview.PreviewHost
import de.lukaspieper.truvark.ui.theme.paddings

@Composable
fun MaterialDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    @StringRes title: Int? = null,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    isLoadingIndicator: Boolean = false,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .padding(MaterialTheme.paddings.extraLarge)
                    .verticalScroll(rememberScrollState())
            ) {
                if (title != null) {
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = MaterialTheme.paddings.large)
                    )
                }

                if (isLoadingIndicator) {
                    CircularProgressIndicator()
                } else {
                    content()
                }

                if (confirmButton != null || dismissButton != null) {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.paddings.extraLarge)
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (confirmButton != null) {
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

@PagePreviews
@Composable
private fun CreateNewFolderDialogPreview() = PreviewHost {
    MaterialDialog(
        title = R.string.create_new_folder,
        confirmButton = {
            Button(onClick = { }) {
                Text(text = "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = { }) {
                Text(text = "Cancel")
            }
        },
    ) {
        var text by remember { mutableStateOf("") }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@PagePreviews
@Composable
private fun LoadingIndicatorPreview() = PreviewHost {
    MaterialDialog(
        isLoadingIndicator = true,
        content = { }
    )
}
