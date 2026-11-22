---
name: web-to-android-apk
description: Use when a user wants an installable Android APK for a website, PWA, or web app, needs a Home Screen app because browser installation is unavailable, or needs to rebuild, sign, update, or diagnose an existing Android wrapper.
---

# Web to Android APK

Deliver a signed `.apk` that opens the requested app on Android. Identify the project from the current chat and workspace. Read its build configuration, branding, and any Android packaging before making changes. Use the user's existing app name, icon, and public URL when known.

## Choose the packaging route

- **Existing Android, Capacitor, or TWA project:** use its build system and package identity. Build the release APK for direct installation. Produce an AAB only for a separate store request.
- **Hosted HTTPS website or web app:** use the Windows WebView starter in [build and test](references/build-and-test.md) when its feature needs fit. Confirm the launch URL and final origin. This gives users a launcher icon through Android's installer, independent of the browser's PWA menu.
- **Local web project:** build the existing project. For a frontend that works as static files, package the output using Capacitor or an Android WebView with `WebViewAssetLoader` and an HTTPS asset origin. Verify SPA route fallback or hash routing as appropriate, asset paths, and offline behavior. Check API CORS and authentication from the packaged origin. Server-rendered pages and backend APIs need a reachable backend. Use an existing deployment or get the launch URL when the packaging depends on it.
- **Browser-dependent sign-in or flows:** assess a Trusted Web Activity when the app can verify domain ownership with Digital Asset Links and a compatible browser is available. Embedded WebViews can break OAuth, passkeys, uploads, downloads, and permission flows. Test the actual features and choose or adapt the wrapper accordingly.

## Build and verify

1. For a new app, choose a stable, project-specific application ID. For an update, keep the previous APK's application ID and signing certificate, and increase `versionCode`. If the original key is unavailable, ask for it or discuss a separately installed package.
2. Use a release key stored privately outside shared artifacts and source control. Keep credentials out of command arguments and output. Preserve the key and document how the owner can rebuild updates.
3. Verify the APK signature and inspect its package name, version, SDK range, launcher entry, icon, and permissions. Install and cold-launch on an Android device or emulator when available. Confirm the real page renders and test the project's key interactions. For updates, test replacement over the prior release with `adb install -r`.
4. For startup crashes, capture the failing launch with `adb logcat -b crash` before changing code. On Android 15, attach the content view before looking up the window insets controller. The starter already does this. Re-run the launch and inspect the new crash log after fixing the cause.
5. Deliver a clickable APK or the user-requested download location, plus Android install/update steps and the actual test coverage. A signature check verifies the installer. A device or emulator launch verifies startup. Website changes and store publication require their own task scope.

Read [build and test](references/build-and-test.md) for the Windows helper, key handling, and launch commands.
