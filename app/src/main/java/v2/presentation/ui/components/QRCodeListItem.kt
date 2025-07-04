package v2.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import v2.data.models.QRCodeModel
import v2.data.models.QRStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QRCodeListItem(
    qrCode: QRCodeModel,
    onExtendClick: (String) -> Unit,
    onDisableClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onWhatsAppShareClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header with QR icon and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = qrCode.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = qrCode.mobileNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                StatusBadge(status = qrCode.getCurrentStatus())
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expiry information
            val expiryTime = Date(qrCode.createdAt.toDate().time + (qrCode.expiryDuration * 24 * 60 * 60 * 1000L))
            val timeFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            Text(
                text = "Expires: ${timeFormat.format(expiryTime)} (${qrCode.expiryDuration} days)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp), // Fixed height for consistent button sizing
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (qrCode.isActive()) {
                        OutlinedButton(
                            onClick = { onExtendClick(qrCode.qrId) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Extend",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (qrCode.status != QRStatus.DISABLED) {
                        OutlinedButton(
                            onClick = { onDisableClick(qrCode.qrId) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Disable",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onShareClick(qrCode.qrId) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Share",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // WhatsApp Share Button
                Button(
                    onClick = { onWhatsAppShareClick(qrCode.qrId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp), // Fixed height for consistency
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics
                            .Color(0xFF25D366),
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = androidx.compose.ui.graphics.Color.White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Share on WhatsApp",
                        color = androidx.compose.ui.graphics.Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: QRStatus,
    modifier: Modifier = Modifier,
) {
    val (text, color) = when (status) {
        QRStatus.ACTIVE -> "Active" to Color(0xFF4CAF50)
        QRStatus.USED -> "Used" to Color(0xFFFF9800)
        QRStatus.EXPIRED -> "Expired" to Color(0xFFF44336)
        QRStatus.DISABLED -> "Disabled" to Color(0xFF9E9E9E)
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
} 
