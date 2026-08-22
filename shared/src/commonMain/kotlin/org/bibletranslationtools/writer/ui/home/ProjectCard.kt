package org.bibletranslationtools.writer.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bibletranslationtools.writer.core.TextStyleType
import org.bibletranslationtools.writer.core.TranslationType
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.utils.getComposeTextStyle

@Composable
fun ProjectCard(
    item: TranslationItem,
    typography: Typography,
    onItemClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languageStyle = typography.getComposeTextStyle(
        translationType = TranslationType.TARGET,
        style = TextStyleType.SUB,
        languageCode = item.translation.targetLanguage.slug,
        direction = item.translation.targetLanguage.direction
    )

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        onClick = onItemClick
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 24.dp, bottom = 24.dp, start = 24.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.4f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                    contentDescription = "project",
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = item.formattedProjectName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = item.formattedTypeName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.1f)
            )

            Text(
                text = item.translation.targetLanguageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = languageStyle,
                modifier = Modifier.weight(0.35f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.End),
                modifier = Modifier.weight(0.15f)
            ) {
                PieProgressBar(
                    progress = item.progress,
                    modifier = Modifier.size(36.dp)
                )

                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info"
                    )
                }
            }
        }
    }
}

@Composable
private fun PieProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "PieProgressAnimation"
    )

    Canvas(
        modifier = modifier
            .size(40.dp)
            .aspectRatio(1f)
    ) {
        val sweepAngle = animatedProgress * 360f

        drawCircle(
            color = backgroundColor,
            radius = size.minDimension / 2
        )

        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset.Zero,
            size = size
        )
    }
}