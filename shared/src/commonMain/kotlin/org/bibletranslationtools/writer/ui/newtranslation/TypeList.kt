package org.bibletranslationtools.writer.ui.newtranslation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.choose_type
import btt_writer.shared.generated.resources.obs_type
import btt_writer.shared.generated.resources.reg_type
import btt_writer.shared.generated.resources.text_type
import btt_writer.shared.generated.resources.tn_type
import btt_writer.shared.generated.resources.tq_type
import btt_writer.shared.generated.resources.tw_type
import btt_writer.shared.generated.resources.udb_type
import btt_writer.shared.generated.resources.ulb_type
import org.bibletranslationtools.writer.core.ResourceType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TypeList(
    options: List<TranslationTypeOption>,
    onTypeSelected: (TranslationTypeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.choose_type),
            fontStyle = FontStyle.Italic,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(options) { option ->
                TextButton(
                    onClick = { onTypeSelected(option) },
                    enabled = option.enabled,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = stringResource(typeLabel(option.resourceType)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        resourceLabel(option.resourceSlug)?.let { label ->
                            Text(
                                text = stringResource(label),
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun typeLabel(type: ResourceType): StringResource {
    return when (type) {
        ResourceType.TEXT -> Res.string.text_type
        ResourceType.TRANSLATION_NOTE -> Res.string.tn_type
        ResourceType.TRANSLATION_QUESTION -> Res.string.tq_type
        ResourceType.TRANSLATION_WORD -> Res.string.tw_type
    }
}

private fun resourceLabel(resourceSlug: String): StringResource? {
    return when (resourceSlug) {
        "reg" -> Res.string.reg_type
        "ulb" -> Res.string.ulb_type
        "udb" -> Res.string.udb_type
        "obs" -> Res.string.obs_type
        else -> null
    }
}
