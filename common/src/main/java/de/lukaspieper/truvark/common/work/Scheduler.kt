/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.work

import de.lukaspieper.truvark.common.domain.vault.Vault

public abstract class Scheduler {
    public abstract fun schedule(workBundle: WorkBundle, metadata: SchedulerMetadata)
    public abstract fun onVaultChanged(vault: Vault)

    // `Metadata` is already used by Kotlin as annotation
    public interface SchedulerMetadata
}
