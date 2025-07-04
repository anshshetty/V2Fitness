package v2.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import v2.presentation.viewmodels.QRStatusFilter

@Composable
fun QRFilterDialog(
    selectedFilter: QRStatusFilter,
    onFilterSelected: (QRStatusFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Filter QR Codes",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
            ) {
                val filterOptions = listOf(
                    QRStatusFilter.ALL to Pair("All QR Codes", Icons.Default.QrCode),
                    QRStatusFilter.ACTIVE to Pair("Active", Icons.Default.CheckCircle),
                    QRStatusFilter.EXPIRED to Pair("Expired", Icons.Default.Schedule),
                    QRStatusFilter.DISABLED to Pair("Disabled", Icons.Default.Block),
                    QRStatusFilter.USED to Pair("Used Today", Icons.Default.Done),
                )

                filterOptions.forEach { (filter, labelIcon) ->
                    val (label, icon) = labelIcon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedFilter == filter,
                                onClick = { onFilterSelected(filter) },
                                role = Role.RadioButton,
                            ).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedFilter == filter,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selectedFilter == filter) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedFilter == filter) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        modifier = modifier,
    )
} 
