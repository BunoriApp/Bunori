package com.halovoid.bunori.extension.api

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickJsTest {

    @Test
    fun testQuickJsBasicEvaluation() = runBlocking {
        val qjs = QuickJs.create(jobDispatcher = Dispatchers.IO)
        try {
            val result = qjs.evaluate<Int>("1 + 1")
            assertEquals(2, result)

            val str = qjs.evaluate<String>("'Hello ' + 'World'")
            assertEquals("Hello World", str)
        } finally {
            qjs.close()
        }
    }

    @Test
    fun testQuickJsBindings() = runBlocking {
        val qjs = QuickJs.create(jobDispatcher = Dispatchers.IO)
        try {
            val logs = mutableListOf<String>()
            qjs.define("console") {
                function("log", FunctionBinding { args ->
                    logs.add(args.joinToString(" "))
                })
            }

            qjs.define("http") {
                asyncFunction("fetch", AsyncFunctionBinding { args ->
                    val url = args[0] as String
                    """{"url":"$url","status":200}"""
                })
            }

            val script = """
                console.log("Starting fetch...");
                async function run() {
                    const res = await http.fetch("https://example.com");
                    console.log("Got response:", res);
                    return String(JSON.parse(res).status);
                }
                await run();
            """.trimIndent()

            val status = qjs.evaluate<String>(script)
            assertEquals("200", status)
            assertEquals(2, logs.size)
            assertEquals("Starting fetch...", logs[0])
            assertEquals("""Got response: {"url":"https://example.com","status":200}""", logs[1])
        } finally {
            qjs.close()
        }
    }

    @Test
    fun testJsoupBridge() = runBlocking {
        val qjs = QuickJs.create(jobDispatcher = Dispatchers.IO)
        try {
            val elements = mutableMapOf<Int, org.jsoup.nodes.Element>()
            var nextId = 1

            qjs.define("_dom") {
                function("parse", FunctionBinding { args ->
                    val html = args[0] as String
                    val baseUri = if (args.size > 1 && args[1] is String) args[1] as String else ""
                    val doc = org.jsoup.Jsoup.parse(html, baseUri)
                    val id = nextId++
                    elements[id] = doc
                    id
                })

                function("select", FunctionBinding { args ->
                    val id = (args[0] as Number).toInt()
                    val selector = args[1] as String
                    val el = elements[id] ?: return@FunctionBinding emptyList<Int>()
                    val matches = el.select(selector)
                    matches.map { child ->
                        val childId = nextId++
                        elements[childId] = child
                        childId
                    }
                })

                function("selectFirst", FunctionBinding { args ->
                    val id = (args[0] as Number).toInt()
                    val selector = args[1] as String
                    val el = elements[id] ?: return@FunctionBinding null
                    val child = el.selectFirst(selector) ?: return@FunctionBinding null
                    val childId = nextId++
                    elements[childId] = child
                    childId
                })

                function("text", FunctionBinding { args ->
                    val id = (args[0] as Number).toInt()
                    elements[id]?.text() ?: ""
                })

                function("attr", FunctionBinding { args ->
                    val id = (args[0] as Number).toInt()
                    val name = args[1] as String
                    elements[id]?.attr(name) ?: ""
                })
            }

            val helperJs = """
                class ElementHandle {
                    constructor(id) { this._id = id; }
                    select(selector) {
                        return _dom.select(this._id, selector).map(id => new ElementHandle(id));
                    }
                    selectFirst(selector) {
                        const id = _dom.selectFirst(this._id, selector);
                        return id != null ? new ElementHandle(id) : null;
                    }
                    text() { return _dom.text(this._id); }
                    attr(name) { return _dom.attr(this._id, name); }
                }
                function parseHtml(html, baseUri = "") {
                    return new ElementHandle(_dom.parse(html, baseUri));
                }
            """.trimIndent()
            qjs.evaluate<Unit>(helperJs)

            val testScript = """
                const html = `
                    <div class="novel">
                        <h1 class="title">Shadow Slave</h1>
                        <div class="chapters">
                            <a href="/ch/1">Chapter 1</a>
                            <a href="/ch/2">Chapter 2</a>
                        </div>
                    </div>
                `;
                const doc = parseHtml(html, "https://novelbins.com");
                const title = doc.selectFirst("h1.title")?.text();
                const chapters = doc.select(".chapters a").map(a => ({
                    title: a.text(),
                    url: a.attr("abs:href")
                }));
                JSON.stringify({ title, chapters });
            """.trimIndent()

            val resultJson = qjs.evaluate<String>(testScript)
            val json = kotlinx.serialization.json.Json.parseToJsonElement(resultJson) as kotlinx.serialization.json.JsonObject
            assertEquals("Shadow Slave", json["title"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content })
            val chs = json["chapters"] as kotlinx.serialization.json.JsonArray
            assertEquals(2, chs.size)
            val ch0 = chs[0] as kotlinx.serialization.json.JsonObject
            assertEquals("Chapter 1", (ch0["title"] as kotlinx.serialization.json.JsonPrimitive).content)
            assertEquals("https://novelbins.com/ch/1", (ch0["url"] as kotlinx.serialization.json.JsonPrimitive).content)
            val ch1 = chs[1] as kotlinx.serialization.json.JsonObject
            assertEquals("https://novelbins.com/ch/2", (ch1["url"] as kotlinx.serialization.json.JsonPrimitive).content)
        } finally {
            qjs.close()
        }
    }
}
