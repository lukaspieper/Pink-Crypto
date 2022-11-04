// Copyright (c) 2021 Lukas Pieper
// Copyright (c) Daniel Hugenroth
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package de.lukaspieper.crypto.pink.argon2

import com.google.crypto.tink.subtle.Random
import java.nio.ByteBuffer

/**
 * Overwrites the bytes of a byte buffer with random bytes. The method asserts that the buffer is a direct buffer as a
 * precondition.
 *
 * @throws [IllegalStateException] if the buffer [ByteBuffer.isDirect] is false.
 */
internal fun ByteBuffer.wipeDirectBuffer() {
    check(this.isDirect) { "Only direct-allocated byte buffers can be meaningfully wiped" }

    val arr = Random.randBytes(this.capacity())
    this.rewind()

    // overwrite bytes (actually overwrites the memory since it is a direct buffer)
    this.put(arr)
}
