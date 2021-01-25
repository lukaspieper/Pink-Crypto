# Pink-Crypto

Pink stands for Password-[Tink](https://github.com/google/tink). It is a Android library that extends the
popular crypto library build by Google to support password based encryption and decryption. Currently it
uses [Signal's Argon2 library](https://github.com/signalapp/Argon2) to derive a key from the password.

> **_NOTE:_**  I am not a cryptography expert and this library is in an early stage of development.

## Usage

Example for creating a new `KeysetHandle`, encrypting it with a password and finally exporting the encrypted
Keyset to store it.

```kotlin
// Generate a new KeysetHandle as you know it from Tink
StreamingAeadConfig.register()
val keysetHandle = KeysetHandle.generateNew(AesGcmHkdfStreamingKeyManager.aes256GcmHkdf4KBTemplate())

// Encrypt it with a password
val passwordBytes = TODO("Password as ByteArray")
val encryptedKeyset = keysetHandle.encryptWithPassword(passwordBytes)

// Export the encrypted Keyset
val encryptedKeysetString = encryptedKeyset.exportAsString()
```

Example for importing an encrypted Keyset and decrypting it with the password to obtain a `KeysetHandle`.

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
