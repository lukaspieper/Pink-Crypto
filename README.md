<!--
SPDX-FileCopyrightText: 2022 Lukas Pieper

SPDX-License-Identifier: GPL-3.0-or-later
-->

<p align="center">
  <img src=".idea/icon.svg" height="128px" alt="Truvark logo"/>
</p>

## Pitch

Maybe you're wondering why you should give an underdog file encryption app a chance? Especially when this type of app is
all about trust?

Compared to many other popular alternatives, Truvark...

- [x] supports multiple vaults on a single device
- [x] supports deep folder structures (subfolders)
- [x] decrypts common media files (images, video, audio) in-app on the fly
- [x] works completely offline, no internet access/permission required
- [x] runs without dangerous permissions like full storage/media access
- [x] contains no advertising, telemetry or other user data collection
- [x] encrypts the database and thumbnails
- [x] allows biometric unlocking (e.g. fingerprint) without compromising security

As you can see, there are already plenty of reasons to give Truvark a try today!

<p align="center">
<a href='https://play.google.com/store/apps/details?id=de.lukaspieper.truvark'>
<img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="250px"/>
</a>
</p>

## Features in detail

### Multiple vaults

You can create multiple vaults on your device. Any empty folder can become a vault. All your data remains on the shared
device storage, which means you can access the encrypted files from a file manager, for example, for backups.

> [!NOTE]
> Using the device's shared storage is a significant difference compared to alternatives. Some apps don't even encrypt
> your files, they just move them to the app's internal storage. This is often referred to as "data hiding" rather than
> encrypting.

### Deep folder structures

Truvark is not an encrypted gallery that just lets you group your images into albums. It is a file encryption app with
full support for subfolders. You are not limited in how you organize your files.

### View encrypted files

Common file types can be viewed in the application. Currently supported are images, video and audio. Decryption is done
*on the fly*, which means that the required data remains in memory (RAM) instead of being written to storage. This is
especially important for long videos that would otherwise not fit in memory. The image viewer supports high resolution
images and shows more details when zooming in instead of getting pixelated (called *subsampling*).

> [!NOTE]
> Some popular alternative apps decrypt the entire file to disk before displaying it. This sacrifices performance and
> may put the file at risk.

### Privacy by default

In short, this app has no Internet permissions. There are no analytics, ads, telemetry, or account requirements. There
is an option in the settings to enable on-device logging, which is turned off by default.

### Security by design

In cryptography, it is enough to get a single parameter wrong to make software insecure. To reduce this risk, popular
open source libraries are used.

One of them is an encryption library built by Google engineers and used in Google Pay
called [Tink](https://github.com/tink-crypto/tink-java). It was designed with the goal of reducing insecure software due
to "configuration" errors. They feature this prominently:

> *Tink provides secure APIs that are easy to use correctly and hard(er) to misuse.*

In addition, Argon2(id) is used for key derivation. It won the
[Password Hashing Competition](https://en.wikipedia.org/wiki/Password_Hashing_Competition)
in 2015 and is one of the best (if not the best) algorithm for this task.

Finally, Realm was chosen for the database because it supports database encryption out of the box.

> [!NOTE]
> Many other vault apps use an unencrypted database!

## License

This app is released under [*GPL-3.0-or-later*](LICENSES/GPL-3.0-or-later.txt).
