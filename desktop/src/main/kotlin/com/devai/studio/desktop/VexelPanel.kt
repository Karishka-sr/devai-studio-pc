package com.devai.studio.desktop

import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.FlowLayout
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities

class VexelPanel : JPanel(BorderLayout(0, 10)) {
    private val apk: File = AppPaths.installedApk().toFile()
    private val adbFile: File? = AppPaths.adb()?.toFile()
    private val output = Theme.area(12)
    private val statusLabel = Theme.label("", 12, Theme.muted)
    init {
        background = Theme.bg
        output.isEditable = false
        val top = JPanel(BorderLayout()).apply { background = Theme.bg }
        top.add(Theme.label("Vexel", 20, Theme.text, true), BorderLayout.WEST)
        top.add(Theme.label("Android-конструктор · сохранён в PC-версии", 12, Theme.muted), BorderLayout.EAST)
        val info = Theme.area(9)
        info.isEditable = false
        info.text = """
            Vexel — это Android-конструктор приложений. В DevAI Studio он остаётся без изменений:
            APK с Vexel входит в установщик и лежит рядом с программой.

            На Windows Vexel запускается как Android-приложение: установите APK на телефон
            или в Android-эмулятор (BlueStacks, LDPlayer, Android Studio Emulator).
            ADB уже входит в полную версию установщика, поэтому кнопка «Установить Vexel»
            работает без отдельной установки Android SDK.

            Автор: Конструктор взят у https://t.me/cheburek_dev, Разрешение есть,
            оригинальный конструктор Vexel скачивать с этого тгк.

            APK: ${apk.absolutePath}
        """.trimIndent()
        val infoScroll = JScrollPane(info).apply {
            border = BorderFactory.createLineBorder(Theme.surface2, 1, true)
            preferredSize = java.awt.Dimension(100, 210)
        }
        val buttons = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply { background = Theme.bg }
        val folder = Theme.button("Папка Vexel")
        val install = Theme.button("Установить Vexel", true)
        val launch = Theme.button("Открыть Vexel")
        val adb = Theme.button("Проверить ADB")
        val guide = Theme.button("Инструкция")
        val download = Theme.button("Скачать APK")
        folder.addActionListener { openFolder() }
        install.addActionListener { installVexel() }
        launch.addActionListener { launchVexel() }
        adb.addActionListener { checkAdb() }
        guide.addActionListener { showGuide() }
        download.addActionListener { browse("https://github.com/Karishka-sr/devai-studio/releases/latest") }
        buttons.add(install)
        buttons.add(launch)
        buttons.add(adb)
        buttons.add(folder)
        buttons.add(guide)
        buttons.add(download)
        val center = JPanel(BorderLayout(0, 8)).apply { background = Theme.bg }
        center.add(infoScroll, BorderLayout.NORTH)
        center.add(buttons, BorderLayout.CENTER)
        center.add(JScrollPane(output).apply {
            border = BorderFactory.createLineBorder(Theme.surface2, 1, true)
        }, BorderLayout.SOUTH)
        add(top, BorderLayout.NORTH)
        add(center, BorderLayout.CENTER)
        add(statusLabel, BorderLayout.SOUTH)
        updateStatus()
    }
    private fun updateStatus() {
        val apkStatus = if (apk.isFile) {
            "APK: ${apk.name} · ${apk.length() / 1024 / 1024} МБ"
        } else {
            "APK не найден"
        }
        val adbStatus = if (adbFile != null) "ADB: встроен" else "ADB: не найден (нужен для установки в эмулятор)"
        statusLabel.text = "$apkStatus · $adbStatus"
    }

    private fun openFolder() {
        val dir = apk.parentFile
        if (dir == null || !dir.exists()) {
            JOptionPane.showMessageDialog(this, "Папка с APK не найдена:\n${apk.absolutePath}", "Vexel", JOptionPane.WARNING_MESSAGE)
            return
        }
        runCatching { Desktop.getDesktop().open(dir) }
            .onFailure { JOptionPane.showMessageDialog(this, it.message, "Vexel", JOptionPane.ERROR_MESSAGE) }
    }
    private fun checkAdb() {
        runAsync("Проверяю ADB…") { runAdb("devices") }
    }

    private fun installVexel() {
        if (!apk.isFile) {
            JOptionPane.showMessageDialog(this, "APK не найден:\n${apk.absolutePath}", "Vexel", JOptionPane.WARNING_MESSAGE)
            return
        }
        runAsync("Устанавливаю Vexel в эмулятор…") {
            val install = runAdb("install", "-r", apk.absolutePath)
            if (!install.contains("Success", ignoreCase = true)) {
                return@runAsync install
            }
            install + "\n\n" + startVexel()
        }
    }

    private fun launchVexel() {
        runAsync("Запускаю Vexel…") { startVexel() }
    }

    private fun startVexel(): String {
        val direct = runAdb("shell", "am", "start", "-n", "com.devai.studio/com.qwenz.qwel.MainActivity")
        val failed = listOf("Error", "Exception", "Permission Denial").any { direct.contains(it, ignoreCase = true) }
        if (!failed) return direct
        val fallback = runAdb("shell", "monkey", "-p", "com.devai.studio", "-c", "android.intent.category.LAUNCHER", "1")
        return direct + "\n\n" + fallback + "\nОткройте Vexel в приложении DevAI Studio."
    }

    private fun runAsync(label: String, block: () -> String) {
        statusLabel.text = label
        output.text = ""
        Thread({
            val result = runCatching(block)
            SwingUtilities.invokeLater {
                result.onSuccess {
                    output.text = it.ifBlank { "Команда выполнена без вывода." }
                    statusLabel.text = "Готово"
                }.onFailure {
                    output.text = it.message.orEmpty()
                    statusLabel.text = "Ошибка. Проверьте, что adb установлен и устройство подключено."
                }
            }
        }, "devai-adb").apply { isDaemon = true }.start()
    }

    private fun runAdb(vararg args: String): String {
        val exe = adbFile?.absolutePath
            ?: if (System.getProperty("os.name").lowercase().contains("windows")) "adb.exe" else "adb"
        val process = ProcessBuilder(listOf(exe) + args).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        if (!process.waitFor(90, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return text + "\nТаймаут команды adb."
        }
        return text
    }
    private fun showGuide() {
        val text = """
            Как запустить Vexel на Windows

            1. Установите Android-эмулятор: BlueStacks, LDPlayer или Android Studio Emulator.
            2. Включите в эмуляторе отладку по USB (ADB).
            3. Нажмите «Проверить ADB» — в списке должно появиться устройство.
            4. Нажмите «Установить Vexel» — APK установится и откроется в эмуляторе.
            5. Если Vexel не открылся, нажмите «Открыть Vexel» или запустите его из меню эмулятора.

            Можно также перекинуть DevAI-Android.apk на телефон и установить вручную.
            Vexel остаётся Android-конструктором; в PC-версии он не удалён и не изменён.

            Конструктор взят у https://t.me/cheburek_dev, Разрешение есть,
            оригинальный конструктор Vexel скачивать с этого тгк.
        """.trimIndent()
        JOptionPane.showMessageDialog(this, text, "Vexel — инструкция", JOptionPane.INFORMATION_MESSAGE)
    }

    private fun browse(url: String) {
        runCatching { Desktop.getDesktop().browse(URI(url)) }
            .onFailure { JOptionPane.showMessageDialog(this, "Не удалось открыть ссылку:\n$url", "Vexel", JOptionPane.ERROR_MESSAGE) }
    }
}
