/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Phone - Day", device = Devices.PIXEL_6, showSystemUi = true)
@Preview(name = "Tablet - Day", device = Devices.PIXEL_TABLET, showSystemUi = true)
@Preview(
    name = "Phone - Night",
    device = Devices.PIXEL_6,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Tablet - Night",
    device = Devices.PIXEL_TABLET,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
annotation class PagePreviews

// Same as above, but without `showSystemUi`
@Preview(name = "Phone - Day", device = Devices.PIXEL_6)
@Preview(name = "Tablet - Day", device = Devices.PIXEL_TABLET)
@Preview(
    name = "Phone - Night",
    device = Devices.PIXEL_6,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Tablet - Night",
    device = Devices.PIXEL_TABLET,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
annotation class ElementPreviews
