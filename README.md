# Web to Android APK

A Codex skill for turning websites and web apps into signed Android APKs that people can install on their phones.

## Install in Codex

Ask Codex:

```text
Use $skill-installer to install the skill from https://github.com/chinleong08/web-to-android-apk/tree/main/web-to-android-apk
```

Or download this repository and copy the `web-to-android-apk` folder into your Codex skills directory, normally `~/.codex/skills/`. The folder must include `SKILL.md` and its supporting directories. The skill will be available on your next turn.

## Use

```text
Use $web-to-android-apk to package my web app as a signed APK I can install on Android.
```

The skill inspects the project, selects an Android packaging approach, builds and signs the APK, verifies its metadata and signature, and tests installation and startup when a device or emulator is available.

- Existing Android, Capacitor or TWA projects keep their existing build system and package identity.
- Hosted HTTPS apps can use the bundled Windows WebView starter.
- Static local apps can be bundled for offline use with Capacitor or WebViewAssetLoader.

## Requirements

Android build tools are separate from this skill. The bundled Windows helper requires PowerShell 7, JDK 17, Android platform 35 and Android SDK Build Tools 35.0.0. Other packaging approaches use their own project requirements. See the [build and test guide](web-to-android-apk/references/build-and-test.md) for setup, signing and device testing.

## Files

- [SKILL.md](web-to-android-apk/SKILL.md): workflow and packaging decisions.
- `web-to-android-apk/agents/openai.yaml`: skill name and invocation prompt.
- `web-to-android-apk/assets/`: Android manifest and Java WebView templates.
- `web-to-android-apk/scripts/build-apk.ps1`: Windows build and signing helper.
- `web-to-android-apk/references/build-and-test.md`: setup, build and verification instructions.

