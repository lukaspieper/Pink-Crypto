package de.lukaspieper.crypto.pink.argon2

import org.signal.argon2.Argon2
import org.signal.argon2.MemoryCost
import org.signal.argon2.Type
import org.signal.argon2.Version

@Throws(IllegalStateException::class)
internal fun Argon2Config.buildArgon2(): Argon2 {
    val version = getSignalArgon2Version(this)
    return Argon2.Builder(version)
        .type(Type.fromEncoded("$" + type + "$"))
        .hashLength(Argon2id.keySize)
        .memoryCost(MemoryCost.KiB(memoryCostInKiBit))
        .iterations(iterations)
        .parallelism(parallelism)
        .build()
}

@Throws(IllegalStateException::class)
private fun getSignalArgon2Version(config: Argon2Config): Version {
    return when (config.version) {
        0x10 -> Version.V10
        0x13 -> Version.V13
        else -> throw IllegalStateException()
    }
}
