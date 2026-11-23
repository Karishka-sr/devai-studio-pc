package com.devai.studio.desktop

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.text.Style
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

class ChatPanel(
    private val settings: AppSettings,
    private val status: (String) -> Unit,
) : JPanel(BorderLayout(0, 10)) {
    private val transcript = JTextPane()
    private val input = Theme.area(3)
    private val sendButton = Theme.button("Отправить", true)
    private val stopButton = Theme.button("Стоп")
    private val searchBox = JCheckBox("Веб-поиск", settings.searchEnabled)
    private val languageBox = JComboBox(CodeLanguages.all.toTypedArray())
    private val providerLabel = Theme.label("", 12, Theme.muted)
    private val modelLabel = Theme.label("", 12, Theme.muted)
    private val messages = mutableListOf<Message>()
    private val busy = AtomicBoolean(false)
    private val cancelled = AtomicBoolean(false)
    private val userStyle: Style
    private val aiStyle: Style
    private val noteStyle: Style
    private val errorStyle: Style
    init {
        background = Theme.bg
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        val doc = transcript.styledDocument
        userStyle = doc.addStyle("user", null).apply {
            StyleConstants.setForeground(this, Theme.accent)
            StyleConstants.setBold(this, true)
            StyleConstants.setFontSize(this, 13)
        }
        aiStyle = doc.addStyle("ai", null).apply {
            StyleConstants.setForeground(this, Theme.text)
            StyleConstants.setFontSize(this, 14)
        }
        noteStyle = doc.addStyle("note", null).apply {
            StyleConstants.setForeground(this, Theme.muted)
            StyleConstants.setFontSize(this, 12)
        }
        errorStyle = doc.addStyle("error", null).apply {
            StyleConstants.setForeground(this, Theme.danger)
            StyleConstants.setFontSize(this, 13)
        }
        buildUi()
        refresh()
        appendWelcome()
    }
    private fun buildUi() {
        transcript.isEditable = false
        transcript.background = Theme.surface
        transcript.foreground = Theme.text
        transcript.font = Theme.font(14)
        transcript.border = BorderFactory.createEmptyBorder(12, 14, 12, 14)
        transcript.caretColor = Theme.accent
        val scroll = JScrollPane(transcript).apply {
            border = BorderFactory.createLineBorder(Theme.surface2, 1, true)
            verticalScrollBar.unitIncrement = 18
        }
        val top = JPanel(BorderLayout()).apply { background = Theme.bg; border = BorderFactory.createEmptyBorder(0, 0, 4, 0) }
        val titles = JPanel(GridLayout(2, 1, 0, 2)).apply { background = Theme.bg }
        titles.add(Theme.label("Чат с DevAI", 20, Theme.text, true))
        val meta = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply { background = Theme.bg }
        meta.add(providerLabel)
        meta.add(Theme.label("·", 12, Theme.muted))
        meta.add(modelLabel)
        titles.add(meta)
        top.add(titles, BorderLayout.WEST)
        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).apply { background = Theme.bg }
        val clear = Theme.button("Новый чат")
        val save = Theme.button("Сохранить")
        clear.addActionListener { clearChat() }
        save.addActionListener { saveChat() }
        actions.add(clear)
        actions.add(save)
        top.add(actions, BorderLayout.EAST)
        val quick = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply { background = Theme.bg }
        languageBox.selectedItem = CodeLanguages.selected(settings.codeLanguage)
        languageBox.font = Theme.font(12)
        languageBox.background = Theme.panel
        languageBox.foreground = Theme.text
        languageBox.toolTipText = "Основной язык программирования для ответов"
        languageBox.addActionListener {
            val picked = languageBox.selectedItem as? CodeLanguage ?: return@addActionListener
            settings.codeLanguage = picked.id
            SettingsStore.save(settings)
            status("Язык кода: ${picked.name}")
        }
        quick.add(languageBox)
        listOf(
            "Объясни этот код" to "Объясни этот код и укажи возможные ошибки:\n",
            "Найди ошибку" to "Помоги найти ошибку в этом коде:\n",
            "Сгенерируй экран" to "Сгенерируй Compose-экран для Android:\n",
            "Код" to "Сгенерируй рабочий пример кода:\n",
            "Vexel" to "Как добавить экран в Vexel-конструктор?\n",
        ).forEach { (title, prompt) ->
            val b = Theme.button(title)
            b.addActionListener { input.text = prompt; input.requestFocus() }
            quick.add(b)
        }
        val inputScroll = JScrollPane(input).apply {
            border = BorderFactory.createLineBorder(Theme.surface2, 1, true)
            preferredSize = Dimension(100, 86)
        }
        val bottomButtons = JPanel(BorderLayout()).apply { background = Theme.bg }
        val left = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply { background = Theme.bg }
        searchBox.background = Theme.bg
        searchBox.foreground = Theme.text
        searchBox.font = Theme.font(12)
        searchBox.toolTipText = "Добавить результаты веб-поиска в контекст ответа"
        searchBox.addActionListener { settings.searchEnabled = searchBox.isSelected; SettingsStore.save(settings) }
        left.add(searchBox)
        left.add(Theme.label("Ctrl+Enter — отправить", 11, Theme.muted))
        bottomButtons.add(left, BorderLayout.WEST)
        val right = JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).apply { background = Theme.bg }
        stopButton.isEnabled = false
        stopButton.addActionListener { cancelled.set(true); status("Останавливаю…") }
        sendButton.addActionListener { send() }
        right.add(stopButton)
        right.add(sendButton)
        bottomButtons.add(right, BorderLayout.EAST)
        val inputPanel = JPanel(BorderLayout(0, 6)).apply { background = Theme.bg }
        inputPanel.add(quick, BorderLayout.NORTH)
        inputPanel.add(inputScroll, BorderLayout.CENTER)
        inputPanel.add(bottomButtons, BorderLayout.SOUTH)
        add(top, BorderLayout.NORTH)
        add(scroll, BorderLayout.CENTER)
        add(inputPanel, BorderLayout.SOUTH)
        input.inputMap.put(KeyStroke.getKeyStroke("ctrl ENTER"), "send")
        input.actionMap.put("send", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) = send()
        })
    }
    fun refresh() {
        providerLabel.text = settings.provider.title
        modelLabel.text = settings.model.ifBlank { "модель не выбрана" }
        searchBox.isSelected = settings.searchEnabled
        val selectedLanguage = CodeLanguages.selected(settings.codeLanguage)
        if (languageBox.selectedItem != selectedLanguage) languageBox.selectedItem = selectedLanguage
    }

    fun focusInput() = input.requestFocus()

    private fun send() {
        if (busy.get()) return
        val text = input.text.trim()
        if (text.isEmpty()) return
        if (settings.apiKey.isBlank()) {
            appendError("Сначала укажите API-ключ на вкладке «Настройки».\n")
            status("Нужен API-ключ")
            return
        }
        messages.add(Message("user", text))
        appendHeader("Вы", userStyle)
        append(text + "\n", aiStyle)
        input.text = ""
        cancelled.set(false)
        setBusy(true)
        status("DevAI думает…")
        Thread({ requestAnswer() }, "devai-chat").apply { isDaemon = true }.start()
    }
    private fun requestAnswer() {
        var system = settings.systemPrompt
        system += "\n\n" + CodeLanguages.instruction(CodeLanguages.selected(settings.codeLanguage))
        if (settings.searchEnabled) {
            SwingUtilities.invokeLater { status("Ищу в интернете…") }
            val query = messages.lastOrNull { it.role == "user" }?.content.orEmpty()
            val found = runCatching { SearchClient.search(settings, query, 5) }.getOrNull()
            if (!found.isNullOrBlank()) {
                system += "\n\nАктуальные результаты веб-поиска (используй их, если относятся к вопросу):\n" + found
            }
        }
        val context = ArrayList<Message>()
        context.add(Message("system", system))
        context.addAll(messages.takeLast(20))
        val started = AtomicBoolean(false)
        try {
            val answer = AiClient.chat(settings, context) { delta ->
                if (!cancelled.get()) {
                    SwingUtilities.invokeLater {
                        if (started.compareAndSet(false, true)) appendHeader("DevAI", userStyle)
                        append(delta, aiStyle)
                    }
                }
            }
            if (cancelled.get()) {
                SwingUtilities.invokeLater { append("\n[остановлено]\n", noteStyle) }
            } else {
                messages.add(Message("assistant", answer))
                SwingUtilities.invokeLater {
                    if (!started.get()) {
                        appendHeader("DevAI", userStyle)
                        append(answer, aiStyle)
                    }
                    append("\n", aiStyle)
                    status("Готово")
                }
            }
        } catch (e: Exception) {
            SwingUtilities.invokeLater {
                appendError("\nОшибка: ${e.message ?: e.javaClass.simpleName}\n")
                status("Ошибка запроса")
            }
        } finally {
            setBusy(false)
        }
    }
    private fun appendWelcome() {
        appendHeader("DevAI Studio", userStyle)
        append("Готов помочь с Android, Kotlin, Compose, 50 языками программирования и Vexel. Выберите провайдера и модель в «Настройках», затем напишите задачу.\n", noteStyle)
    }

    private fun appendHeader(title: String, style: Style) {
        val doc = transcript.styledDocument
        if (doc.length > 0) doc.insertString(doc.length, "\n", noteStyle)
        doc.insertString(doc.length, "$title\n", style)
        scrollToEnd()
    }

    private fun append(value: String, style: Style) {
        val doc = transcript.styledDocument
        doc.insertString(doc.length, value, style)
        scrollToEnd()
    }

    private fun appendError(value: String) = append(value, errorStyle)

    private fun scrollToEnd() {
        transcript.caretPosition = transcript.document.length
    }

    private fun clearChat() {
        if (busy.get()) return
        messages.clear()
        transcript.styledDocument.remove(0, transcript.document.length)
        appendWelcome()
        status("Новый чат")
    }

    private fun saveChat() {
        runCatching {
            Files.write(AppPaths.historyFile, transcript.text.toByteArray(StandardCharsets.UTF_8))
            status("История сохранена: ${AppPaths.historyFile}")
        }.onFailure { status("Не удалось сохранить: ${it.message}") }
    }

    private fun setBusy(value: Boolean) {
        busy.set(value)
        SwingUtilities.invokeLater {
            sendButton.isEnabled = !value
            stopButton.isEnabled = value
            input.isEnabled = !value
        }
    }
}
