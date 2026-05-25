package org.bibletranslationtools.writer.ui.publish

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.dp
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.review
import org.bibletranslationtools.writer.core.TextStyleType
import org.bibletranslationtools.writer.core.TranslationType
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.core.Validation
import org.bibletranslationtools.writer.utils.getComposeTextStyle
import org.jetbrains.compose.resources.stringResource

@Composable
fun ValidationCard(
    item: ValidationItem,
    typography: Typography,
    onReviewClick: (Validation.InvalidFrame) -> Unit
) {
    val isFrame = item.validation is Validation.ValidFrame
            || item.validation is Validation.InvalidFrame
    val horizontalPadding = if (isFrame) 48.dp else 0.dp

    val titleStyle = typography.getComposeTextStyle(
        translationType = TranslationType.TARGET,
        style = TextStyleType.SUB,
        languageCode = item.validation.titleLanguage.slug,
        direction = item.validation.titleLanguage.direction
    )

    val bodyStyle = typography.getComposeTextStyle(
        translationType = TranslationType.TARGET,
        style = TextStyleType.SUB,
        languageCode = (item.validation as? Validation.InvalidFrame)?.bodyLanguage?.slug,
        direction = (item.validation as? Validation.InvalidFrame)?.bodyLanguage?.direction
    )

    val inlineContentMap = mapOf(
        "note_icon" to InlineTextContent(
            Placeholder(
                width = bodyStyle.fontSize,
                height = bodyStyle.fontSize,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Note",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = horizontalPadding)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(3.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.validation.title,
                        style = titleStyle,
                        modifier = Modifier.weight(1f)
                    )

                    when (item.validation) {
                        is Validation.ValidFrame, is Validation.ValidGroup -> {
                            Icon(
                                imageVector = if (item.validation.isRange) {
                                    Icons.Default.DoneAll
                                } else Icons.Default.Done,
                                contentDescription = "Valid",
                                tint = MaterialTheme.colorScheme.tertiaryFixed
                            )
                        }
                        is Validation.InvalidGroup -> {
                            Icon(
                                imageVector = Icons.Default.Report,
                                contentDescription = "Invalid Group",
                                tint = MaterialTheme.colorScheme.errorContainer
                            )
                        }
                        is Validation.InvalidFrame -> {
                            Button(
                                onClick = { onReviewClick(item.validation) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(stringResource(Res.string.review))
                            }
                        }
                    }
                }

                if (item.validation is Validation.InvalidFrame) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.rendered,
                        style = bodyStyle,
                        inlineContent = inlineContentMap
                    )
                }
            }
        }
    }
}