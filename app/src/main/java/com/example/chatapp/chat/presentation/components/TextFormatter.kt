package com.example.chatapp.chat.presentation.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun ClickableFormattedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = LocalContentColor.current
) {
    val uriHandler = LocalUriHandler.current
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val patterns = mapOf(
            Regex("""(?:###|##)\s*(.*?)(?=\n|$)""") to "heading",
            Regex("""\*\*(.*?)\*\*""") to "bold",
            Regex("""\*(.*?)\*""") to "italic",
            Regex("""`(.*?)`""") to "code",
            Regex("""~~(.*?)~~""") to "strikethrough",
            Regex("""_(.*?)_""") to "underline",
            Regex("""\[(.*?)]\((.*?)\)""") to "link"
        )

        while (currentIndex < text.length) {
            val matches = patterns.map { (regex, type) ->
                regex.find(text, currentIndex)?.let {
                    Triple(it.range.first, type, it)
                }
            }

            val firstMatch = matches.filterNotNull().minByOrNull { it.first }

            if (firstMatch == null) {
                append(text.substring(currentIndex))
                break
            }

            append(text.substring(currentIndex, firstMatch.first))

            when (firstMatch.second) {
                "heading" -> {
                    append("\n")
                    withStyle(
                        SpanStyle(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    ) {
                        append(firstMatch.third.groupValues[1].trim())
                    }
                    append("\n")
                }

                "link" -> {
                    val linkText = firstMatch.third.groupValues[1]
                    val linkUrl = firstMatch.third.groupValues[2]
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = linkUrl
                    )
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(linkText)
                    }
                    pop()
                }

                "code" -> {
                    withStyle(
                        SpanStyle(
                            background = MaterialTheme.colorScheme.surfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            fontSize = style.fontSize * 0.9,
                            letterSpacing = 0.sp
                        )
                    ) {
                        append(" ${firstMatch.third.groupValues[1]} ")
                    }
                }

                "bold" -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(firstMatch.third.groupValues[1])
                    }
                }

                "italic" -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(firstMatch.third.groupValues[1])
                    }
                }

                "strikethrough" -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(firstMatch.third.groupValues[1])
                    }
                }

                "underline" -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(firstMatch.third.groupValues[1])
                    }
                }
            }
            currentIndex = firstMatch.third.range.last + 1
        }
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    BasicText(
        text = annotatedString,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                layoutResult.value?.let { layoutResult ->
                    val position = layoutResult.getOffsetForPosition(offset)
                    annotatedString
                        .getStringAnnotations(tag = "URL", start = position, end = position)
                        .firstOrNull()
                        ?.let { annotation -> uriHandler.openUri(annotation.item) }
                }
            }
        },
        style = style.copy(color = color),
        onTextLayout = { layoutResult.value = it }
    )
}