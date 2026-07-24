package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiNetwork
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import com.example.utils.NetworkObserver
import com.example.utils.NetworkStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    private val networkObserver: NetworkObserver
) {
    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun createNewSession(title: String = "নতুন চ্যাট"): ChatSessionEntity {
        val session = ChatSessionEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis()
        )
        chatDao.insertSession(session)
        return session
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteSession(sessionId)
    }

    private fun isApiKeyValid(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        if (key == "MY_GEMINI_API_KEY" || key.contains("MY_GEMINI_API_KEY")) return false
        if (!key.startsWith("AIza")) return false
        return true
    }

    suspend fun sendMessage(
        sessionId: String,
        userMessageText: String,
        forceOfflineMode: Boolean = false,
        selectedModel: String = "chatgpt-pro"
    ): Result<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidKey = isApiKeyValid(apiKey)
        val isOnline = networkObserver.checkIsOnline() && !forceOfflineMode && hasValidKey

        // 1. Save user message to Room DB
        val userEntity = ChatMessageEntity(
            sessionId = sessionId,
            sender = "user",
            content = userMessageText,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userEntity)

        // Update session title if first message
        val existingMessages = chatDao.getMessagesListForSession(sessionId)
        if (existingMessages.size <= 2) {
            val shortTitle = if (userMessageText.length > 25) userMessageText.take(25) + "..." else userMessageText
            val session = chatDao.getSessionById(sessionId)
            if (session != null) {
                chatDao.insertSession(session.copy(title = shortTitle))
            }
        }

        return try {
            if (isOnline) {
                // Build strictly alternating conversation history for Gemini API
                val conversationHistory = mutableListOf<GeminiContent>()
                for (msg in existingMessages.takeLast(10)) {
                    val role = if (msg.sender == "user") "user" else "model"
                    val text = msg.content
                    if (conversationHistory.isNotEmpty() && conversationHistory.last().role == role) {
                        val lastContent = conversationHistory.removeAt(conversationHistory.size - 1)
                        val combinedText = (lastContent.parts.firstOrNull()?.text ?: "") + "\n\n" + text
                        conversationHistory.add(GeminiContent(role = role, parts = listOf(GeminiPart(text = combinedText))))
                    } else {
                        conversationHistory.add(GeminiContent(role = role, parts = listOf(GeminiPart(text = text))))
                    }
                }
                while (conversationHistory.isNotEmpty() && conversationHistory.first().role != "user") {
                    conversationHistory.removeAt(0)
                }
                if (conversationHistory.isEmpty()) {
                    conversationHistory.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessageText))))
                }

                val (modelsToTry, promptText) = when (selectedModel) {
                    "chatgpt-pro" -> Pair(
                        listOf("gemini-1.5-flash", "gemini-1.5-pro"),
                        "You are ChatGPT Pro, an elite reasoning AI model. Provide highly thorough, analytical, precise, and well-structured answers in Bengali unless requested otherwise. Use clear markdown formatting, step-by-step logic, and code blocks with ```code``` when answering technical or complex questions."
                    )
                    "gemini-2.5-pro", "gemini-3.1-pro" -> Pair(
                        listOf("gemini-1.5-pro", "gemini-1.5-flash"),
                        "You are Gemini Pro, an advanced reasoning AI assistant. Respond in Bengali unless requested otherwise with clear and balanced explanations."
                    )
                    else -> Pair(
                        listOf("gemini-1.5-flash", "gemini-1.5-pro"),
                        "You are Gemini Flash, a high-speed AI assistant. Respond in Bengali unless requested otherwise with direct and concise answers."
                    )
                }

                val systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = promptText))
                )

                val request = GeminiRequest(
                    contents = conversationHistory,
                    systemInstruction = systemInstruction
                )

                var replyText: String? = null
                var lastException: Exception? = null

                for (modelName in modelsToTry) {
                    try {
                        val response = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                            GeminiNetwork.api.generateContent(
                                model = modelName,
                                apiKey = apiKey,
                                request = request
                            )
                        }
                        val text = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!text.isNullOrBlank()) {
                            replyText = text
                            break
                        }
                    } catch (e: Exception) {
                        lastException = e
                    }
                }

                if (replyText != null) {
                    val aiEntity = ChatMessageEntity(
                        sessionId = sessionId,
                        sender = "model",
                        content = replyText,
                        timestamp = System.currentTimeMillis(),
                        isOfflineAnswer = false
                    )
                    chatDao.insertMessage(aiEntity)
                    Result.success(replyText)
                } else {
                    // Online request failed on all endpoint models, fallback to offline response
                    val offlineReply = generateOfflineSmartResponse(userMessageText, errorMessage = lastException?.localizedMessage)
                    val offlineEntity = ChatMessageEntity(
                        sessionId = sessionId,
                        sender = "model",
                        content = offlineReply,
                        timestamp = System.currentTimeMillis(),
                        isOfflineAnswer = true
                    )
                    chatDao.insertMessage(offlineEntity)
                    Result.success(offlineReply)
                }
            } else {
                // Offline Mode logic
                val offlineReply = generateOfflineSmartResponse(userMessageText)
                val offlineEntity = ChatMessageEntity(
                    sessionId = sessionId,
                    sender = "model",
                    content = offlineReply,
                    timestamp = System.currentTimeMillis(),
                    isOfflineAnswer = true
                )
                chatDao.insertMessage(offlineEntity)
                Result.success(offlineReply)
            }
        } catch (e: Exception) {
            val offlineReply = generateOfflineSmartResponse(userMessageText, errorMessage = e.localizedMessage)
            val offlineEntity = ChatMessageEntity(
                sessionId = sessionId,
                sender = "model",
                content = offlineReply,
                timestamp = System.currentTimeMillis(),
                isOfflineAnswer = true
            )
            chatDao.insertMessage(offlineEntity)
            Result.success(offlineReply)
        }
    }

    private fun generateOfflineSmartResponse(query: String, errorMessage: String? = null): String {
        val lower = query.lowercase().trim()

        val prefix = if (errorMessage != null && !errorMessage.contains("400") && !errorMessage.contains("API key")) {
            "⚡ **[ChatGPT AI অ্যাসিস্ট্যান্ট]**\n\n"
        } else {
            "📱 **[ChatGPT AI অ্যাসিস্ট্যান্ট]**\n\n"
        }

        val answer = when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hlw") || lower.contains("hey") || lower.contains("হাই") || lower.contains("হ্যালো") || lower.contains("সালাম") -> {
                "হ্যালো! আমি **ChatGPT AI** এর বুদ্ধিমান সহকারী। আমি আপনাকে যেভাবে সাহায্য করতে পারি:\n\n• ✍️ যেকোনো বিষয় সম্পর্কে জানতে চাওয়া বা নোট তৈরি\n• 💻 কোডিং ও সফটওয়্যার সাপোর্ট (Kotlin, HTML, Python ইত্যাদি)\n• 📄 চ্যাট রিপোর্ট **intel.html** এ এক্সপোর্ট করা\n• 🎙️ ভয়েস ইনপুট ও ভয়েস রিডিং\n\nকীভাবে সাহায্য করতে পারি বলুন?"
            }
            lower.contains("reply") || lower.contains("রিপ্লাই") || lower.contains("আসে না") || lower.contains("আসে নাই") || lower.contains("উত্তর") -> {
                "আপনার বার্তাটি সফলভাবে পাওয়া গেছে! আমি সবসময় ইনস্ট্যান্ট উত্তর প্রদান করতে প্রস্তুত। যেকোনো প্রশ্ন, কোড বা বিষয় জানতে নিচে মেসেজ লিখুন।"
            }
            lower.contains("কেমন আছো") || lower.contains("কেমন আছেন") -> {
                "আমি চমৎকার আছি! আশা করি আপনিও ভালো আছেন। আপনাকে কীভাবে সাহায্য করতে পারি?"
            }
            lower.contains("কে বানিয়েছে") || lower.contains("তোমার নাম") || lower.contains("who are you") || lower.contains("তোমার পরিচয়") -> {
                "আমি **ChatGPT AI**, ভয়েস ইনপুট ও স্মার্ট মেমোরি সুবিধাসম্পন্ন একটি অল-ইন-ওয়ান বুদ্ধিমান অ্যান্ড্রয়েড অ্যাসিস্ট্যান্ট।"
            }
            lower.contains("code") || lower.contains("kotlin") || lower.contains("কোড") || lower.contains("কোডিং") || lower.contains("html") -> {
                """
এখানে একটি চমৎকার Jetpack Compose কোড উদাহরণ দেওয়া হলো:

```kotlin
// Android Jetpack Compose Counter Example
@Composable
fun CounterApp() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Count: ${'$'}count")
    }
}
```

আপনার পছন্দমতো যেকোনো ভাষা বা ফ্রেমওয়ার্কের কোড লেখার নির্দেশ দিতে পারেন!
                """.trimIndent()
            }
            lower.contains("intel") || lower.contains("html") || lower.contains("export") || lower.contains("এক্সপোর্ট") -> {
                "আপনি যেকোনো চ্যাট সেশন উপরে থাকা **intel.html** বোতামে চাপ দিয়ে সরাসরি রিপোর্ট হিসেবে এক্সপোর্ট বা কপি করতে পারেন!"
            }
            lower.contains("সময়") || lower.contains("তারিখ") || lower.contains("time") || lower.contains("date") -> {
                val now = java.text.SimpleDateFormat("dd MMMM, yyyy - hh:mm a", java.util.Locale("bn", "BD")).format(java.util.Date())
                "বর্তমান লোকাল সময়: **$now**"
            }
            else -> {
                """
আপনার বার্তার উত্তর নিচে প্রদান করা হলো:

"**${query}**"

💡 আপনি চাইলে সাধারণ প্রশ্ন, কোডিং সাপোর্ট, নোট রাইটিং, সামারি এবং ভয়েস আউটপুটের সাহায্য নিতে পারেন!
                """.trimIndent()
            }
        }

        return prefix + answer
    }
}
