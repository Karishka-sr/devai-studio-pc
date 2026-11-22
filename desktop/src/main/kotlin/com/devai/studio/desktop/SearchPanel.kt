package com.devai.studio.desktop

import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities

class SearchPanel(private val settings: AppSettings) : JPanel(BorderLayout(0, 10)) {
    private val query = Theme.field(42)
    private val limit = JSpinner(SpinnerNumberModel(5, 1, 8, 1))
    private val output = Theme.area(18)
    private val statusLabel = Theme.label("Готов к поиску", 12, Theme.muted)
    private val searchButton = Theme.button("Найти", true)
    init {
        background = Theme.bg
        output.isEditable = false
        val top = JPanel(BorderLayout()).apply { background = Theme.bg }
        top.add(Theme.label("Веб-поиск", 20, Theme.text, true), BorderLayout.WEST)
        top.add(Theme.label("Tavily + GitHub + Stack Overflow + Wikipedia + DuckDuckGo + Hacker News", 11, Theme.muted), BorderLayout.EAST)
        val controls = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply { background = Theme.bg }
        controls.add(query)
        controls.add(Theme.label("Результатов:", 12, Theme.muted))
        controls.add(limit)
        controls.add(searchButton)
        query.addActionListener { search() }
        searchButton.addActionListener { search() }
        val center = JPanel(BorderLayout(0, 8)).apply { background = Theme.bg }
        center.add(controls, BorderLayout.NORTH)
        center.add(JScrollPane(output).apply {
            border = BorderFactory.createLineBorder(Theme.surface2, 1, true)
            verticalScrollBar.unitIncrement = 18
        }, BorderLayout.CENTER)
        val bottom = JPanel(BorderLayout()).apply { background = Theme.bg }
        bottom.add(statusLabel, BorderLayout.WEST)
        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).apply { background = Theme.bg }
        val copy = Theme.button("Скопировать")
        val clear = Theme.button("Очистить")
        copy.addActionListener {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(output.text), null)
            statusLabel.text = "Скопировано"
        }
        clear.addActionListener { output.text = ""; statusLabel.text = "Готов к поиску" }
        actions.add(copy)
        actions.add(clear)
        bottom.add(actions, BorderLayout.EAST)
        add(top, BorderLayout.NORTH)
        add(center, BorderLayout.CENTER)
        add(bottom, BorderLayout.SOUTH)
    }
    private fun search() {
        val q = query.text.trim()
        if (q.isEmpty()) return
        searchButton.isEnabled = false
        statusLabel.text = "Ищу…"
        output.text = ""
        val max = (limit.value as Number).toInt()
        Thread({
            val result = runCatching { SearchClient.search(settings, q, max) }
            SwingUtilities.invokeLater {
                result.onSuccess {
                    output.text = it
                    output.caretPosition = 0
                    statusLabel.text = "Готово · ${it.length} символов"
                }.onFailure {
                    output.text = "Ошибка: ${it.message}"
                    statusLabel.text = "Ошибка поиска"
                }
                searchButton.isEnabled = true
            }
        }, "devai-search").apply { isDaemon = true }.start()
    }
}
