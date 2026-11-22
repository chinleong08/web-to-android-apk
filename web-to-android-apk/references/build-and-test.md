# Build and test

## Hosted HTTPS app on Windows

The PowerShell 7 helper compiles the bundled Java WebView and manifest templates with Android SDK Build Tools and JDK 17. It uses Android platform 35, targets Android 15, and supports API 26 and newer. Use the project's existing tools if present. On other systems, use the project's Gradle build or port these steps using the platform's Android SDK executables.

Locate the JDK, `android.jar`, and Build Tools. If missing, obtain a JDK and use Android Studio SDK Manager or [sdkmanager](https://developer.android.com/tools/sdkmanager) to install `platforms;android-35` and `build-tools;35.0.0`. Install `platform-tools` for ADB. Keep downloaded tools and build intermediates out of deliverables.

The starter opens the supplied HTTPS URL and keeps its origin in the WebView. External HTTPS links open in the device's browser. It enables JavaScript and local storage, blocks cleartext and mixed content, and shows a retry view for navigation failures. Review [Android WebView](https://developer.android.com/develop/ui/views/layout/webapps/webview) when the app needs additional features. For local bundles, see [Load in-app content](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content). For browser-based packaging, see [Trusted Web Activities](https://developer.chrome.com/docs/android/trusted-web-activity/quick-start).

Use an existing square PNG app icon, ideally 512 × 512. Use the canonical launch URL after redirects. If the site has multiple trusted origins or requires opening a separate window, implement and test that behavior in the project wrapper.

## Signing and example build

Reuse the previous release key and alias when updating. Create a dedicated key for a new app if none exists. Set `APK_SIGNING_PASSWORD` from an existing secret store or interactive input in the same process as the build. On Windows, an encrypted value created by `ConvertFrom-SecureString` can be read back by the same Windows user on the same machine. Some sandbox accounts cannot access DPAPI. Keep key backup and recovery separate from the APK. See [Android signing](https://developer.android.com/studio/publish/app-signing).

For a new key, with the password already set in the environment, run:

```powershell
& "$jdk/bin/keytool.exe" -genkeypair -storetype PKCS12 -keystore $keyPath `
  -storepass:env APK_SIGNING_PASSWORD -alias release -keyalg RSA `
  -keysize 3072 -validity 10000 -dname 'CN=Example App'
```

Use a fresh intermediate directory and a new output filename for each build. Paths below are inputs set for the current project.

```powershell
$build = @{
  Url = 'https://app.example.com/'
  AppName = 'Example App'
  PackageId = 'com.example.app'
  Icon = $iconPng
  Jdk = $jdk
  BuildTools = $buildTools
  PlatformJar = $androidJar
  SigningKey = $keyPath
  KeyAlias = 'release'
  BuildDirectory = $scratchDirectory
  Output = $apkPath
  VersionCode = 1
  VersionName = '1.0.0'
}
& "$skillDirectory/scripts/build-apk.ps1" @build
```

The helper prints the verified signer, APK metadata, and SHA-256 file hash. Clear the password from the environment when done. Check the old and new certificate digests with [apksigner](https://developer.android.com/tools/apksigner) for an update.

## Launch test

Use `adb devices -l` to identify the test target. For a physical phone, use the authorized device. For an emulator, use a version relevant to the user's device, including Android 15 when reproducing the insets crash.

```powershell
& $adb -s $device install -r $apkPath
& $adb -s $device shell am force-stop $packageId
& $adb -s $device shell am start -W -n "$packageId/.MainActivity"
& $adb -s $device logcat -d -b crash -s AndroidRuntime
```

Check fresh crash entries for the app, then wait for the dashboard to render and inspect it. An activity launch command can report success even when the process crashes immediately afterward. Test app-specific features such as login, deep links, uploads, and offline behavior if they are required. If the physical device is unavailable, report which emulator ran and what remains untested.

For manual installation, transfer the APK to the phone, open it from Files, and tap **Install** or **Update**, then **Open**. Android may prompt the user to permit that source. See [Android installation help](https://support.google.com/pixelphone/answer/7391672).
