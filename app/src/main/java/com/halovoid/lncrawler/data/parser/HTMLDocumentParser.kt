package com.halovoid.lncrawler.data.parser

import com.halovoid.lncrawler.domain.models.Block
import com.halovoid.lncrawler.domain.models.InlineSpan
import com.halovoid.lncrawler.domain.models.ListItem
import com.halovoid.lncrawler.domain.models.ReaderDocument
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Converts raw chapter HTML into a ReaderDocument tree of Blocks.
 *
 * Requires: implementation("org.jsoup:jsoup:1.17.2")
 *
 * Designed to degrade gracefully:
 * - Unknown/wrapper tags (div/section/article/span) are unwrapped so their
 *   children are still parsed, rather than being dropped.
 * - Genuinely unrecognized tags fall back to salvaging their text content.
 * - Any parse failure produces a single Unsupported block with the raw
 *   (truncated) HTML rather than throwing, so one bad chapter can't crash
 *   the reader.
 *
 * [baseUrl] is used to resolve relative image/link URLs (e.g. the novel's
 * source domain) via Jsoup's absUrl.
 */
class HtmlDocumentParser(
    private val baseUrl: String? = null
) {
    private var idCounter = 0
    private fun nextId(prefix: String) = "$prefix-${idCounter++}"

    fun parse(html: String, chapterId: Int): ReaderDocument {
        idCounter = 0
        if (html.isBlank()) return ReaderDocument.EMPTY

        return try {
            val doc = Jsoup.parseBodyFragment(html, baseUrl.orEmpty())
            val body = doc.body()
            val blocks = mutableListOf<Block>()
            body.childNodes().forEach { node ->
                parseNode(node, chapterId)?.let { blocks.addAll(it) }
            }
            ReaderDocument(
                blocks.ifEmpty {
                    val text = doc.text().trim()
                    if (text.isEmpty()) emptyList()
                    else listOf(Block.Paragraph(nextId("c$chapterId-p"), listOf(InlineSpan.Text(text))))
                }
            )
        } catch (e: Exception) {
            ReaderDocument(
                listOf(Block.Unsupported(nextId("c$chapterId-err"), html.take(4000)))
            )
        }
    }

    private fun parseNode(node: Node, chapterId: Int): List<Block>? {
        if (node !is Element) {
            val text = (node as? TextNode)?.text()?.trim()
            return if (!text.isNullOrEmpty()) {
                listOf(Block.Paragraph(nextId("c$chapterId-p"), listOf(InlineSpan.Text(text))))
            } else null
        }

        return when (node.tagName().lowercase()) {
            "p" -> listOf(Block.Paragraph(nextId("c$chapterId-p"), parseInline(node)))

            "h1", "h2", "h3", "h4", "h5", "h6" -> listOf(
                Block.Heading(
                    nextId("c$chapterId-h"),
                    node.tagName().substring(1).toIntOrNull() ?: 3,
                    parseInline(node)
                )
            )

            "div", "section", "article" -> {
                // Structural wrappers: unwrap and parse children directly rather
                // than inventing an extra visual block for them.
                if (hasBlockChildren(node)) {
                    node.childNodes().flatMap { parseNode(it, chapterId) ?: emptyList() }
                } else {
                    val text = node.text().trim()
                    if (text.isEmpty()) emptyList()
                    else listOf(Block.Paragraph(nextId("c$chapterId-p"), parseInline(node)))
                }
            }

            "blockquote" -> listOf(
                Block.Quote(
                    nextId("c$chapterId-q"),
                    node.childNodes().flatMap { parseNode(it, chapterId) ?: emptyList() }
                        .ifEmpty { listOf(Block.Paragraph(nextId("c$chapterId-p"), parseInline(node))) }
                )
            )

            "ul", "ol" -> listOf(
                Block.ListBlock(
                    nextId("c$chapterId-l"),
                    ordered = node.tagName() == "ol",
                    items = node.children().filter { it.tagName() == "li" }.map {
                        ListItem(nextId("c$chapterId-li"), parseInline(it))
                    }
                )
            )

            "img" -> {
                val src = node.absUrl("src").ifBlank { node.attr("src") }
                if (src.isBlank()) emptyList()
                else listOf(
                    Block.ImageBlock(
                        nextId("c$chapterId-img"),
                        src = src,
                        alt = node.attr("alt").ifBlank { null },
                        width = node.attr("width").toIntOrNull(),
                        height = node.attr("height").toIntOrNull()
                    )
                )
            }

            "hr" -> listOf(Block.Divider(nextId("c$chapterId-hr")))

            "br" -> null // only meaningful inline; handled in parseInlineNode

            else -> {
                // Unknown tag: try to salvage children rather than dropping content.
                val children = node.childNodes().flatMap { parseNode(it, chapterId) ?: emptyList() }
                children.ifEmpty {
                    val text = node.text().trim()
                    if (text.isEmpty()) emptyList()
                    else listOf(Block.Paragraph(nextId("c$chapterId-p"), listOf(InlineSpan.Text(text))))
                }
            }
        }
    }

    private fun hasBlockChildren(el: Element): Boolean =
        el.children().any { it.tagName().lowercase() in BLOCK_TAGS }

    private fun parseInline(el: Element): List<InlineSpan> {
        val spans = el.childNodes().flatMap { parseInlineNode(it) }
        return spans.ifEmpty { listOf(InlineSpan.Text(el.text())) }
    }

    private fun parseInlineNode(node: Node): List<InlineSpan> {
        if (node is TextNode) {
            val text = node.text()
            return if (text.isEmpty()) emptyList() else listOf(InlineSpan.Text(text))
        }
        if (node !is Element) return emptyList()

        val children = node.childNodes().flatMap { parseInlineNode(it) }
        return when (node.tagName().lowercase()) {
            "b", "strong" -> listOf(InlineSpan.Bold(children.ifEmpty { listOf(InlineSpan.Text(node.text())) }))
            "i", "em" -> listOf(InlineSpan.Italic(children.ifEmpty { listOf(InlineSpan.Text(node.text())) }))
            "u" -> listOf(InlineSpan.Underline(children.ifEmpty { listOf(InlineSpan.Text(node.text())) }))
            "s", "strike", "del" -> listOf(InlineSpan.Strikethrough(children.ifEmpty { listOf(InlineSpan.Text(node.text())) }))
            "a" -> listOf(
                InlineSpan.Link(
                    node.absUrl("href").ifBlank { node.attr("href") },
                    children.ifEmpty { listOf(InlineSpan.Text(node.text())) }
                )
            )
            "br" -> listOf(InlineSpan.LineBreak) + children
            else -> children.ifEmpty {
                val text = node.text()
                if (text.isEmpty()) emptyList() else listOf(InlineSpan.Text(text))
            }
        }
    }

    companion object {
        private val BLOCK_TAGS = setOf(
            "p", "div", "section", "article", "blockquote",
            "ul", "ol", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "img"
        )
    }
}