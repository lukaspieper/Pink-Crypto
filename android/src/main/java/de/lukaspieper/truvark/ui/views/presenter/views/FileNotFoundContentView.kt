/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.presenter.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.ui.preview.ElementPreviews
import de.lukaspieper.truvark.ui.preview.PreviewHost
import de.lukaspieper.truvark.ui.theme.DarkColorScheme

@Composable
fun FileNotFoundContentView(
    fileName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ManageSearch,
            contentDescription = null,
            tint = DarkColorScheme.onBackground,
            modifier = Modifier
                .requiredSize(100.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            stringResource(R.string.encrypted_file_not_found),
            textAlign = TextAlign.Center,
            color = DarkColorScheme.onBackground
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = fileName,
            fontStyle = FontStyle.Italic,
            color = DarkColorScheme.onBackground
        )
    }
}

@ElementPreviews
@Composable
private fun FileNotFoundContentViewPreview() = PreviewHost(backgroundColor = Color.Black) {
    FileNotFoundContentView("example.txt")
}
