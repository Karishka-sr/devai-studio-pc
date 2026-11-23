package com.devai.studio.desktop

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities

fun main(args: Array<String>) {
    CodeLanguages.load()
    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")
    val settings = SettingsStore.load()
    args.firstOrNull { it.startsWith("--target=") }?.substringAfter("=")?.toIntOrNull()?.let { settings.targetWindows = it }
    Theme.install(settings.targetWindows)
    SwingUtilities.invokeLater {
        MainFrame(settings).isVisible = true
    }
}

class MainFrame(private val settings: AppSettings) : JFrame("DevAI Studio") {
    private val statusLabel = Theme.label("Готов", 12, Theme.muted)
    private val osBadge = Theme.label("", 11, Theme.accent)
    private val providerBadge = Theme.label("", 11, Theme.muted)
    private val chat = ChatPanel(settings) { statusLabel.text = it }
    private val search = SearchPanel(settings)
    private val vexel = VexelPanel()
    private val settingsPanel = SettingsPanel(settings) { refreshBadges(); chat.refresh() }
    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(920, 620)
        size = Dimension(1180, 780)
        setLocationRelativeTo(null)
        contentPane.background = Theme.bg
        rootPane.border = BorderFactory.createEmptyBorder(12, 14, 12, 14)
        Theme.icon()?.let { setIconImage(it) }
        val header = JPanel(BorderLayout()).apply {
            background = Theme.panel
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.surface2, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12),
            )
        }
        val brand = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0)).apply { background = Theme.panel }
        Theme.icon()?.let {
            val scaled = it.getScaledInstance(42, 42, java.awt.Image.SCALE_SMOOTH)
            brand.add(JLabel(javax.swing.ImageIcon(scaled)))
        }
        val names = JPanel().apply {
            background = Theme.panel
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            add(Theme.label("DevAI Studio", 18, Theme.text, true))
            add(Theme.label("PC-версия 3.2.0 · 75 языков · Android + Vexel", 11, Theme.muted))
        }
        brand.add(names)
        header.add(brand, BorderLayout.WEST)
        val badges = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply { background = Theme.panel }
        badges.add(osBadge)
        badges.add(providerBadge)
        header.add(badges, BorderLayout.EAST)
        val tabs = JTabbedPane().apply {
            background = Theme.bg
            foreground = Theme.text
            font = Theme.font(13, true)
            border = BorderFactory.createEmptyBorder(8, 0, 0, 0)
        }
        tabs.addTab("Чат", chat)
        tabs.addTab("Поиск", search)
        tabs.addTab("Vexel", vexel)
        tabs.addTab("Настройки", settingsPanel)
        tabs.addChangeListener { if (tabs.selectedIndex == 0) chat.focusInput() }
        val status = JPanel(BorderLayout()).apply {
            background = Theme.bg
            border = BorderFactory.createEmptyBorder(6, 2, 0, 2)
        }
        status.add(statusLabel, BorderLayout.WEST)
        status.add(Theme.label("Vexel · разрешение от @cheburek_dev", 11, Theme.muted), BorderLayout.EAST)
        add(header, BorderLayout.NORTH)
        add(tabs, BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                SettingsStore.save(settings)
            }
        })
        refreshBadges()
    }

    private fun refreshBadges() {
        osBadge.text = "● ${OsInfo.title(settings.targetWindows)}"
        providerBadge.text = "● ${settings.provider.title} / ${settings.model}"
    }
}
