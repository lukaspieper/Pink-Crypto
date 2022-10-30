// Copyright (c) 2021 Lukas Pieper
// Copyright (c) Daniel Hugenroth
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package de.lukaspieper.crypto.pink.argon2

import java.nio.ByteBuffer
import java.util.*

/**
 * Overwrites the bytes of a byte buffer with random bytes. The method asserts that the buffer is a direct buffer as a
 * precondition.
 *
 * @param random The random generator to use for overwriting. Default's to Java's standard [Random] implementation.
 * However, you might want to use a [java.security.SecureRandom] source for more adverse threat models.
 *
 * @throws [IllegalStateException] if the buffer [ByteBuffer.isDirect] is false.
 */
internal fun ByteBuffer.wipeDirectBuffer(random: Random = Random()) {
    if (!this.isDirect) throw IllegalStateException("Only direct-allocated byte buffers can be meaningfully wiped")

    val arr = ByteArray(this.capacity())
    this.rewind()

    // overwrite bytes (actually overwrites the memory since it is a direct buffer)
    random.nextBytes(arr)
    this.put(arr)
}

/** If the assertion holds nothing happens. Otherwise, an IllegalArgumentException is thrown with the given message. */
internal fun checkArgument(assertion: Boolean, message: String) {
    if (!assertion) throw IllegalArgumentException(message)
}
