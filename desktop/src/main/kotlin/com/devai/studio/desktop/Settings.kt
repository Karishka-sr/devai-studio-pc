package com.devai.studio.desktop

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties

enum class Provider(val title: String, val defaultBaseUrl: String, val defaultModel: String) {
    OPENAI("OpenAI-совместимый", "https://api.openai.com/v1", "gpt-4o-mini"),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta", "gemini-2.0-flash"),
    ANTHROPIC("Anthropic Claude", "https://api.anthropic.com", "claude-3-5-sonnet-latest"),
    CUSTOM("Свой OpenAI-совместимый", "http://localhost:11434/v1", "llama3.1"),
}

data class AppSettings(
    var provider: Provider = Provider.OPENAI,
    var baseUrl: String = Provider.OPENAI.defaultBaseUrl,
    var model: String = Provider.OPENAI.defaultModel,
    var apiKey: String = "",
    var temperature: Double = 0.7,
    var topP: Double = 0.95,
    var maxTokens: Int = 2048,
    var timeoutSeconds: Int = 120,
    var systemPrompt: String = "Ты DevAI Studio — помощник разработчика. Отвечай по делу, код оформляй блоками.",
    var searchEnabled: Boolean = false,
    var tavilyKey: String = "",
    var githubToken: String = "",
    var targetWindows: Int = OsInfo.detectedWindows(),
    var codeLanguage: String = CodeLanguages.DEFAULT_ID,
) {
    fun copyForProvider(provider: Provider): AppSettings = copy(
        provider = provider,
        baseUrl = provider.defaultBaseUrl,
        model = provider.defaultModel,
    )
}

object AppPaths {
    val appDir: Path by lazy {
        runCatching {
            val location = AppPaths::class.java.protectionDomain.codeSource.location
            val path = Paths.get(location.toURI())
            if (Files.isRegularFile(path)) path.parent else path
        }.getOrElse { Paths.get(System.getProperty("user.dir")) }
    }

    val home: Path = run {
        val appData = System.getenv("APPDATA")
        if (appData.isNullOrBlank()) Paths.get(System.getProperty("user.home"), ".devai-studio")
        else Paths.get(appData, "DevAI Studio")
    }

    val settingsFile: Path get() = home.resolve("settings.properties")
    val historyFile: Path get() = home.resolve("history.txt")

    fun ensure() {
        Files.createDirectories(home)
    }

    fun adb(): Path? {
        val exe = if (System.getProperty("os.name").lowercase().contains("windows")) "adb.exe" else "adb"
        return listOf(
            appDir.parent.resolve("platform-tools").resolve(exe),
            appDir.resolve("platform-tools").resolve(exe),
            Paths.get(System.getProperty("user.dir"), "platform-tools", exe),
        ).firstOrNull { Files.isRegularFile(it) }
    }

    fun installedApk(): Path {
        val candidates = listOf(
            appDir.parent.resolve("DevAI-Android.apk"),
            appDir.resolve("DevAI-Android.apk"),
            Paths.get(System.getProperty("user.dir"), "DevAI-Android.apk"),
            Paths.get(System.getProperty("user.dir"), "app", "DevAI-Android.apk"),
            home.resolve("DevAI-Android.apk"),
        )
        return candidates.firstOrNull { Files.isRegularFile(it) } ?: candidates.first()
    }
}

object SettingsStore {
    fun load(): AppSettings {
        AppPaths.ensure()
        val defaults = AppSettings(targetWindows = readInstalledTarget())
        val file = AppPaths.settingsFile.toFile()
        if (!file.isFile) return defaults
        val p = Properties()
        try {
            FileInputStream(file).use { p.load(it) }
        } catch (_: Exception) {
            return defaults
        }
        val provider = runCatching { Provider.valueOf(p.getProperty("provider", defaults.provider.name)) }
            .getOrDefault(defaults.provider)
        return defaults.copy(
            provider = provider,
            baseUrl = p.getProperty("baseUrl", provider.defaultBaseUrl),
            model = p.getProperty("model", provider.defaultModel),
            apiKey = p.getProperty("apiKey", ""),
            temperature = p.getProperty("temperature")?.toDoubleOrNull() ?: defaults.temperature,
            topP = p.getProperty("topP")?.toDoubleOrNull() ?: defaults.topP,
            maxTokens = p.getProperty("maxTokens")?.toIntOrNull() ?: defaults.maxTokens,
            timeoutSeconds = p.getProperty("timeoutSeconds")?.toIntOrNull() ?: defaults.timeoutSeconds,
            systemPrompt = p.getProperty("systemPrompt", defaults.systemPrompt),
            searchEnabled = p.getProperty("searchEnabled")?.toBoolean() ?: defaults.searchEnabled,
            tavilyKey = p.getProperty("tavilyKey", ""),
            githubToken = p.getProperty("githubToken", ""),
            targetWindows = p.getProperty("targetWindows")?.toIntOrNull() ?: defaults.targetWindows,
            codeLanguage = p.getProperty("codeLanguage", defaults.codeLanguage),
        )
    }

    fun save(s: AppSettings) {
        AppPaths.ensure()
        val p = Properties()
        p.setProperty("provider", s.provider.name)
        p.setProperty("baseUrl", s.baseUrl)
        p.setProperty("model", s.model)
        p.setProperty("apiKey", s.apiKey)
        p.setProperty("temperature", s.temperature.toString())
        p.setProperty("topP", s.topP.toString())
        p.setProperty("maxTokens", s.maxTokens.toString())
        p.setProperty("timeoutSeconds", s.timeoutSeconds.toString())
        p.setProperty("systemPrompt", s.systemPrompt)
        p.setProperty("searchEnabled", s.searchEnabled.toString())
        p.setProperty("tavilyKey", s.tavilyKey)
        p.setProperty("githubToken", s.githubToken)
        p.setProperty("targetWindows", s.targetWindows.toString())
        p.setProperty("codeLanguage", s.codeLanguage)
        val tmp = AppPaths.home.resolve("settings.properties.tmp").toFile()
        FileOutputStream(tmp).use { p.store(it, "DevAI Studio") }
        Files.move(tmp.toPath(), AppPaths.settingsFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }

    private fun readInstalledTarget(): Int {
        val candidates = listOf(AppPaths.appDir.parent.resolve("target.ini"), Paths.get(System.getProperty("user.dir"), "target.ini"))
        val ini = candidates.firstOrNull { Files.isRegularFile(it) }?.toFile() ?: return OsInfo.detectedWindows()
        return try {
            ini.readLines()
                .firstOrNull { it.trim().startsWith("target=") }
                ?.substringAfter("=")?.trim()?.toIntOrNull()
                ?: OsInfo.detectedWindows()
        } catch (_: Exception) {
            OsInfo.detectedWindows()
        }
    }
}

object OsInfo {
    fun detectedWindows(): Int {
        val name = System.getProperty("os.name", "").lowercase()
        if (!name.contains("windows")) return 10
        val version = System.getProperty("os.version", "")
        val parts = version.split('.').mapNotNull { it.toIntOrNull() }
        val major = parts.getOrNull(0) ?: 6
        val minor = parts.getOrNull(1) ?: 1
        val build = parts.getOrNull(2) ?: 0
        return when {
            major >= 10 && build >= 22000 -> 11
            major >= 10 -> 10
            major == 6 && minor == 1 -> 7
            else -> 10
        }
    }

    fun title(version: Int): String = when (version) {
        7 -> "Windows 7"
        11 -> "Windows 11"
        else -> "Windows 10"
    }
}
