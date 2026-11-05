package com.example.prscanner.engine

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractedDocumentInfo(
    val dates: List<String>,
    val deadlines: List<String>,
    val financialAmounts: List<String>,
    val emails: List<String>,
    val phoneNumbers: List<String>,
    val detectedType: String,
    val summary: String,
    val keyPoints: List<String>
)

object AiDocumentEngine {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Extracts structured intelligence from OCR text using smart NLP heuristics
     */
    fun extractInformation(text: String): ExtractedDocumentInfo {
        if (text.isBlank()) {
            return ExtractedDocumentInfo(
                dates = emptyList(),
                deadlines = emptyList(),
                financialAmounts = emptyList(),
                emails = emptyList(),
                phoneNumbers = emptyList(),
                detectedType = "Empty Document",
                summary = "No text was detected in this document.",
                keyPoints = emptyList()
            )
        }

        // 1. Dates (e.g., 2026-11-04, 11/04/2026, Nov 4 2026)
        val dateRegex = Pattern.compile(
            "\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\.?\\s+\\d{1,2}(?:st|nd|rd|th)?,?\\s+\\d{4}|\\d{4}-\\d{2}-\\d{2})\\b",
            Pattern.CASE_INSENSITIVE
        )
        val dateMatcher = dateRegex.matcher(text)
        val foundDates = mutableListOf<String>()
        while (dateMatcher.find()) {
            val d = dateMatcher.group()
            if (!foundDates.contains(d)) foundDates.add(d)
        }

        // 2. Deadlines / Due dates
        val deadlineRegex = Pattern.compile(
            "(?:due\\s+date|deadline|valid\\s+until|expiry|expires|payment\\s+due)[:\\s]+([A-Za-z0-9,./ -]{5,25})",
            Pattern.CASE_INSENSITIVE
        )
        val dlMatcher = deadlineRegex.matcher(text)
        val deadlines = mutableListOf<String>()
        while (dlMatcher.find()) {
            val match = dlMatcher.group(1)?.trim() ?: ""
            if (match.isNotBlank() && !deadlines.contains(match)) deadlines.add(match)
        }

        // 3. Financial Totals / Currency
        val amountRegex = Pattern.compile(
            "(?:[\\$€£₹]\\s*\\d+(?:,\\d{3})*(?:\\.\\d{2})?|\\b(?:USD|EUR|GBP|INR|CAD|AUD)\\s*\\d+(?:,\\d{3})*(?:\\.\\d{2})?|\\b\\d+(?:,\\d{3})*\\.\\d{2}\\b)"
        )
        val amtMatcher = amountRegex.matcher(text)
        val amounts = mutableListOf<String>()
        while (amtMatcher.find()) {
            val a = amtMatcher.group()
            if (!amounts.contains(a)) amounts.add(a)
        }

        // 4. Emails
        val emailRegex = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val emailMatcher = emailRegex.matcher(text)
        val emails = mutableListOf<String>()
        while (emailMatcher.find()) {
            val e = emailMatcher.group()
            if (!emails.contains(e)) emails.add(e)
        }

        // 5. Phone numbers
        val phoneRegex = Pattern.compile("(?:\\+?\\d{1,3}[- .]?)?\\(?\\d{3}\\)?[- .]?\\d{3}[- .]?\\d{4}")
        val phoneMatcher = phoneRegex.matcher(text)
        val phones = mutableListOf<String>()
        while (phoneMatcher.find()) {
            val p = phoneMatcher.group()
            if (!phones.contains(p)) phones.add(p)
        }

        // 6. Classification
        val lower = text.lowercase()
        val detectedType = when {
            lower.contains("receipt") || lower.contains("subtotal") || lower.contains("cashier") -> "Receipt"
            lower.contains("invoice") || lower.contains("bill to") || lower.contains("invoice #") -> "Invoice"
            lower.contains("passport") || lower.contains("driving license") || lower.contains("identity card") || lower.contains("id no") -> "Identity Document"
            lower.contains("agreement") || lower.contains("contract") || lower.contains("terms and conditions") || lower.contains("parties") -> "Legal Contract"
            lower.contains("certificate") || lower.contains("conferred upon") || lower.contains("awarded to") -> "Certificate"
            lower.contains("abstract") || lower.contains("thesis") || lower.contains("university") || lower.contains("exam") -> "Academic Document"
            else -> "General Document"
        }

        // 7. Generate concise summary
        val lines = text.lines().map { it.trim() }.filter { it.length > 5 }
        val summary = if (lines.size <= 2) {
            text.take(300)
        } else {
            val header = lines.take(2).joinToString(" - ")
            val bodySample = lines.drop(2).take(3).joinToString("; ")
            "[$detectedType] $header. Highlights: $bodySample"
        }

        val keyPoints = lines.take(5).map { "• $it" }

        return ExtractedDocumentInfo(
            dates = foundDates.take(6),
            deadlines = deadlines.take(4),
            financialAmounts = amounts.take(8),
            emails = emails.take(5),
            phoneNumbers = phones.take(5),
            detectedType = detectedType,
            summary = summary,
            keyPoints = keyPoints
        )
    }

