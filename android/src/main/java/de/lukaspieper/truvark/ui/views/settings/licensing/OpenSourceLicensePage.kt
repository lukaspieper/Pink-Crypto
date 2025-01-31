/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.settings.licensing

import android.content.res.Resources
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.ui.preview.DetailPanePreviewHost
import de.lukaspieper.truvark.ui.preview.PagePreviews
import de.lukaspieper.truvark.ui.theme.paddings
import de.lukaspieper.truvark.ui.views.settings.licensing.License.GeneralPublicLicenseV30OrLater

@Composable
fun OpenSourceLicensePage(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val licensedItems by produceState(initialValue = emptyList<LicensedItem>()) {
        value = fetchLicenseItems(context.resources)
    }

    OpenSourceLicenseView(
        licensedItems = licensedItems,
        modifier = modifier
    )
}

private fun fetchLicenseItems(resources: Resources): List<LicensedItem> {
    val metadata = resources.readRawStringResource(R.raw.third_party_license_metadata)
    val licenseUris = resources.readRawStringResource(R.raw.third_party_licenses)

    val undetectedLicensedItems = listOf(
        LicensedItem("logcat", License.ApacheLicenseV20)
    )

    val metadataRegex = Regex("^(\\d+):(\\d+)\\s(.+)\$")
    return metadata.lineSequence()
        .mapNotNull { line -> metadataRegex.matchEntire(line) }
        .map { matchResult ->
            val (uriStartIndex, uriLength, itemId) = matchResult.destructured
            val licenseUri = licenseUris.substring(uriStartIndex.toInt(), uriStartIndex.toInt() + uriLength.toInt())
            LicensedItem(itemId, License.getByUri(licenseUri))
        }
        .plus(undetectedLicensedItems)
        .distinctBy { it.id.lowercase() }
        .sortedBy { it.id.lowercase() }
        .toList()
}

private fun Resources.readRawStringResource(@RawRes id: Int): String {
    openRawResource(id).use { inputStream ->
        return inputStream.bufferedReader().readText()
    }
}

@Composable
fun OpenSourceLicenseView(
    licensedItems: List<LicensedItem>,
    modifier: Modifier = Modifier
) {
    var selectedLicense by remember { mutableStateOf<License>(License.UnknownLicense) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.paddings.small),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Card(
                onClick = { selectedLicense = GeneralPublicLicenseV30OrLater },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(all = MaterialTheme.paddings.small)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = GeneralPublicLicenseV30OrLater.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.size(MaterialTheme.paddings.extraLarge))
        }

        items(licensedItems) { licenseItem ->
            Card(
                enabled = licenseItem.license != License.UnknownLicense,
                onClick = { selectedLicense = licenseItem.license },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(all = MaterialTheme.paddings.small)) {
                    Text(
                        text = licenseItem.id,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = licenseItem.license.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Spacer(Modifier.size(8.dp))
        }
    }

    if (selectedLicense != License.UnknownLicense) {
        val context = LocalContext.current
        val licenseText by produceState(initialValue = "") {
            selectedLicense.textResId?.let { textResId ->
                value = context.resources.readRawStringResource(textResId)
            }
        }

        Dialog(
            onDismissRequest = { selectedLicense = License.UnknownLicense },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .padding(MaterialTheme.paddings.small)
                    .sizeIn(maxWidth = 600.dp)
            ) {
                Box(Modifier.padding(horizontal = MaterialTheme.paddings.small)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = licenseText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = MaterialTheme.paddings.medium, bottom = 72.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = { selectedLicense = License.UnknownLicense },
                        modifier = Modifier
                            .align(alignment = Alignment.BottomEnd)
                            .padding(bottom = MaterialTheme.paddings.small)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            }
        }
    }
}

@PagePreviews
@Composable
private fun OpenSourceLicenseViewPreview() = DetailPanePreviewHost { contentPadding ->
    val licensedItems = List(10) {
        LicensedItem("Item $it", License.UnknownLicense)
    }

    OpenSourceLicenseView(
        licensedItems = licensedItems,
        modifier = Modifier.padding(contentPadding)
    )
}
