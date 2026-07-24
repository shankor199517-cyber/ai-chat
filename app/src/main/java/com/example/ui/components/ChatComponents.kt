package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.utils.NetworkStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NetworkStatusBadge(
    networkStatus: NetworkStatus,
    forceOfflineMode: Boolean,
    onToggleOffline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline = networkStatus == NetworkStatus.ONLINE && !forceOfflineMode

    Surface(
        color = if (isOnline) Color(0xFF0F5132) else Color(0xFF842029),
        contentColor = Color.White,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOnline) "অনলাইন মোড (Gemini 3.5 Flash Active)" else if (forceOfflineMode) "অফলাইন মোড ম্যানুয়ালি অন" else "অফলাইন মোড (ইন্টারনেট বিচ্ছিন্ন)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onToggleOffline,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = if (forceOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudDone,
                    contentDescription = "Toggle Mode",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (forceOfflineMode) "অনলাইন করুন" else "অফলাইন করুন",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    onSpeakText: (String) -> Unit,
    isSpeakingThis: Boolean,
    onStopSpeak: () -> Unit
) {
    val isUser = message.sender == "user"
    val clipboardManager = LocalClipboardManager.current
    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10A37F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isUser) "আপনি" else if (message.isOfflineAnswer) "Offline Assistant" else "ChatGPT AI",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isUser && message.isOfflineAnswer) {
                    Surface(
                        color = Color(0xFFD97706),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "OFFLINE",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = 2.dp,
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SelectionContainer {
                        FormattedChatMessageContent(
                            text = message.content,
                            isUser = isUser
                        )
                    }

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.content))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy text",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isSpeakingThis) onStopSpeak() else onSpeakText(message.content)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeakingThis) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Read aloud",
                                    tint = if (isSpeakingThis) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Avatar",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FormattedChatMessageContent(
    text: String,
    isUser: Boolean
) {
    // Basic Markdown code block splitting
    val parts = remember(text) { text.split("```") }

    if (parts.size <= 1) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp)
        )
    } else {
        Column {
            parts.forEachIndexed { index, part ->
                if (index % 2 == 1) {
                    // Code block
                    val lines = part.trim().lines()
                    val language = if (lines.isNotEmpty() && lines.first().matches(Regex("^[a-zA-Z0-9_-]+$"))) lines.first() else ""
                    val codeContent = if (language.isNotEmpty()) lines.drop(1).joinToString("\n") else part.trim()

                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (language.isNotEmpty()) {
                                Text(
                                    text = language.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10A37F),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = codeContent,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                color = Color(0xFFD4D4D4)
                            )
                        }
                    }
                } else if (part.isNotBlank()) {
                    Text(
                        text = part.trim(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceListeningBar(
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulseScale"
    )

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Recording",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "কথা শুনছি... বাংলায় কথা বলুন",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "আপনার কণ্ঠস্বর টেক্সটে রূপান্তর হচ্ছে...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            IconButton(onClick = onStopListening) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop speech recognition",
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
fun QuickSuggestionChips(
    onChipClick: (String) -> Unit
) {
    val suggestions = listOf(
        "ChatGPT AI কি কি করতে পারে?",
        "একটি প্রফেশনাল ইমেইল ড্রাফট করো",
        "কোডিং শিখতে চাই",
        "Intel HTML এক্সপোর্ট ফিচার কি?"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.take(2).forEach { text ->
            AssistChip(
                onClick = { onChipClick(text) },
                label = { Text(text, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }
    }
}
