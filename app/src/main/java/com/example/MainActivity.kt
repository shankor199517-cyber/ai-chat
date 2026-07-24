package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ChatUiState
import com.example.ui.ChatViewModel
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                ChatAppMainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAppMainScreen(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Auto-scroll list to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "চ্যাট হিস্টোরি",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = {
                            viewModel.createNewSession()
                            coroutineScope.launch { drawerState.close() }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "New Chat")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.sessions, key = { it.id }) { session ->
                            val isSelected = session.id == uiState.currentSessionId
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = session.title,
                                        maxLines = 1,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectSession(session.id)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null
                                    )
                                },
                                badge = {
                                    IconButton(
                                        onClick = { viewModel.deleteSession(session.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete session",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "🛠️ এআই ক্রিয়েটর টুলস",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("🎨 AI Photo Editor") },
                        selected = false,
                        onClick = {
                            viewModel.openPhotoEditor()
                            coroutineScope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF8E24AA)) }
                    )

                    NavigationDrawerItem(
                        label = { Text("🎥 AI Video Maker Studio") },
                        selected = false,
                        onClick = {
                            viewModel.openVideoMaker()
                            coroutineScope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.MovieFilter, contentDescription = null, tint = Color(0xFF0284C7)) }
                    )

                    NavigationDrawerItem(
                        label = { Text("📱 AI APK Maker & Download") },
                        selected = false,
                        onClick = {
                            viewModel.openApkMaker()
                            coroutineScope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Android, contentDescription = null, tint = Color(0xFF059669)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Button(
                        onClick = {
                            viewModel.createNewSession()
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10A37F))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("নতুন চ্যাট খুলুন")
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.currentSessionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { modelMenuExpanded = true }
                                    .padding(vertical = 1.dp)
                            ) {
                                val (modelLabel, modelIcon, modelColor) = when (uiState.selectedModel) {
                                    "chatgpt-pro" -> Triple("ChatGPT Pro", Icons.Default.AutoAwesome, Color(0xFF10A37F))
                                    "gemini-2.5-pro" -> Triple("Gemini Pro", Icons.Default.Stars, Color(0xFFAB47BC))
                                    else -> Triple("Gemini Flash", Icons.Default.Bolt, Color(0xFFFFB300))
                                }
                                Icon(
                                    imageVector = modelIcon,
                                    contentDescription = null,
                                    tint = modelColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = modelLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Model",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )

                                DropdownMenu(
                                    expanded = modelMenuExpanded,
                                    onDismissRequest = { modelMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10A37F),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("ChatGPT Pro", fontWeight = FontWeight.Bold)
                                                    Text("Elite reasoning & deep analysis mode", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectModel("chatgpt-pro")
                                            modelMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Stars,
                                                    contentDescription = null,
                                                    tint = Color(0xFFAB47BC),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("Gemini 3.1 Pro", fontWeight = FontWeight.Bold)
                                                    Text("Advanced multimodal reasoning", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectModel("gemini-2.5-pro")
                                            modelMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Bolt,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFB300),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("Gemini 2.5 Flash", fontWeight = FontWeight.Bold)
                                                    Text("Fast & lightweight responses", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectModel("gemini-2.5-flash")
                                            modelMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Intel HTML Export Button
                        FilledTonalButton(
                            onClick = { viewModel.generateIntelExportHtml() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF10A37F).copy(alpha = 0.2f),
                                contentColor = Color(0xFF10A37F)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("intel_export_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Intel Export",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "intel.html",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.shareConversation(context) },
                            modifier = Modifier.testTag("share_conversation_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Conversation Thread")
                        }

                        IconButton(onClick = { viewModel.createNewSession() }) {
                            Icon(Icons.Default.AddComment, contentDescription = "New Session")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            // Explicitly pass empty WindowInsets to Scaffold content, so bottom IME insets are handled in the Chat Input container!
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Online/Offline Status Banner
                NetworkStatusBadge(
                    networkStatus = uiState.networkStatus,
                    forceOfflineMode = uiState.forceOfflineMode,
                    onToggleOffline = { viewModel.toggleOfflineMode() }
                )

                // Main Chat Message Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (uiState.messages.isEmpty()) {
                        EmptyChatPlaceholder(
                            onChipClick = { prompt ->
                                viewModel.onInputTextChange(prompt)
                                viewModel.sendMessage()
                            }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                        ) {
                            items(uiState.messages, key = { it.id }) { msg ->
                                ChatMessageItem(
                                    message = msg,
                                    onSpeakText = { text -> viewModel.speakText(text) },
                                    isSpeakingThis = uiState.isSpeakingTts,
                                    onStopSpeak = { viewModel.stopSpeaking() }
                                )
                            }
                        }
                    }
                }

                // Voice Listening Mic Bar
                AnimatedVisibility(
                    visible = uiState.isListeningSpeech,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    VoiceListeningBar(
                        onStopListening = { viewModel.toggleVoiceInput() }
                    )
                }

                // Keyboard Overlap Prevention Container (imePadding applied HERE!)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .imePadding() // CRITICAL: Makes input box float above soft keyboard!
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (uiState.speechError != null) {
                            Text(
                                text = uiState.speechError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(bottom = 4.dp, start = 8.dp)
                            )
                        }

                        // Selected Attachment Preview Chip
                        if (uiState.selectedAttachment != null) {
                            SelectedAttachmentChipBar(
                                attachment = uiState.selectedAttachment!!,
                                onRemoveAttachment = { viewModel.removeAttachment() }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Attachment Picker Button (Paperclip)
                            IconButton(
                                onClick = { viewModel.openAttachmentSheet() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("attach_file_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach File",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Voice Input Mic Button
                            IconButton(
                                onClick = { viewModel.toggleVoiceInput() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.isListeningSpeech) Color(0xFFEF4444)
                                        else Color(0xFF10A37F).copy(alpha = 0.15f)
                                    )
                                    .testTag("mic_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isListeningSpeech) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = if (uiState.isListeningSpeech) Color.White else Color(0xFF10A37F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Chat Input Text Field
                            OutlinedTextField(
                                value = uiState.inputText,
                                onValueChange = { viewModel.onInputTextChange(it) },
                                placeholder = {
                                    Text(
                                        text = if (uiState.isListeningSpeech) "কথা বলুন..." else "বার্তা বা প্রশ্ন লিখুন...",
                                        fontSize = 14.sp
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF10A37F),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                maxLines = 4,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field")
                            )

                            // Send Button / Loading Indicator
                            if (uiState.isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = Color(0xFF10A37F),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                FloatingActionButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.sendMessage()
                                    },
                                    containerColor = Color(0xFF10A37F),
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Intel HTML Code Export Dialog
    if (uiState.showExportDialog) {
        IntelHtmlExportDialog(
            htmlCode = uiState.exportedHtmlCode,
            onDismiss = { viewModel.closeExportDialog() }
        )
    }

    // Attachment Options Sheet
    if (uiState.showAttachmentSheet) {
        AttachmentOptionsModalSheet(
            onDismiss = { viewModel.closeAttachmentSheet() },
            onFileSelected = { file -> viewModel.setSelectedAttachment(file) },
            onOpenPhotoEditor = { viewModel.openPhotoEditor() },
            onOpenVideoMaker = { viewModel.openVideoMaker() },
            onOpenApkMaker = { viewModel.openApkMaker() }
        )
    }

    // AI Photo Editor Dialog
    if (uiState.showPhotoEditor) {
        PhotoEditorDialog(
            onDismiss = { viewModel.closePhotoEditor() },
            onAttachToChat = { uri, fileName ->
                viewModel.setSelectedAttachment(
                    com.example.ui.AttachedFile(
                        type = com.example.ui.AttachmentType.PHOTO,
                        uri = uri,
                        name = fileName,
                        sizeString = "Edited Photo"
                    )
                )
            }
        )
    }

    // AI Video Maker Studio Dialog
    if (uiState.showVideoMaker) {
        AiVideoMakerDialog(
            onDismiss = { viewModel.closeVideoMaker() },
            onAttachVideoToChat = { uri, fileName ->
                viewModel.setSelectedAttachment(
                    com.example.ui.AttachedFile(
                        type = com.example.ui.AttachmentType.VIDEO,
                        uri = uri,
                        name = fileName,
                        sizeString = "AI Video"
                    )
                )
            }
        )
    }

    // AI APK Maker & Downloader Studio Dialog
    if (uiState.showApkMaker) {
        AiApkMakerDialog(
            onDismiss = { viewModel.closeApkMaker() },
            onAttachApkToChat = { uri, fileName ->
                viewModel.setSelectedAttachment(
                    com.example.ui.AttachedFile(
                        type = com.example.ui.AttachmentType.DOCUMENT,
                        uri = uri,
                        name = fileName,
                        sizeString = "AI Generated APK"
                    )
                )
            }
        )
    }
}

@Composable
fun EmptyChatPlaceholder(
    onChipClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF10A37F).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF10A37F),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ChatGPT AI তে স্বাগতম",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "ভয়েস ইনপুট দিয়ে কথা বলুন বা যেকোনো বিষয়ে প্রশ্ন করুন। অফলাইন মোডেও ডাটাবেসে চ্যাট সেভ থাকবে!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        QuickSuggestionChips(onChipClick = onChipClick)
    }
}
