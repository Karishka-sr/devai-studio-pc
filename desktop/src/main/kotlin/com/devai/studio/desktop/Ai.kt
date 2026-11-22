package com.devai.studio.desktop

import org.json.JSONArray
import org.json.JSONObject

data class Message(val role: String, val content: String)

object PromptSanitizer {
    private val controlToken = Regex(
        """<\|(?:bos|eos|unk|pad|s|start_of_turn|end_of_turn|im_start|im_end|system|user|assistant|endoftext|begin_of_text|end_of_text|eot_id|start_header_id|end_header_id|end|tool)\|>|</?(?:bos|eos|unk|pad|s|start_of_turn|end_of_turn)>""",
        RegexOption.IGNORE_CASE,
    )

    fun clean(text: String): String = controlToken.replace(text, "")
        .filter { it == '\n' || it == '\t' || it.code >= 32 }
        .trim()
        .ifBlank { " " }
}

object AiClient {
    fun chat(
        settings: AppSettings,
        messages: List<Message>,
        onDelta: (String) -> Unit,
    ): String = when (settings.provider) {
        Provider.GEMINI -> gemini(settings, messages, onDelta)
        Provider.ANTHROPIC -> anthropic(settings, messages, onDelta)
        Provider.OPENAI, Provider.CUSTOM -> openAi(settings, messages, onDelta)
    }

    private fun openAi(
        settings: AppSettings,
        messages: List<Message>,
        onDelta: (String) -> Unit,
    ): String {
        val base = settings.baseUrl.trimEnd('/')
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        // stream
        val payload = JSONObject().apply {
            put("model", settings.model)
            put("stream", true)
            put("temperature", settings.temperature)
            put("top_p", settings.topP)
            put("max_tokens", settings.maxTokens)
            put("messages", JSONArray().apply {
                messages.forEach { put(JSONObject().put("role", it.role).put("content", PromptSanitizer.clean(it.content))) }
            })
        }
        val sb = StringBuilder()
        var saw = false
        Http.stream(url, "POST", mapOf("Authorization" to "Bearer ${settings.apiKey}", "Content-Type" to "application/json"), payload.toString(), settings.timeoutSeconds) { line ->

            if (!line.startsWith("data:")) return@stream
            saw = true
            val data = line.removePrefix("data:").trim()
            if (data.isEmpty()) return@stream
            if (data == "DONE") return@stream
            runCatching {
                val choice = JSONObject(data).optJSONArray("choices")?.optJSONObject(0)
                val text = choice?.optJSONObject("delta")?.text("content").orEmpty()
                if (text.isNotEmpty()) { sb.append(text); onDelta(text) }
            }
        }
        if (!saw) error("Сервер не вернул поток данных")
        if (sb.isEmpty()) error("Пустой ответ сервера")
        return sb.toString()
    }















    private fun gemini(
        settings: AppSettings,
        messages: List<Message>,
        onDelta: (String) -> Unit,
    ): String {
        val base = settings.baseUrl.trimEnd('/')
        val url = "$base/models/${Http.encode(settings.model)}:streamGenerateContent?alt=sse&key=${Http.encode(settings.apiKey)}"
        val contents = JSONArray()
        messages.filter { it.role != "system" }.forEach { m ->
            val role = if (m.role == "assistant") "model" else "user"
            val part = JSONObject().put("text", PromptSanitizer.clean(m.content))
            contents.put(JSONObject().put("role", role).put("parts", JSONArray().put(part)))
        }
        val payload = JSONObject().apply {
            put("contents", contents)
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", PromptSanitizer.clean(settings.systemPrompt)))))
            put("generationConfig", JSONObject().apply {
                put("temperature", settings.temperature)
                put("topP", settings.topP)
                put("maxOutputTokens", settings.maxTokens)
            })
        }
        val sb = StringBuilder()
        var saw = false
        Http.stream(url, "POST", mapOf("Content-Type" to "application/json"), payload.toString(), settings.timeoutSeconds) { line ->
            if (!line.startsWith("data:")) return@stream
            saw = true
            val data = line.removePrefix("data:").trim()
            if (data.isEmpty() || data == "DONE") return@stream
            runCatching {
                val parts = JSONObject(data).optJSONArray("candidates")
                    ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                if (parts != null) for (i in 0 until parts.length()) {
                    val text = parts.optJSONObject(i)?.text("text").orEmpty()
                    if (text.isNotEmpty()) { sb.append(text); onDelta(text) }
                }
            }
        }
        if (!saw) error("Gemini не вернул поток данных")
        if (sb.isEmpty()) error("Пустой ответ Gemini")
        return sb.toString()
    }
    private fun anthropic(
        settings: AppSettings,
        messages: List<Message>,
        onDelta: (String) -> Unit,
    ): String {
        val base = settings.baseUrl.trimEnd('/')
        val url = if (base.endsWith("/v1/messages")) base else "$base/v1/messages"
        val msgs = JSONArray()
        messages.filter { it.role != "system" }.forEach { m ->
            msgs.put(JSONObject().put("role", m.role).put("content", PromptSanitizer.clean(m.content)))
        }
        val payload = JSONObject().apply {
            put("model", settings.model)
            put("max_tokens", settings.maxTokens)
            put("temperature", settings.temperature)
            put("system", PromptSanitizer.clean(settings.systemPrompt))
            put("messages", msgs)
            put("stream", true)
        }
        val headers = mapOf(
            "x-api-key" to settings.apiKey,
            "anthropic-version" to "2023-06-01",
            "Content-Type" to "application/json",
        )
        val sb = StringBuilder()
        var saw = false
        Http.stream(url, "POST", headers, payload.toString(), settings.timeoutSeconds) { line ->
            if (!line.startsWith("data:")) return@stream
            saw = true
            val data = line.removePrefix("data:").trim()
            if (data.isEmpty() || data == "DONE") return@stream
            runCatching {
                val obj = JSONObject(data)
                if (obj.text("type") == "error") error(obj.toString())
                val text = obj.optJSONObject("delta")?.text("text").orEmpty()
                if (text.isNotEmpty()) { sb.append(text); onDelta(text) }
            }
        }
        if (!saw) error("Claude не вернул поток данных")
        if (sb.isEmpty()) error("Пустой ответ Claude")
        return sb.toString()
    }
}
