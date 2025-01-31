/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.test.fakes

import de.lukaspieper.truvark.common.domain.vault.Vault
import de.lukaspieper.truvark.common.work.Scheduler
import de.lukaspieper.truvark.common.work.WorkBundle
import kotlinx.coroutines.runBlocking

/**
 * A fake implementation of [Scheduler] that processes all [WorkBundle] units synchronously (blocking).
 */
class SchedulerFake : Scheduler() {
    override fun schedule(workBundle: WorkBundle, metadata: SchedulerMetadata) {
        runBlocking {
            for (i in 0 until workBundle.size) {
                workBundle.processUnit()
            }
        }
    }

    override fun onVaultChanged(vault: Vault) {
        // Intentionally empty.
    }

    object MetadataFake : SchedulerMetadata
}
