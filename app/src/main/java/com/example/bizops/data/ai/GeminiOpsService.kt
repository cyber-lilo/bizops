package com.example.bizops.data.ai

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

object GeminiOpsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    data class GeneratedEmail(
        val subject: String,
        val body: String
    )

    suspend fun generateCustomEmail(
        prompt: String,
        tone: String = "Professional",
        senderName: String = "Operations Manager",
        companyName: String = "BizOps Enterprise",
        clientName: String = "Valued Client"
    ): GeneratedEmail = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val systemPrompt = """
                    You are an expert executive business communication specialist.
                    Generate a high-impact daily business email based on the user's instructions.
                    Tone: $tone.
                    Sender: $senderName from $companyName.
                    Recipient: $clientName.
                    
                    Return ONLY a JSON object with strictly these two fields:
                    {
                      "subject": "Email Subject Line Here",
                      "body": "Complete email body formatted with clean line breaks."
                    }
                    Do not enclose in markdown blocks or extra text.
                """.trimIndent()

                val resultJson = callGemini(apiKey, systemPrompt, prompt)
                if (resultJson != null) {
                    val cleaned = resultJson.trim().removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()
                    val json = JSONObject(cleaned)
                    return@withContext GeneratedEmail(
                        subject = json.optString("subject", "Business Update from $companyName"),
                        body = json.optString("body", "")
                    )
                }
            } catch (e: Exception) {
                // Fall back to offline generator
            }
        }
        // Smart offline generator fallback
        return@withContext generateOfflineEmail(prompt, tone, senderName, companyName, clientName)
    }

    suspend fun enhanceOrRephraseEmail(
        currentSubject: String,
        currentBody: String,
        tone: String,
        instruction: String
    ): GeneratedEmail = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val systemPrompt = """
                    You are an elite business communication editor.
                    Refine the provided email according to the target tone ($tone) and instructions: $instruction.
                    Ensure the output is polished, free of fluff, polite, and clearly structured.
                    
                    Return ONLY a JSON object:
                    {
                      "subject": "Refined Subject Line",
                      "body": "Refined email body"
                    }
                """.trimIndent()

                val userPrompt = """
                    Original Subject: $currentSubject
                    Original Body:
                    $currentBody
                """.trimIndent()

                val resultJson = callGemini(apiKey, systemPrompt, userPrompt)
                if (resultJson != null) {
                    val cleaned = resultJson.trim().removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()
                    val json = JSONObject(cleaned)
                    return@withContext GeneratedEmail(
                        subject = json.optString("subject", currentSubject),
                        body = json.optString("body", currentBody)
                    )
                }
            } catch (e: Exception) {
                // Fall back
            }
        }

        // Offline enhancement
        return@withContext enhanceOffline(currentSubject, currentBody, tone)
    }

    private fun callGemini(apiKey: String, systemInstruction: String, userPrompt: String): String? {
        val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
        val jsonPayload = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val bodyStr = response.body?.string() ?: return null
        if (!response.isSuccessful) return null

        val rootObj = JSONObject(bodyStr)
        val candidates = rootObj.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        return parts.getJSONObject(0).optString("text")
    }

    private fun generateOfflineEmail(
        prompt: String,
        tone: String,
        senderName: String,
        companyName: String,
        clientName: String
    ): GeneratedEmail {
        val lower = prompt.lowercase()
        val subject: String
        val body: String

        when {
            lower.contains("remind") || lower.contains("payment") || lower.contains("due") -> {
                subject = when (tone.lowercase()) {
                    "urgent" -> "URGENT: Outstanding Balance Notice - $companyName"
                    "friendly" -> "Friendly Reminder: Upcoming Invoice Settlement"
                    else -> "Invoice Payment Status & Settlement Request - $companyName"
                }
                body = """Dear $clientName,

I hope this message finds you well.

Regarding our recent discussions and project milestones ($prompt), we would like to confirm the payment settlement schedule.

Summary of Request:
• Overview: $prompt
• Status: Pending verification
• Payment Instructions: Available on the attached invoice documentation.

Please let us know if you need any adjustments or if our accounts department can assist you further.

Warm regards,
$senderName
$companyName"""
            }

            lower.contains("proposal") || lower.contains("quote") || lower.contains("scope") -> {
                subject = "Proposal & Project Scope: $companyName x $clientName"
                body = """Hi $clientName,

Thank you for exploring this collaboration with $companyName.

Based on our review, here is our operational scope proposal:

Objectives & Focus:
• $prompt
• Rapid turnaround, dedicated SLA support, and end-to-end deliverables.

Next Steps:
We are ready to initiate the discovery phase immediately upon your sign-off. Would you have 15 minutes this week to align on the kickoff schedule?

Best regards,
$senderName
$companyName"""
            }

            lower.contains("meeting") || lower.contains("schedule") || lower.contains("call") -> {
                subject = "Meeting Request: Operational Alignment - $companyName"
                body = """Hi $clientName,

I would like to schedule a quick 20-minute discussion regarding $prompt.

Proposed Discussion Topics:
1. Operational overview & current milestone health
2. Key priorities and timeline targets
3. Open questions and immediate action items

Please let me know if tomorrow afternoon or Thursday morning works best for your schedule.

Looking forward to connecting!

Best regards,
$senderName
$companyName"""
            }

            else -> {
                subject = "Update: $prompt - $companyName"
                body = """Hi $clientName,

I am reaching out regarding: $prompt.

Key Highlights:
• Current Progress: Operations running smoothly according to plan.
• Priorities: Ensuring all milestones are delivered with top quality.
• Action Required: Please review the latest status and let us know if you have any questions.

Thank you for your ongoing partnership!

Sincerely,
$senderName
$companyName"""
            }
        }

        return GeneratedEmail(subject, body)
    }

    private fun enhanceOffline(
        currentSubject: String,
        currentBody: String,
        tone: String
    ): GeneratedEmail {
        val prefix = when (tone.lowercase()) {
            "urgent" -> "[Action Required] "
            "formal" -> "Official Notice: "
            "friendly" -> "Quick Update: "
            "concise" -> ""
            else -> ""
        }

        val enhancedBody = when (tone.lowercase()) {
            "concise" -> {
                currentBody.lines()
                    .filter { it.isNotBlank() }
                    .take(6)
                    .joinToString("\n\n")
            }
            "formal" -> {
                "Dear Valued Partner,\n\n$currentBody\n\nWe remain at your service for any inquiries."
            }
            "urgent" -> {
                "TIME SENSITIVE NOTICE:\n\n$currentBody\n\nPlease confirm receipt and provide an update today."
            }
            else -> currentBody
        }

        return GeneratedEmail(
            subject = if (currentSubject.startsWith(prefix)) currentSubject else "$prefix$currentSubject",
            body = enhancedBody
        )
    }
}
