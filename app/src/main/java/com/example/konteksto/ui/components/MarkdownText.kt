package com.example.konteksto.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renders the subset of Markdown produced by Gemini explanations: headings, bullet and
 * numbered lists, fenced code blocks, and inline bold / italic / `code`.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    val codeSpanStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = MaterialTheme.colorScheme.surfaceContainerHigh,
    )

    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            val previous = blocks.getOrNull(index - 1)
            val topSpacing = when {
                previous == null -> 0.dp
                block is MarkdownBlock.Heading -> 12.dp
                block is MarkdownBlock.ListItem && previous is MarkdownBlock.ListItem -> 4.dp
                else -> 8.dp
            }
            val blockModifier = Modifier.padding(top = topSpacing)

            when (block) {
                is MarkdownBlock.Heading -> Text(
                    text = parseInlineMarkdown(block.text, codeSpanStyle),
                    style = (if (block.level <= 2) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall)
                        .copy(fontWeight = FontWeight.Bold),
                    color = color,
                    modifier = blockModifier,
                )

                is MarkdownBlock.Paragraph -> Text(
                    text = parseInlineMarkdown(block.text, codeSpanStyle),
                    style = style,
                    color = color,
                    modifier = blockModifier,
                )

                is MarkdownBlock.ListItem -> Row(
                    modifier = blockModifier
                        .fillMaxWidth()
                        .padding(start = 16.dp * block.depth),
                ) {
                    Text(text = block.marker, style = style, color = color)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = parseInlineMarkdown(block.text, codeSpanStyle),
                        style = style,
                        color = color,
                        modifier = Modifier.weight(1f),
                    )
                }

                is MarkdownBlock.CodeBlock -> Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = blockModifier.fillMaxWidth(),
                ) {
                    Text(
                        text = block.code,
                        style = style.copy(fontFamily = FontFamily.Monospace),
                        color = color,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}

internal sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class ListItem(val marker: String, val depth: Int, val text: String) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
}

private val headingRegex = Regex("^#{1,6}\\s+(.*)$")
private val bulletRegex = Regex("^(\\s*)[*\\-+•]\\s+(.*)$")
private val numberedRegex = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
private val ruleRegex = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")

internal fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()
    val code = mutableListOf<String>()
    var inCodeBlock = false

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(paragraph.joinToString("\n"))
            paragraph.clear()
        }
    }

    for (rawLine in markdown.replace("\r\n", "\n").lines()) {
        if (rawLine.trimStart().startsWith("```")) {
            if (inCodeBlock) {
                blocks += MarkdownBlock.CodeBlock(code.joinToString("\n"))
                code.clear()
            } else {
                flushParagraph()
            }
            inCodeBlock = !inCodeBlock
            continue
        }
        if (inCodeBlock) {
            code += rawLine
            continue
        }

        val line = rawLine.trimEnd()
        val heading = headingRegex.find(line.trimStart())
        val bullet = bulletRegex.find(line)
        val numbered = numberedRegex.find(line)

        when {
            line.isBlank() || ruleRegex.matches(line) -> flushParagraph()
            heading != null -> {
                flushParagraph()
                val level = line.trimStart().takeWhile { it == '#' }.length
                blocks += MarkdownBlock.Heading(level, heading.groupValues[1].trim())
            }
            bullet != null -> {
                flushParagraph()
                blocks += MarkdownBlock.ListItem("•", bullet.groupValues[1].length / 2, bullet.groupValues[2].trim())
            }
            numbered != null -> {
                flushParagraph()
                blocks += MarkdownBlock.ListItem("${numbered.groupValues[2]}.", numbered.groupValues[1].length / 2, numbered.groupValues[3].trim())
            }
            else -> paragraph += line.trim()
        }
    }

    flushParagraph()
    if (code.isNotEmpty()) blocks += MarkdownBlock.CodeBlock(code.joinToString("\n"))
    return blocks
}

/** Parses inline `**bold**`, `*italic*` and `` `code` `` spans. Bold may contain the other two. */
internal fun parseInlineMarkdown(text: String, codeSpanStyle: SpanStyle): AnnotatedString =
    buildAnnotatedString { appendInlineMarkdown(text, codeSpanStyle) }

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String, codeSpanStyle: SpanStyle) {
    var i = 0
    while (i < text.length) {
        when {
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    withStyle(codeSpanStyle) { append(text.substring(i + 1, end)) }
                    i = end + 1
                    continue
                }
            }

            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInlineMarkdown(text.substring(i + 2, end), codeSpanStyle)
                    }
                    i = end + 2
                    continue
                }
            }

            text[i] == '*' && i + 1 < text.length && !text[i + 1].isWhitespace() -> {
                val end = findItalicEnd(text, i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendInlineMarkdown(text.substring(i + 1, end), codeSpanStyle)
                    }
                    i = end + 1
                    continue
                }
            }
        }
        append(text[i])
        i++
    }
}

private fun findItalicEnd(text: String, from: Int): Int {
    var j = text.indexOf('*', from)
    while (j != -1) {
        val isDoubleStar = j + 1 < text.length && text[j + 1] == '*'
        if (!isDoubleStar && !text[j - 1].isWhitespace()) return j
        j = text.indexOf('*', if (isDoubleStar) j + 2 else j + 1)
    }
    return -1
}
