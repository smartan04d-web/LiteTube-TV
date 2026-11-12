# LiteTube TV (Android 10+)

Experimental Android TV WebView wrapper for https://m.youtube.com/.

## Features
- TV launcher entry with banner
- Focusable remote toolbar (Home, Back, Reload, Search, Browse)
- Remote OK/arrow navigation in toolbar
- WebView page navigation where supported by the TV's WebView
- Search via Android TV on-screen keyboard
- Fullscreen video callback and BACK exit
- No background services or third-party SDKs

## Important limitations
Android TV devices do not always include an enabled, modern WebView implementation. This app requires Android System WebView and may fail on some televisions. The mobile YouTube website is not optimized for D-pad navigation; some elements may require a mouse/air mouse. Video playback and sign-in are not guaranteed. YouTube advertising is not removed. The project has not been compiled or device-tested in this environment.

## Build APK on GitHub
Create a NEW GitHub repository (recommended: LiteTube-TV). Upload all project files, including hidden `.github/workflows/build-apk.yml`, to the repository root. GitHub Actions runs automatically on a push to `main`. Open Actions > successful run > Artifacts > LiteTube-TV-debug-APK. Extract ZIP to get `app-debug.apk`.

## Install on Android TV
Transfer the APK using USB drive or a trusted file-transfer app. On Android TV enable installation from the file manager under Settings > Apps > Special app access > Install unknown apps (menu varies). Open APK and install. App should appear in TV launcher. If installation fails, verify Android 10+ and WebView availability.

## Local build
Requires JDK 17, Android SDK platform 35 and build-tools 35.0.0, Gradle 8.9. Run `gradle assembleDebug`.

## Design
Separate package ID `com.litetube.tv`, so it does not replace the phone LiteTube app. The app does not provide DRM bypass, ad skipping, or video downloads.

## v2: optional ad controls
- Remote-focusable `Ads filter ON/OFF` toggle: conservatively filters selected third-party advertising hosts. It does not block all YouTube ads.
- Remote-focusable `Auto skip ON/OFF` toggle: once per second while the app is in foreground, attempts to click a visible, enabled YouTube Skip Ad button. It cannot bypass a countdown, unskippable ads, or guarantee success on TV WebView.
- Both preferences persist. Turn off ad filtering if videos, sign-in, or other content stops working.
- No background service. Skip checks stop when the app is paused.
- The toolbar may be wide on smaller 720p TVs; a mouse or larger display may be needed.
