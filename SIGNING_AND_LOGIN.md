# LiteTube TV signing and sign-in notes

## APK signing (install/update)

The workflow always builds a debug-signed APK. To also build a **persistently release-signed** APK, create a keystore once and retain it securely:

```bash
keytool -genkeypair -v -keystore litetube-release.jks -alias litetube -keyalg RSA -keysize 3072 -validity 10000
base64 -w 0 litetube-release.jks > keystore-base64.txt
```

In GitHub repository **Settings > Secrets and variables > Actions**, add four **secrets**:

- `LITETUBE_KEYSTORE_BASE64`: full contents of `keystore-base64.txt`
- `LITETUBE_KEYSTORE_PASSWORD`: keystore password
- `LITETUBE_KEY_ALIAS`: `litetube` (or your chosen alias)
- `LITETUBE_KEY_PASSWORD`: key password

Do not commit the keystore, passwords or base64 file. Keep a private backup of the keystore: all future releases must use the **same** key. A debug-signed build cannot update an installed release-signed build, or a debug build signed with another key. If you have an old APK signed with another key, uninstalling it may erase its app data.

## Google / YouTube sign-in

Google may reject login from embedded Android WebView. This project keeps normal HTTP(S) navigation inside LiteTube TV and does not automatically open Chrome, but **this does not fix Google sign-in**. A successful Chrome login does not transfer its cookies to WebView. Google OAuth/Credential Manager can authenticate a native account, but cannot turn the YouTube WebView into a logged-in YouTube session. Never copy browser cookies or passwords to work around this.

## Playback

YouTube may restrict some content in WebView. In-app playback and fullscreen are retained where supported, but not guaranteed for every video or TV device. This ZIP contains source, not a compiled or tested APK.
