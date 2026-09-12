package com.halovoid.bunori.domain.models

data class ReaderDocument(
    val blocks: List<Block>
) {
    companion object {
        val EMPTY = ReaderDocument(emptyList())
    }
}

sealed class Block {
    abstract val id: String

    data class Paragraph(
        override val id: String,
        val spans: List<InlineSpan>
    ): Block()

    data class Heading(
        override val id: String,
        val level: Int,
        val spans: List<InlineSpan>
    ): Block()

    data class Quote(
        override val id: String,
        val blocks: List<Block>
    ): Block()

    data class ListBlock(
        override val id: String,
        val ordered: Boolean,
        val items: List<ListItem>
    ): Block()

    data class ImageBlock(
        override val id: String,
        val src: String,
        val alt: String?,
        val width: Int? = null,
        val height: Int? = null
    ): Block()

    data class Divider(
        override val id: String
    ): Block()

    data class ErrorPlaceholder(
        override val id: String,
        val message: String
    ): Block()

    data class Unsupported(
        override val id: String,
        val rawText: String
    ): Block()
}

data class ListItem(
    val id: String,
    val spans: List<InlineSpan>
)

sealed class InlineSpan {
    data class Text(val text: String): InlineSpan()
    data class Bold(val children: List<InlineSpan>): InlineSpan()
    data class Italic(val children: List<InlineSpan>): InlineSpan()
    data class Underline(val children: List<InlineSpan>): InlineSpan()
    data class Strikethrough(val children: List<InlineSpan>): InlineSpan()
    data class Link(val href: String, val children: List<InlineSpan>): InlineSpan()
    object LineBreak: InlineSpan()
}
