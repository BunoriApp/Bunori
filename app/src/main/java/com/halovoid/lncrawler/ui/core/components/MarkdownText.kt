package com.halovoid.lncrawler.ui.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.ui.core.theme.PrimaryText
import com.halovoid.lncrawler.ui.core.theme.SecondaryText

@Composable
fun MarkdownContent(markdown: String) {
    val lines = markdown.lines()
    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("###") -> {
                    Text(
                        text = trimmed.removePrefix("###").trim(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                trimmed.startsWith("##") -> {
                    if (!trimmed.contains("v", ignoreCase = true)) {
                        Text(
                            text = trimmed.removePrefix("##").trim(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
                        )
                    }
                }
                trimmed.startsWith("*") || trimmed.startsWith("-") -> {
                    Row(modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
                        Text("•", color = SecondaryText, modifier = Modifier.padding(end = 12.dp))
                        Text(
                            text = parseBasicMarkdown(trimmed.substring(1).trim()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText,
                            lineHeight = 22.sp
                        )
                    }
                }
                trimmed.isNotBlank() -> {
                    Text(
                        text = parseBasicMarkdown(trimmed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Basic markdown parser for bolding (**text**).
 */
fun parseBasicMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val regex = "\\*\\*(.*?)\\*\\*".toRegex()
        val matches = regex.findAll(text)
        
        matches.forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last
            
            append(text.substring(currentIndex, start))
            
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = PrimaryText)) {
                append(matchResult.groupValues[1])
            }
            
            currentIndex = end + 1
        }
        
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
