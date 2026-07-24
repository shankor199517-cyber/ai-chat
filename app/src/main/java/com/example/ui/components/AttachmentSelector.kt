package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AttachedFile
import com.example.ui.AttachmentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentOptionsModalSheet(
    onDismiss: () -> Unit,
    onFileSelected: (AttachedFile) -> Unit,
    onOpenPhotoEditor: () -> Unit,
    onOpenVideoMaker: () -> Unit,
    onOpenApkMaker: () -> Unit
) {
    // Pick Image Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Photo_${System.currentTimeMillis()}.jpg"
            onFileSelected(AttachedFile(AttachmentType.PHOTO, uri, fileName, "Image"))
            onDismiss()
        }
    }

    // Pick Video Launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Video_${System.currentTimeMillis()}.mp4"
            onFileSelected(AttachedFile(AttachmentType.VIDEO, uri, fileName, "Video"))
            onDismiss()
        }
    }

    // Pick Document / Any File Launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Document_${System.currentTimeMillis()}.pdf"
            onFileSelected(AttachedFile(AttachmentType.DOCUMENT, uri, fileName, "Document"))
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📎 ফাইল ও মিডিয়া যুক্ত করুন (Attach File)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttachmentOptionItem(
                    icon = Icons.Default.Image,
                    label = "ফটো / ছবি",
                    color = Color(0xFF10A37F),
                    onClick = { imagePickerLauncher.launch("image/*") }
                )

                AttachmentOptionItem(
                    icon = Icons.Default.Videocam,
                    label = "ভিডিও",
                    color = Color(0xFF0284C7),
                    onClick = { videoPickerLauncher.launch("video/*") }
                )

                AttachmentOptionItem(
                    icon = Icons.Default.InsertDriveFile,
                    label = "ডকুমেন্ট / ফাইল",
                    color = Color(0xFFD97706),
                    onClick = { documentPickerLauncher.launch("*/*") }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "✨ ক্রিয়েটিভ এআই ক্রিয়েটর স্টুডিও (AI Studio Tools)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttachmentOptionItem(
                    icon = Icons.Default.AutoFixHigh,
                    label = "Photo Editor",
                    color = Color(0xFF8E24AA),
                    onClick = {
                        onDismiss()
                        onOpenPhotoEditor()
                    }
                )

                AttachmentOptionItem(
                    icon = Icons.Default.MovieFilter,
                    label = "AI Video Maker",
                    color = Color(0xFF0284C7),
                    onClick = {
                        onDismiss()
                        onOpenVideoMaker()
                    }
                )

                AttachmentOptionItem(
                    icon = Icons.Default.Android,
                    label = "AI APK Maker",
                    color = Color(0xFF059669),
                    onClick = {
                        onDismiss()
                        onOpenApkMaker()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AttachmentOptionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SelectedAttachmentChipBar(
    attachment: AttachedFile,
    onRemoveAttachment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val icon = when (attachment.type) {
                    AttachmentType.PHOTO -> Icons.Default.Image
                    AttachmentType.VIDEO -> Icons.Default.Videocam
                    AttachmentType.DOCUMENT -> Icons.Default.InsertDriveFile
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = attachment.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "সংযুক্ত ফাইল (${attachment.type.name})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            IconButton(
                onClick = onRemoveAttachment,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove attachment",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
