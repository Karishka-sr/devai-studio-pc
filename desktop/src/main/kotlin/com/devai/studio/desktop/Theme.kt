package com.devai.studio.desktop

import java.awt.Color
import java.awt.Font
import java.awt.Image
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.UIManager
import javax.swing.border.Border

object Theme {
    val bg = Color(0x0B1020)
    val panel = Color(0x111827)
    val surface = Color(0x172033)
    val surface2 = Color(0x1E293B)
    val accent = Color(0x22D3EE)
    val accent2 = Color(0x8B5CF6)
    val text = Color(0xE5E7EB)
    val muted = Color(0x94A3B8)
    val danger = Color(0xF87171)
    val success = Color(0x34D399)

    fun install(targetWindows: Int) {
        if (targetWindows == 7) {
            System.setProperty("sun.java2d.noddraw", "true")
            runCatching { UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel") }
        } else {
            runCatching { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel") }
        }
        UIManager.put("control", panel)
        UIManager.put("info", surface)
        UIManager.put("nimbusBase", surface2)
        UIManager.put("nimbusBlueGrey", surface2)
        UIManager.put("nimbusLightBackground", panel)
        UIManager.put("text", text)
        UIManager.put("Label.foreground", text)
        UIManager.put("Panel.background", bg)
        UIManager.put("TabbedPane.background", bg)
        UIManager.put("TabbedPane.foreground", text)
        UIManager.put("TabbedPane.selected", surface)
        UIManager.put("TabbedPane.contentAreaColor", panel)
        UIManager.put("ScrollPane.background", panel)
        UIManager.put("Viewport.background", panel)
        UIManager.put("TextArea.background", surface)
        UIManager.put("TextArea.foreground", text)
        UIManager.put("TextField.background", surface)
        UIManager.put("TextField.foreground", text)
        UIManager.put("ComboBox.background", surface)
        UIManager.put("ComboBox.foreground", text)
        UIManager.put("OptionPane.background", panel)
        UIManager.put("OptionPane.messageForeground", text)
    }

    fun font(size: Int, bold: Boolean = false): Font =
        Font(if (System.getProperty("os.name").lowercase().contains("windows")) "Segoe UI" else "Dialog", if (bold) Font.BOLD else Font.PLAIN, size)

    fun panel(color: Color = bg): JPanel = JPanel().apply { background = color }

    fun label(value: String, size: Int = 13, color: Color = text, bold: Boolean = false): JLabel =
        JLabel(value).apply { foreground = color; font = font(size, bold) }

    fun card(title: String? = null): JPanel = JPanel().apply {
        background = panel
        border = cardBorder(title)
    }

    fun cardBorder(title: String? = null): Border {
        val line = BorderFactory.createLineBorder(surface2, 1, true)
        val pad = BorderFactory.createEmptyBorder(14, 14, 14, 14)
        return if (title == null) BorderFactory.createCompoundBorder(line, pad)
        else BorderFactory.createCompoundBorder(line, BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(line, title, 0, 0, font(12, true), accent), pad,
        ))
    }

    fun button(value: String, primary: Boolean = false): JButton = JButton(value).apply {
        font = font(13, true)
        isFocusPainted = false
        isOpaque = true
        background = if (primary) accent else surface2
        foreground = if (primary) Color(0x06121F) else Theme.text
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(if (primary) accent else surface2, 1, true),
            BorderFactory.createEmptyBorder(8, 14, 8, 14),
        )
        cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
    }

    fun field(columns: Int = 20): JTextField = JTextField(columns).apply {
        font = font(13)
        background = surface
        foreground = Theme.text
        caretColor = accent
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(surface2, 1, true),
            BorderFactory.createEmptyBorder(7, 9, 7, 9),
        )
    }

    fun area(rows: Int = 5): JTextArea = JTextArea(rows, 20).apply {
        font = font(13)
        background = surface
        foreground = Theme.text
        caretColor = accent
        lineWrap = true
        wrapStyleWord = true
        border = BorderFactory.createEmptyBorder(8, 9, 8, 9)
    }

    fun style(component: JComponent) {
        component.background = surface
        component.foreground = text
        component.font = font(13)
    }

    fun icon(): Image? = runCatching {
        val stream = Theme::class.java.getResourceAsStream("/icon.png") ?: return null
        ImageIO.read(stream)
    }.getOrNull()
}
