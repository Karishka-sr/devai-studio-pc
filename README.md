# DevAI Studio PC

Windows-версия DevAI Studio: чат с ИИ, веб-поиск и конструктор Vexel.

[Скачать PC-версию](https://github.com/Karishka-sr/devai-studio-pc/releases/latest) · Windows 7/10/11 · JRE 8 внутри · версия 3.2.0

## Что внутри

- **75 языков программирования**: Kotlin, Java, Python, JavaScript, TypeScript, Dart, Swift, C, C++, C#, Go, PHP, Ruby, SQL, Bash и ещё десятки.
- Выбор языка в чате и в «Настройках»: примеры, тесты и команды приходят на выбранном языке.
- Установщик NSIS с выбором Windows 7, Windows 10 или Windows 11.
- Страница «Компоненты»: «Полная версия» или «Только программа».
- Полная версия: Vexel APK, ADB, ярлыки DevAI Studio и Vexel.
- Встроенная Java Runtime 8 — отдельная Java не нужна.
- Чат с OpenAI-совместимыми API, Gemini и Claude.
- Веб-поиск: GitHub, Stack Overflow, Wikipedia, DuckDuckGo, Hacker News, Tavily.
- Вкладка Vexel: «Установить Vexel», «Открыть Vexel», «Проверить ADB».
- Портативная сборка: JAR + JRE 8 + Android APK + ADB в одном ZIP.

## Vexel

Vexel — Android-конструктор. Он входит в установщик как APK без изменений.
ADB уже включён в полную версию. Установите Android-эмулятор (BlueStacks,
LDPlayer, Android Studio Emulator) или подключите телефон с отладкой по USB,
затем нажмите ярлык «Vexel».

Автор: конструктор взят у https://t.me/cheburek_dev, разрешение есть,
оригинальный конструктор Vexel скачивать с этого тгк.

## Портативная версия

1. Распакуйте `DevAI-Studio-PC-3.2.0-portable.zip`.
2. Запустите `DevAI.cmd` или `runtime\bin\javaw.exe -jar app\DevAIStudio-all.jar`.
3. Настройки и история лежат в `%APPDATA%\DevAI Studio`.

## Сборка

```bash
./gradlew :desktop:fatJar
```

JAR: `desktop/build/libs/DevAIStudio-all.jar`.

Для установщика соберите staging-каталог:

- `app/DevAIStudio-all.jar`
- `runtime/` — JRE 8 x64
- `DevAI-Android.apk`
- `platform-tools/` — `adb.exe`, `AdbWinApi.dll`, `AdbWinUsbApi.dll`
- `icon.ico`, `README-PC.txt`, `Vexel.cmd`, `installer.nsi`

Затем:

```bash
makensis installer.nsi
```

## Основной репозиторий

Android-приложение и Vexel: https://github.com/Karishka-sr/devai-studio
