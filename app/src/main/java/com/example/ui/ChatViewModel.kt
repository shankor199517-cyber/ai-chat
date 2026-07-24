package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.repository.ChatRepository
import com.example.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AttachmentType { PHOTO, VIDEO, DOCUMENT }

data class AttachedFile(
    val type: AttachmentType,
    val uri: Uri,
    val name: String,
    val sizeString: String
)

data class ChatUiState(
    val sessions: List<ChatSessionEntity> = emptyList(),
    val currentSessionId: String? = null,
    val currentSessionTitle: String = "নতুন চ্যাট",
    val messages: List<ChatMessageEntity> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val forceOfflineMode: Boolean = false,
    val selectedModel: String = "chatgpt-pro", // ChatGPT Pro by default
    val networkStatus: NetworkStatus = NetworkStatus.OFFLINE,
    val isListeningSpeech: Boolean = false,
    val speechError: String? = null,
    val isSpeakingTts: Boolean = false,
    val showExportDialog: Boolean = false,
    val exportedHtmlCode: String = "",
    val selectedAttachment: AttachedFile? = null,
    val showAttachmentSheet: Boolean = false,
    val showPhotoEditor: Boolean = false,
    val showVideoMaker: Boolean = false,
    val showApkMaker: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChatDatabase.getInstance(application)
    private val networkObserver = NetworkObserver(application)
    private val repository = ChatRepository(db.chatDao(), networkObserver)
    private val speechManager = SpeechRecognizerManager(application)
    private val ttsManager = TextToSpeechManager(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Observe Network Connectivity
        viewModelScope.launch {
            networkObserver.status.collect { netStatus ->
                _uiState.update { it.copy(networkStatus = netStatus) }
            }
        }

        // Observe Sessions
        viewModelScope.launch {
            repository.allSessions.collect { sessionList ->
                _uiState.update { currentState ->
                    val activeId = currentState.currentSessionId ?: sessionList.firstOrNull()?.id
                    currentState.copy(
                        sessions = sessionList,
                        currentSessionId = activeId
                    )
                }

                if (_uiState.value.currentSessionId == null && sessionList.isNotEmpty()) {
                    selectSession(sessionList.first().id)
                } else if (sessionList.isEmpty()) {
                    createNewSession()
                }
            }
        }

        // Observe Messages reactively whenever currentSessionId changes
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
            _uiState
                .map { it.currentSessionId }
                .filterNotNull()
                .distinctUntilChanged()
                .flatMapLatest { sessionId ->
                    val session = db.chatDao().getSessionById(sessionId)
                    if (session != null) {
                        _uiState.update { it.copy(currentSessionTitle = session.title) }
                    }
                    repository.getMessagesForSession(sessionId)
                }
                .collect { msgList ->
                    _uiState.update { it.copy(messages = msgList) }
                }
        }

        // Observe Speech Recognizer
        viewModelScope.launch {
            speechManager.speechState.collect { speechState ->
                when (speechState) {
                    is SpeechState.Listening -> {
                        _uiState.update { it.copy(isListeningSpeech = true, speechError = null) }
                    }
                    is SpeechState.Success -> {
                        _uiState.update {
                            it.copy(
                                isListeningSpeech = false,
                                inputText = speechState.text,
                                speechError = null
                            )
                        }
                    }
                    is SpeechState.Error -> {
                        _uiState.update {
                            it.copy(
                                isListeningSpeech = false,
                                speechError = speechState.message
                            )
                        }
                    }
                    SpeechState.Idle -> {
                        _uiState.update { it.copy(isListeningSpeech = false) }
                    }
                }
            }
        }

        // Observe TTS Speaking
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                _uiState.update { it.copy(isSpeakingTts = speaking) }
            }
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun selectSession(sessionId: String) {
        _uiState.update { it.copy(currentSessionId = sessionId) }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val newSession = repository.createNewSession()
            selectSession(newSession.id)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                val remaining = _uiState.value.sessions.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first().id)
                } else {
                    createNewSession()
                }
            }
        }
    }

    fun sendMessage() {
        val rawText = _uiState.value.inputText.trim()
        val attachment = _uiState.value.selectedAttachment
        if (rawText.isEmpty() && attachment == null) return
        if (_uiState.value.isSending) return

        val messageText = if (attachment != null) {
            val fileLabel = when (attachment.type) {
                AttachmentType.PHOTO -> "📷 Photo Attachment"
                AttachmentType.VIDEO -> "🎥 Video Attachment"
                AttachmentType.DOCUMENT -> "📄 Document Attachment"
            }
            if (rawText.isNotEmpty()) {
                "$rawText\n\n[$fileLabel: ${attachment.name}]"
            } else {
                "Analysing $fileLabel: ${attachment.name}"
            }
        } else {
            rawText
        }

        _uiState.update { it.copy(inputText = "", selectedAttachment = null, isSending = true) }

        viewModelScope.launch {
            try {
                var activeSessionId = _uiState.value.currentSessionId
                if (activeSessionId == null) {
                    val newSession = repository.createNewSession()
                    activeSessionId = newSession.id
                    _uiState.update { it.copy(currentSessionId = activeSessionId) }
                }

                repository.sendMessage(
                    sessionId = activeSessionId,
                    userMessageText = messageText,
                    forceOfflineMode = _uiState.value.forceOfflineMode,
                    selectedModel = _uiState.value.selectedModel
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    fun setSelectedAttachment(file: AttachedFile?) {
        _uiState.update { it.copy(selectedAttachment = file) }
    }

    fun removeAttachment() {
        _uiState.update { it.copy(selectedAttachment = null) }
    }

    fun openAttachmentSheet() {
        _uiState.update { it.copy(showAttachmentSheet = true) }
    }

    fun closeAttachmentSheet() {
        _uiState.update { it.copy(showAttachmentSheet = false) }
    }

    fun openPhotoEditor() {
        _uiState.update { it.copy(showPhotoEditor = true) }
    }

    fun closePhotoEditor() {
        _uiState.update { it.copy(showPhotoEditor = false) }
    }

    fun openVideoMaker() {
        _uiState.update { it.copy(showVideoMaker = true) }
    }

    fun closeVideoMaker() {
        _uiState.update { it.copy(showVideoMaker = false) }
    }

    fun openApkMaker() {
        _uiState.update { it.copy(showApkMaker = true) }
    }

    fun closeApkMaker() {
        _uiState.update { it.copy(showApkMaker = false) }
    }

    fun selectModel(model: String) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun toggleOfflineMode() {
        _uiState.update { it.copy(forceOfflineMode = !it.forceOfflineMode) }
    }

    fun toggleVoiceInput() {
        if (_uiState.value.isListeningSpeech) {
            speechManager.stopListening()
        } else {
            speechManager.startListening(languageCode = "bn-BD")
        }
    }

    fun speakText(text: String) {
        ttsManager.speak(text)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun generateIntelExportHtml() {
        val sessionId = _uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            val session = db.chatDao().getSessionById(sessionId) ?: return@launch
            val messages = db.chatDao().getMessagesListForSession(sessionId)
            val html = HtmlExporter.generateIntelHtml(session, messages)

            _uiState.update {
                it.copy(
                    exportedHtmlCode = html,
                    showExportDialog = true
                )
            }
        }
    }

    fun closeExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }

    fun shareConversation(context: android.content.Context) {
        val sessionId = _uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            val session = db.chatDao().getSessionById(sessionId) ?: return@launch
            val messages = db.chatDao().getMessagesListForSession(sessionId)
            if (messages.isEmpty()) return@launch

            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            val sb = StringBuilder()
            sb.append("💬 ChatGPT AI - Conversation Export\n")
            sb.append("Title: ").append(session.title).append("\n")
            sb.append("Date: ").append(dateFormat.format(java.util.Date())).append("\n")
            sb.append("----------------------------------------\n\n")

            for (msg in messages) {
                val senderLabel = if (msg.sender == "user") "👤 You" else "🤖 ChatGPT AI"
                val timeStr = dateFormat.format(java.util.Date(msg.timestamp))
                sb.append("[$timeStr] $senderLabel:\n")
                sb.append(msg.content).append("\n\n")
            }

            sb.append("----------------------------------------\n")
            sb.append("Exported from ChatGPT AI Android App\n")

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "ChatGPT AI - ${session.title}")
                putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Conversation"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        speechManager.stopListening()
    }
}
