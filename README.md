# Pink-Crypto

The name 'Pink' stands for Password-[Tink](https://github.com/google/tink). It is a Android library
that extends the popular crypto library build by Google engineers to support password based
encryption and decryption. It uses the
[official Argon2 source](https://github.com/P-H-C/phc-winner-argon2) with JNI to derive a key from
the password.

This library was created for and is an elementary part of my file encryption app Truvark 
([Play Store](https://play.google.com/store/apps/details?id=de.lukaspieper.truvark)).

> **_NOTE:_**  I am NOT a cryptography expert. The Argon2 implementation is using code from
> [Argon2Kt](https://github.com/lambdapioneer/argon2kt) and the interaction with Tink is based on
> [this issue](https://github.com/google/tink/issues/347) from their repository.

## Usage

Example for creating a new `KeysetHandle`, encrypting it with a password and finally exporting the
encrypted Keyset to store it.

```kotlin
// Generate a new KeysetHandle as you know it from Tink
StreamingAeadConfig.register()
val aesKeyTemplate = KeyTemplates.get("AES256_GCM_HKDF_4KB")
val keysetHandle = KeysetHandle.generateNew(aesKeyTemplate)

// Encrypt it with a password
val passwordBytes = TODO("Password as ByteArray")
val encryptedKeyset = keysetHandle.encryptWithPassword(passwordBytes)

// Export the encrypted Keyset
val encryptedKeysetString = encryptedKeyset.exportAsString()
```

Example for importing an encrypted Keyset and decrypting it with the password to obtain a 
`KeysetHandle`.

```kotlin
// Register Tink if not done earlier. Get the encryptedKeysetString and password
StreamingAeadConfig.register()
val encryptedKeysetString = TODO("The exported encrypted Keyset String, see above")
val passwordBytes = TODO("Password as ByteArray")

// Import the stored String
val encryptedKeyset = PasswordEncryptedKeyset.importFromString(encryptedKeysetString)

// Decrypt the Keyset
val keysetHandle = encryptedKeyset.decryptWithPassword(passwordBytes)

// Use the KeysetHandle as you know it from Tink, e.g.
val streamingAead = keysetHandle.getPrimitive(StreamingAead::class.java)
```
