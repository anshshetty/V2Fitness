package v2.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import v2.data.models.QRCodeModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExtendQRDialog(
    qrCode: QRCodeModel,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDays by remember { mutableIntStateOf(7) }

    val currentExpiryDate = Date(qrCode.createdAt.toDate().time + (qrCode.expiryDuration * 24 * 60 * 60 * 1000L))
    val newExpiryDate = Date(currentExpiryDate.time + (selectedDays * 24 * 60 * 60 * 1000L))
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "Extend QR Code",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "QR Code for: ${qrCode.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = "Current expiry: ${dateFormat.format(currentExpiryDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = "Select additional days:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )

                val dayOptions = listOf(30, 60, 120, 180, 365)

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    dayOptions.forEach { days ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedDays == days,
                                    onClick = { selectedDays = days },
                                ).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedDays == days,
                                onClick = { selectedDays = days },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (days) {
                                    1 -> "1 day"
                                    else -> "$days days"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "New expiry date:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = dateFormat.format(newExpiryDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDays) },
            ) {
                Text("Extend")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
        modifier = modifier,
    )
} 
