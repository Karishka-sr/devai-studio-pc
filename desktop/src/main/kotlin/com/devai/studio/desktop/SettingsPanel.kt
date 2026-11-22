package com.devai.studio.desktop

import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities

class SettingsPanel(
    private val settings: AppSettings,
    private val onSaved: () -> Unit,
) : JPanel(BorderLayout(0, 10)) {
    private val provider = JComboBox(Provider.values().map { it.title }.toTypedArray())
    private val baseUrl = Theme.field(34)
    private val model = Theme.field(28)
    private val apiKey = JPasswordField(34)
    private val showKey = JCheckBox("Показать ключ")
    private val temperature = JSpinner(SpinnerNumberModel(0.7, 0.0, 2.0, 0.1))
    private val topP = JSpinner(SpinnerNumberModel(0.95, 0.0, 1.0, 0.05))
    private val maxTokens = JSpinner(SpinnerNumberModel(2048, 64, 32768, 64))
    private val timeout = JSpinner(SpinnerNumberModel(120, 15, 600, 15))
    private val systemPrompt = Theme.area(5)
    private val tavilyKey = Theme.field(34)
    private val githubToken = Theme.field(34)
    private val searchDefault = JCheckBox("Включать веб-поиск по умолчанию")
    private val statusLabel = Theme.label("", 12, Theme.muted)
    init {
        background = Theme.bg
        buildUi()
        load()
        provider.addActionListener {
            val p = Provider.values()[provider.selectedIndex]
            baseUrl.text = p.defaultBaseUrl
            model.text = p.defaultModel
        }
        showKey.addActionListener { apiKey.echoChar = if (showKey.isSelected) 0.toChar() else '•' }
    }
    private fun buildUi() {
        val top = JPanel(BorderLayout()).apply { background = Theme.bg }
        top.add(Theme.label("Настройки", 20, Theme.text, true), BorderLayout.WEST)
        top.add(Theme.label("Целевая ОС: ${OsInfo.title(settings.targetWindows)}", 12, Theme.muted), BorderLayout.EAST)
        val content = JPanel().apply {
            background = Theme.bg
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(4, 2, 4, 10)
        }
        content.add(section("Провайдер и модель",
            row("Провайдер", provider),
            row("Base URL", baseUrl, "Для OpenAI, OpenRouter, Groq, DeepSeek, Mistral, Ollama и других совместимых API"),
            row("Модель", model),
            row("API-ключ", apiKey, "Ключ хранится локально в %APPDATA%\\DevAI Studio\\settings.properties"),
            row("", showKey),
        ))
        content.add(section("Параметры генерации",
            row("Температура", temperature),
            row("Top P", topP),
            row("Максимум токенов", maxTokens),
            row("Таймаут, секунд", timeout),
        ))
        content.add(section("Системный промпт", row("Инструкция для модели", scroll(systemPrompt, 100))))
        content.add(section("Веб-поиск",
            row("Tavily API-ключ (необязательно)", tavilyKey, "Без ключа работают GitHub, Stack Overflow, Wikipedia, DuckDuckGo и Hacker News"),
            row("GitHub token (необязательно)", githubToken, "Нужен для поиска по коду и увеличения лимита GitHub API"),
            row("", searchDefault),
        ))
        val scroll = JScrollPane(content).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 18
        }
        add(top, BorderLayout.NORTH)
        add(scroll, BorderLayout.CENTER)
        val bottom = JPanel(BorderLayout()).apply { background = Theme.bg }
        bottom.add(statusLabel, BorderLayout.WEST)
        val buttons = JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).apply { background = Theme.bg }
        val folder = Theme.button("Папка настроек")
        val test = Theme.button("Проверить")
        val save = Theme.button("Сохранить", true)
        folder.addActionListener { runCatching { Desktop.getDesktop().open(AppPaths.home.toFile()) } }
        test.addActionListener { testConnection() }
        save.addActionListener { save() }
        buttons.add(folder)
        buttons.add(test)
        buttons.add(save)
        bottom.add(buttons, BorderLayout.EAST)
        add(bottom, BorderLayout.SOUTH)
    }
    private fun load() {
        provider.selectedIndex = settings.provider.ordinal
        baseUrl.text = settings.baseUrl
        model.text = settings.model
        apiKey.text = settings.apiKey
        apiKey.echoChar = '•'
        temperature.value = settings.temperature
        topP.value = settings.topP
        maxTokens.value = settings.maxTokens
        timeout.value = settings.timeoutSeconds
        systemPrompt.text = settings.systemPrompt
        tavilyKey.text = settings.tavilyKey
        githubToken.text = settings.githubToken
        searchDefault.isSelected = settings.searchEnabled
        searchDefault.background = Theme.panel
        searchDefault.foreground = Theme.text
    }

    private fun save() {
        val p = Provider.values()[provider.selectedIndex]
        if (baseUrl.text.isBlank() || model.text.isBlank()) {
            statusLabel.text = "Заполните Base URL и модель"
            return
        }
        settings.provider = p
        settings.baseUrl = baseUrl.text.trim()
        settings.model = model.text.trim()
        settings.apiKey = String(apiKey.password)
        settings.temperature = (temperature.value as Number).toDouble()
        settings.topP = (topP.value as Number).toDouble()
        settings.maxTokens = (maxTokens.value as Number).toInt()
        settings.timeoutSeconds = (timeout.value as Number).toInt()
        settings.systemPrompt = systemPrompt.text
        settings.tavilyKey = tavilyKey.text.trim()
        settings.githubToken = githubToken.text.trim()
        settings.searchEnabled = searchDefault.isSelected
        SettingsStore.save(settings)
        statusLabel.text = "Сохранено"
        onSaved()
    }
    private fun testConnection() {
        if (apiKey.password.isEmpty()) {
            statusLabel.text = "Сначала укажите API-ключ"
            return
        }
        val p = Provider.values()[provider.selectedIndex]
        val probe = settings.copy(
            provider = p,
            baseUrl = baseUrl.text.trim(),
            model = model.text.trim(),
            apiKey = String(apiKey.password),
            temperature = 0.1,
            maxTokens = 64,
            timeoutSeconds = 45,
        )
        statusLabel.text = "Проверяю подключение…"
        Thread({
            val result = runCatching {
                AiClient.chat(probe, listOf(Message("user", "Ответь одним словом: работает")), {})
            }
            SwingUtilities.invokeLater {
                result.onSuccess {
                    statusLabel.text = "Подключение работает"
                    JOptionPane.showMessageDialog(this, "Провайдер ответил:\n${it.take(300)}", "Проверка", JOptionPane.INFORMATION_MESSAGE)
                }.onFailure {
                    statusLabel.text = "Ошибка подключения"
                    JOptionPane.showMessageDialog(this, "Не удалось подключиться:\n${it.message}", "Проверка", JOptionPane.ERROR_MESSAGE)
                }
            }
        }, "devai-test").apply { isDaemon = true }.start()
    }
    private fun section(title: String, vararg rows: JComponent): JPanel {
        val p = Theme.card(title)
        p.layout = BoxLayout(p, BoxLayout.Y_AXIS)
        rows.forEach { row ->
            row.alignmentX = 0f
            p.add(row)
            p.add(javax.swing.Box.createVerticalStrut(8))
        }
        p.maximumSize = Dimension(Int.MAX_VALUE, p.preferredSize.height + 40)
        p.alignmentX = 0f
        return p
    }

    private fun row(label: String, component: JComponent, hint: String? = null): JPanel {
        val p = JPanel(BorderLayout(0, 4)).apply { background = Theme.panel }
        if (label.isNotBlank()) p.add(Theme.label(label, 12, Theme.muted), BorderLayout.NORTH)
        p.add(component, BorderLayout.CENTER)
        if (hint != null) p.add(Theme.label(hint, 11, Theme.muted), BorderLayout.SOUTH)
        return p
    }

    private fun scroll(component: JComponent, height: Int): JScrollPane = JScrollPane(component).apply {
        preferredSize = Dimension(100, height)
        border = BorderFactory.createLineBorder(Theme.surface2, 1, true)
    }
}
