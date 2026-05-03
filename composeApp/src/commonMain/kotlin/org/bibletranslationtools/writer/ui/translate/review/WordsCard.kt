package org.bibletranslationtools.writer.ui.translate.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.description
import btt_writer.composeapp.generated.resources.dismiss
import btt_writer.composeapp.generated.resources.index
import org.bibletranslationtools.resourcecontainer.Language
import org.bibletranslationtools.writer.core.TextStyleType
import org.bibletranslationtools.writer.core.TranslationType
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.utils.getComposeTextStyle
import org.jetbrains.compose.resources.stringResource

@Composable
fun WordsCard(
    title: String,
    body: AnnotatedString,
    sourceLanguage: Language?,
    typography: Typography,
    onCloseClick: () -> Unit,
    onIndexClick: () -> Unit
) {
    val titleStyle = typography.getComposeTextStyle(
        translationType = TranslationType.SOURCE,
        style = TextStyleType.TITLE,
        languageCode = sourceLanguage?.slug,
        direction = sourceLanguage?.direction
    )

    val bodyStyle = typography.getComposeTextStyle(
        translationType = TranslationType.SOURCE,
        style = TextStyleType.SUB,
        languageCode = sourceLanguage?.slug,
        direction = sourceLanguage?.direction
    )

    val scrollState = rememberScrollState()

    LaunchedEffect(body) {
        scrollState.scrollTo(0)
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            bottomStart = 16.dp,
            topEnd = 0.dp,
            bottomEnd = 0.dp
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TextButton(onClick = onCloseClick) {
                Text(stringResource(Res.string.dismiss))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(Res.string.description),
                    style = titleStyle,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = body,
                    style = bodyStyle,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                TextButton(
                    onClick = onIndexClick
                ) {
                    Text(
                        text = stringResource(Res.string.index)
                    )
                }
            }
        }
    }
}