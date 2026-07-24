package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorDialog(
    onDismiss: () -> Unit,
    onAttachToChat: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFilter by remember { mutableStateOf("Original") }
    var brightness by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var saturation by remember { mutableStateOf(1f) }
    var aiPromptText by remember { mutableStateOf("") }
    var isProcessingAi by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    loadedBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            statusMessage = "ছবি সফলভাবে সিলেক্ট করা হয়েছে"
        }
    }

    // Color Matrix calculation for live filters
    val colorMatrix = remember(selectedFilter, brightness, contrast, saturation) {
        val cm = ColorMatrix()
        cm.setToSaturation(saturation)
        
        // Apply filter presets
        when (selectedFilter) {
            "B&W" -> cm.setToSaturation(0f)
            "Sepia" -> {
                cm.setToSaturation(0f)
                val sepiaMatrix = floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.set(ColorMatrix(sepiaMatrix))
            }
            "Vintage" -> {
                val vintageMatrix = floatArrayOf(
                    0.9f, 0.1f, 0.1f, 0f, 20f,
                    0.1f, 0.8f, 0.1f, 0f, 15f,
                    0.1f, 0.1f, 0.7f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.set(ColorMatrix(vintageMatrix))
            }
            "Cyberpunk" -> {
                val cyberMatrix = floatArrayOf(
                    1.2f, 0f, 0.3f, 0f, 30f,
                    0f, 0.9f, 0.5f, 0f, -10f,
                    0.5f, 0.2f, 1.4f, 0f, 40f,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.set(ColorMatrix(cyberMatrix))
            }
        }
        cm
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8E24AA).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = Color(0xFF8E24AA)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🎨 AI Photo Editor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ফিল্টার, এআই রিটাচ এবং ইমেজ এনহ্যান্সমেন্ট",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Photo Preview Canvas / Picker Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            if (loadedBitmap != null) {
                                Image(
                                    bitmap = loadedBitmap!!.asImageBitmap(),
                                    contentDescription = "Preview Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.colorMatrix(colorMatrix)
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Selected Image",
                                        tint = Color(0xFF8E24AA),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("ছবি লোড হয়েছে", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Overlay Re-pick button
                            IconButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Change photo",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { photoPickerLauncher.launch("image/*") }
                                    .padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Pick photo",
                                    tint = Color(0xFF8E24AA),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ছবি গ্যালারি থেকে বেছে নিন",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "বা এখানে ট্যাপ করে যেকোনো ছবি আপলোড করুন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (statusMessage != null) {
                        Text(
                            text = statusMessage ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF8E24AA),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Preset Filter Chips
                    Column {
                        Text(
                            text = "ফিল্টার প্রিসেট",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val filters = listOf("Original", "B&W", "Sepia", "Vintage", "Cyberpunk")
                            filters.forEach { filterName ->
                                FilterChip(
                                    selected = selectedFilter == filterName,
                                    onClick = { selectedFilter = filterName },
                                    label = { Text(filterName) },
                                    leadingIcon = if (selectedFilter == filterName) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Sliders for Adjustments
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ছবি টিউনিং (Brightness, Contrast & Saturation)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, contentDescription = "Brightness", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saturate: ${(saturation * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = saturation,
                                onValueChange = { saturation = it },
                                valueRange = 0f..2f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // AI Prompt Retouch Section
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF8E24AA).copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF8E24AA),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Prompt Retouch",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8E24AA)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = aiPromptText,
                                onValueChange = { aiPromptText = it },
                                placeholder = { Text("উদাহরণ: 'Remove background', 'Add warm lighting'", fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (selectedImageUri == null) {
                                        statusMessage = "দয়া করে আগে একটি ছবি সিলেক্ট করুন"
                                        return@Button
                                    }
                                    isProcessingAi = true
                                    statusMessage = "AI ব্যাকগ্রাউন্ড রিটাচিং চলছে..."
                                    // Simulate AI processing
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        isProcessingAi = false
                                        selectedFilter = "Cyberpunk"
                                        saturation = 1.4f
                                        statusMessage = "✨ AI Auto-Retouch সফলভাবে সম্পন্ন হয়েছে!"
                                    }, 1500)
                                },
                                enabled = !isProcessingAi,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA))
                            ) {
                                if (isProcessingAi) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("প্রসেসিং হচ্ছে...")
                                } else {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("এআই দিয়ে রিটাচ করুন")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (selectedImageUri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_STREAM, selectedImageUri)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Edited Image"))
                            } else {
                                statusMessage = "শেয়ার করার জন্য ছবি সিলেক্ট করুন"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("শেয়ার")
                    }

                    Button(
                        onClick = {
                            if (selectedImageUri != null) {
                                onAttachToChat(selectedImageUri!!, "Edited_Photo_${System.currentTimeMillis()}.jpg")
                                onDismiss()
                            } else {
                                statusMessage = "চ্যাটে অ্যাটাচ করার জন্য ছবি বেছে নিন"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10A37F))
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("চ্যাটে যুক্ত করুন")
                    }
                }
            }
        }
    }
}
