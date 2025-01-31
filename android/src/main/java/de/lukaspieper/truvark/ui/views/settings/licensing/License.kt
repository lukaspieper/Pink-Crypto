/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.settings.licensing

import de.lukaspieper.truvark.R

sealed class License(
    val name: String,
    val textResId: Int?
) {
    companion object {
        // All uris listed in the generated file `third_party_licenses` should be a key in this map
        private val licenseByUri = mapOf(
            "http://www.apache.org/licenses/LICENSE-2.0.txt" to ApacheLicenseV20,
            "https://www.apache.org/licenses/LICENSE-2.0.txt" to ApacheLicenseV20,
            "https://www.apache.org/licenses/LICENSE-2.0" to ApacheLicenseV20,
            "https://github.com/lambdapioneer/argon2kt/blob/master/LICENSE" to MitLicense
        )

        fun getByUri(uri: String): License {
            return licenseByUri[uri] ?: UnknownLicense
        }
    }

    data object UnknownLicense : License(
        name = "Unknown license",
        textResId = null
    )

    data object ApacheLicenseV20 : License(
        name = "Apache License Version 2.0",
        textResId = R.raw.apache_2_0
    )

    data object MitLicense : License(
        name = "MIT License",
        textResId = R.raw.mit
    )

    data object GeneralPublicLicenseV30OrLater : License(
        name = "GNU General Public License",
        textResId = R.raw.gpl_3_0_or_later
    )
}
