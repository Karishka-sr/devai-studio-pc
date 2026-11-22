package com.devai.studio.desktop

import org.json.JSONObject
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object SearchClient {
    fun search(settings: AppSettings, query: String, limit: Int = 5): String {
        val q = query.trim()
        require(q.isNotEmpty()) { "Пустой поисковый запрос" }
        val max = limit.coerceIn(1, 8)
        val tasks = listOf<Callable<String?>>(
            Callable { tavily(settings.tavilyKey, q, max) },
            Callable { github(q, settings.githubToken, max) },
            Callable { stackOverflow(q, max) },
            Callable { wikipedia(q, max) },
            Callable { duckDuckGo(q, max) },
            Callable { hackerNews(q, max) },
        )
        val pool = Executors.newFixedThreadPool(tasks.size)
        val sections = try {
            pool.invokeAll(tasks, 40, TimeUnit.SECONDS)
                .mapNotNull { runCatching { it.get() }.getOrNull() }
                .filter { it.isNotBlank() }
        } finally {
            pool.shutdownNow()
        }
        if (sections.isEmpty()) error("Поиск не дал результатов. Проверьте интернет.")
        return sections.joinToString("\n\n").take(9000)
    }
    private fun tavily(key: String, query: String, limit: Int): String? {
        if (key.isBlank()) return null
        val payload = JSONObject().apply {
            put("api_key", key)
            put("query", query)
            put("search_depth", "basic")
            put("max_results", limit)
            put("include_answer", true)
        }
        val json = JSONObject(Http.postJson("https://api.tavily.com/search", payload, timeoutSec = 30))
        val results = json.optJSONArray("results") ?: return null
        val text = buildString {
            append("### Tavily (веб)\n")
            json.text("answer").takeIf { it.isNotBlank() }?.let { append("Краткий ответ: ").append(it).append("\n\n") }
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                append(i + 1).append(". ").append(item.text("title").ifBlank { "Без названия" }).append("\n")
                append(item.text("url")).append("\n")
                item.text("content").take(900).takeIf { it.isNotBlank() }?.let { append(it).append("\n") }
                append("\n")
            }
        }.trim()
        return text.takeIf { it.length > 18 }
    }
    private fun github(query: String, token: String, limit: Int): String? {
        val perPage = limit.coerceAtMost(5)
        val headers = if (token.isNotBlank()) mapOf("Authorization" to "Bearer $token", "Accept" to "application/vnd.github+json") else emptyMap()
        val repos = runCatching { getJson(url("https://api.github.com/search/repositories", mapOf("q" to query, "per_page" to "$perPage", "sort" to "stars", "order" to "desc")), headers) }.getOrNull()
        val issues = runCatching { getJson(url("https://api.github.com/search/issues", mapOf("q" to query, "per_page" to "$perPage", "sort" to "relevance")), headers) }.getOrNull()
        var found = false
        val text = buildString {
            append("### GitHub\n")
            repos?.optJSONArray("items")?.let { items ->
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    found = true
                    append("- Репозиторий: ").append(item.text("full_name")).append(" · ★ ").append(item.optInt("stargazers_count"))
                    item.text("language").takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                    append("\n  ").append(item.text("html_url")).append("\n")
                }
            }
            issues?.optJSONArray("items")?.let { items ->
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    found = true
                    append("- ").append(if (item.has("pull_request")) "PR" else "Issue").append(": ")
                    append(item.text("title")).append("\n  ").append(item.text("html_url")).append("\n")
                }
            }
        }.trim()
        return text.takeIf { found }
    }
    private fun stackOverflow(query: String, limit: Int): String? {
        val json = getJson(url("https://api.stackexchange.com/2.3/search/advanced", mapOf(
            "order" to "desc", "sort" to "relevance", "q" to query, "site" to "stackoverflow", "pagesize" to "${limit.coerceAtMost(5)}",
        )))
        val items = json.optJSONArray("items") ?: return null
        if (items.length() == 0) return null
        return buildString {
            append("### Stack Overflow\n")
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                append(i + 1).append(". ").append(item.text("title")).append("\n")
                append(item.text("link")).append("\nБаллы: ").append(item.optInt("score")).append("\n\n")
            }
        }.trim()
    }
    private fun wikipedia(query: String, limit: Int): String? {
        val json = getJson(url("https://ru.wikipedia.org/w/api.php", mapOf(
            "action" to "query", "list" to "search", "srsearch" to query, "format" to "json", "utf8" to "1", "srlimit" to "${limit.coerceAtMost(5)}",
        )))
        val items = json.optJSONObject("query")?.optJSONArray("search") ?: return null
        if (items.length() == 0) return null
        return buildString {
            append("### Wikipedia\n")
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                append(i + 1).append(". ").append(item.text("title")).append("\n")
                append("https://ru.wikipedia.org/?curid=").append(item.optInt("pageid")).append("\n\n")
            }
        }.trim()
    }
    private fun duckDuckGo(query: String, limit: Int): String? {
        val json = getJson(url("https://api.duckduckgo.com/", mapOf(
            "q" to query, "format" to "json", "no_html" to "1", "skip_disambig" to "1",
        )))
        val abstract = json.text("AbstractText")
        if (abstract.isBlank()) return null
        return buildString {
            append("### DuckDuckGo\n").append(abstract.take(700)).append("\n")
            json.text("AbstractURL").takeIf { it.isNotBlank() }?.let { append(it).append("\n") }
        }.trim()
    }
    private fun hackerNews(query: String, limit: Int): String? {
        val json = getJson(url("https://hn.algolia.com/api/v1/search", mapOf(
            "query" to query, "tags" to "story", "hitsPerPage" to "${limit.coerceAtMost(5)}",
        )))
        val hits = json.optJSONArray("hits") ?: return null
        if (hits.length() == 0) return null
        return buildString {
            append("### Hacker News\n")
            for (i in 0 until hits.length()) {
                val item = hits.optJSONObject(i) ?: continue
                append(i + 1).append(". ").append(item.text("title")).append("\n")
                append("https://news.ycombinator.com/item?id=").append(item.optString("objectID")).append("\n\n")
            }
        }.trim()
    }

    private fun getJson(url: String, headers: Map<String, String> = emptyMap()): JSONObject =
        JSONObject(Http.get(url, headers, timeoutSec = 30))

    private fun url(base: String, params: Map<String, String>): String =
        base + "?" + params.entries.joinToString("&") { "${Http.encode(it.key)}=${Http.encode(it.value)}" }
}