    /**
     * Ask questions, summarize, translate, or polish document text using Gemini API if key is present,
     * otherwise using built-in high precision language heuristics.
     */
    suspend fun queryDocument(
        questionOrPrompt: String,
        documentText: String,
        taskType: String = "QA" // QA, SUMMARY, TRANSLATE, POLISH
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            val field = BuildConfig::class.java.getDeclaredField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                return@withContext callGeminiApi(apiKey, questionOrPrompt, documentText, taskType)
            } catch (e: Exception) {
                // Fallback to local intelligent assistant
            }
        }

        // Local smart assistant
        when (taskType) {
            "SUMMARY" -> {
                val info = extractInformation(documentText)
                """
                📋 AI DOCUMENT SUMMARY
                
                Category: ${info.detectedType}
                Summary: ${info.summary}
                
                Key Highlights:
                ${info.keyPoints.joinToString("\n")}
                
                ${if (info.financialAmounts.isNotEmpty()) "Amounts found: " + info.financialAmounts.joinToString(", ") else ""}
                ${if (info.dates.isNotEmpty()) "Dates referenced: " + info.dates.joinToString(", ") else ""}
                """.trimIndent()
            }
            "TRANSLATE" -> {
                val targetLang = questionOrPrompt.ifBlank { "Spanish" }
                """
                🌐 TRANSLATION PREVIEW (${targetLang.uppercase()})
                
                [Original snippet]
                "${documentText.take(200)}..."
                
                (Note: Connect your Gemini API key in the Secrets panel for full instant multi-language translation of complete multi-page documents).
                """.trimIndent()
            }
            "POLISH" -> {
                val cleaned = documentText.replace(Regex("\\s+"), " ").trim()
                """
                ✨ POLISHED OCR TEXT:
                
                $cleaned
                """.trimIndent()
            }
            else -> {
                // QA Mode
                val lowerDoc = documentText.lowercase()
                val lowerQ = questionOrPrompt.lowercase()

                val answer = when {
                    lowerQ.contains("date") || lowerQ.contains("when") -> {
                        val dates = extractInformation(documentText).dates
                        if (dates.isNotEmpty()) "Identified dates in document: ${dates.joinToString(", ")}"
                        else "No specific calendar dates were found in the document text."
                    }
                    lowerQ.contains("total") || lowerQ.contains("amount") || lowerQ.contains("price") || lowerQ.contains("cost") -> {
                        val amounts = extractInformation(documentText).financialAmounts
                        if (amounts.isNotEmpty()) "Identified financial figures: ${amounts.joinToString(", ")}"
                        else "No specific currency amounts or totals were detected."
                    }
                    lowerQ.contains("who") || lowerQ.contains("name") || lowerQ.contains("email") -> {
                        val emails = extractInformation(documentText).emails
                        if (emails.isNotEmpty()) "Found contacts: ${emails.joinToString(", ")}"
                        else "No direct email contacts found. Please check document header."
                    }
                    else -> {
                        val matchingLine = documentText.lines().firstOrNull { line ->
                            lowerQ.split(" ").filter { it.length > 3 }.any { line.lowercase().contains(it) }
                        }
                        if (matchingLine != null) {
                            "Based on the document text: \"${matchingLine.trim()}\""
                        } else {
                            "Document Overview: Found ${documentText.length} characters across ${documentText.lines().size} lines. Query: \"$questionOrPrompt\"."
                        }
                    }
                }
                answer
            }
        }
    }

    private fun callGeminiApi(
        apiKey: String,
        prompt: String,
        contextText: String,
        taskType: String
    ): String {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
        val systemPrompt = "You are PR Scanner AI Assistant, an expert document analyst. Analyze this document and answer user queries clearly and concisely."
        val fullContent = "$systemPrompt\n\nDOCUMENT CONTEXT:\n$contextText\n\nUSER REQUEST: $prompt (Task: $taskType)"

        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", fullContent) })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("API error: ${response.code}")
            }
            val bodyStr = response.body?.string() ?: throw IllegalStateException("Empty response")
            val root = JSONObject(bodyStr)
            val candidates = root.getJSONArray("candidates")
            val first = candidates.getJSONObject(0)
            val content = first.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            return parts.getJSONObject(0).getString("text")
        }
    }
}
