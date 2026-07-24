package com.example.utils

import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HtmlExporter {

    fun generateIntelHtml(session: ChatSessionEntity, messages: List<ChatMessageEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val generatedDate = dateFormat.format(Date())

        val messagesHtml = StringBuilder()
        for (msg in messages) {
            val isUser = msg.sender == "user"
            val badge = if (isUser) "USER" else if (msg.isOfflineAnswer) "OFFLINE AI" else "GEMINI AI"
            val badgeClass = if (isUser) "user-badge" else if (msg.isOfflineAnswer) "offline-badge" else "ai-badge"
            val formattedTime = dateFormat.format(Date(msg.timestamp))
            val escapedContent = escapeHtml(msg.content)

            messagesHtml.append("""
                <div class="message-card ${if (isUser) "message-user" else "message-ai"}">
                    <div class="message-header">
                        <span class="badge $badgeClass">$badge</span>
                        <span class="message-time">$formattedTime</span>
                    </div>
                    <div class="message-body">$escapedContent</div>
                </div>
            """.trimIndent())
        }

        return """
<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ChatGPT AI - Intel Report (${session.title})</title>
    <style>
        :root {
            --bg-color: #0d1117;
            --card-bg: #161b22;
            --text-color: #c9d1d9;
            --accent-color: #10a37f;
            --user-bg: #1f2937;
            --ai-bg: #111827;
            --border-color: #30363d;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            background-color: var(--bg-color);
            color: var(--text-color);
            margin: 0;
            padding: 20px;
            line-height: 1.6;
        }

        .container {
            max-width: 900px;
            margin: 0 auto;
        }

        .header {
            background: linear-gradient(135deg, #10a37f 0%, #059669 100%);
            color: white;
            padding: 24px;
            border-radius: 12px;
            margin-bottom: 24px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        }

        .header h1 {
            margin: 0 0 8px 0;
            font-size: 24px;
        }

        .header p {
            margin: 4px 0;
            font-size: 14px;
            opacity: 0.9;
        }

        .stats-bar {
            display: flex;
            gap: 16px;
            margin-top: 12px;
            font-size: 13px;
            background: rgba(0,0,0,0.2);
            padding: 8px 12px;
            border-radius: 6px;
        }

        .message-card {
            background: var(--card-bg);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 16px 20px;
            margin-bottom: 16px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.2);
        }

        .message-user {
            border-left: 4px solid #3b82f6;
            background: var(--user-bg);
        }

        .message-ai {
            border-left: 4px solid #10a37f;
            background: var(--ai-bg);
        }

        .message-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
        }

        .badge {
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 11px;
            font-weight: bold;
            letter-spacing: 0.5px;
        }

        .user-badge { background: #2563eb; color: white; }
        .ai-badge { background: #10a37f; color: white; }
        .offline-badge { background: #d97706; color: white; }

        .message-time {
            font-size: 12px;
            color: #8b949e;
        }

        .message-body {
            font-size: 15px;
            white-space: pre-wrap;
            word-break: break-word;
        }

        .footer {
            text-align: center;
            padding: 20px;
            color: #8b949e;
            font-size: 13px;
            border-top: 1px solid var(--border-color);
            margin-top: 30px;
        }

        @media print {
            body { background: white; color: black; }
            .message-card { border: 1px solid #ccc; background: white; color: black; }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>ChatGPT AI - Intel Session Report</h1>
            <p><strong>Session Title:</strong> ${escapeHtml(session.title)}</p>
            <p><strong>Generated On:</strong> $generatedDate</p>
            <div class="stats-bar">
                <span>💬 Total Messages: ${messages.size}</span>
                <span>⚡ Mode: ${if (session.isOfflineMode) "Offline Mode" else "Online Gemini AI"}</span>
                <span>📑 File ID: intel.html</span>
            </div>
        </div>

        <div class="chat-container">
            $messagesHtml
        </div>

        <div class="footer">
            <p>Generated by ChatGPT AI Android App • Intel HTML Export System</p>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
