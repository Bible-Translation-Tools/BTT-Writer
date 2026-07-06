package org.bibletranslationtools.writer.ui.publish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.next
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.core.Validation
import org.jetbrains.compose.resources.stringResource

@Composable
fun ValidationSection(
    items: List<ValidationItem>,
    typography: Typography,
    onNext: () -> Unit,
    onReview: (Validation.InvalidFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                ValidationCard(
                    item = item,
                    typography = typography,
                    onReviewClick = onReview
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onNext,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.next),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}