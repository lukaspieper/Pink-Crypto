/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class IdGeneratorTests {

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 5, 10, 20])
    fun `createStringId returns valid id`(length: Int) {
        // Act
        val id = IdGenerator.Default.createStringId(length)

        // Assert
        assertAll(
            { assertEquals(length, id.length) },
            { assertTrue(id.contains(Regex("[${IdGenerator.Default.ALLOWED_CHARS}]*"))) }
        )
    }

    @Test
    fun `createStringId returns different ids`() {
        // Arrange
        val length = 20
        val list = ArrayList<String>()

        // Act
        repeat(100000) {
            list.add(IdGenerator.Default.createStringId(length))
        }

        // Assert
        assertIterableEquals(list.distinct(), list)
    }
}
