# DevAI Studio PC — repository notes

- Windows desktop client: Kotlin JVM 1.8 + Swing, version 3.2.1.
- `CodeLanguages.kt` contains 50 languages; selection is stored in `AppSettings.codeLanguage` and appended to the system prompt.
- Main class: `com.devai.studio.desktop.MainKt`.
- Build: `./gradlew :desktop:fatJar` → `desktop/build/libs/DevAIStudio-all.jar`.
- Installer: `desktop/installer.nsi`, built with `makensis installer.nsi` from a staging directory.
- Staging directory needs `app/DevAIStudio-all.jar`, `runtime/` (JRE 8 x64), `DevAI-Android.apk`, `platform-tools/` (adb.exe + DLLs), `icon.ico`, `README-PC.txt`, `Vexel.cmd`.
- The installer has a custom components page with `Полная версия` / `Только программа` buttons. NSIS section IDs must be declared before they are referenced in functions, so keep section order in mind.
- The bundled JRE 8 accepts class-file version 52.0 only; keep desktop compilation at Java 8.
- Vexel is integrated unchanged. Attribution: конструктор взят у https://t.me/cheburek_dev, разрешение есть.
- Android app and Vexel source: https://github.com/Karishka-sr/devai-studio
