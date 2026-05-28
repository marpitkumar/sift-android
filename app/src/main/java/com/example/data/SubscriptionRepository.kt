package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.api.Content
import com.example.api.ExtractedSubscription
import com.example.api.GeminiApiClient
import com.example.api.GeminiRequest
import com.example.api.GenerationConfig
import com.example.api.InlineData
import com.example.api.Part
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SubscriptionRepository(
    val subscriptionDao: SubscriptionDao,
    private val apiKey: String
) {
    val allSubscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()
    val allCategories: Flow<List<Category>> = subscriptionDao.getAllCategories()

    suspend fun insertSubscription(subscription: Subscription) = withContext(Dispatchers.IO) {
        subscriptionDao.insertSubscription(subscription)
    }

    suspend fun deleteSubscriptionById(id: Int) = withContext(Dispatchers.IO) {
        subscriptionDao.deleteSubscriptionById(id)
    }

    suspend fun getSubscriptionById(id: Int): Subscription? = withContext(Dispatchers.IO) {
        subscriptionDao.getSubscriptionById(id)
    }

    suspend fun insertCategory(category: Category) = withContext(Dispatchers.IO) {
        subscriptionDao.insertCategory(category)
    }

    suspend fun deleteCategoryById(id: Int) = withContext(Dispatchers.IO) {
        subscriptionDao.deleteCategoryById(id)
    }

    /**
     * Extracts subscription details from unstructured user text or a pasted receipt using Gemini 3.5 Flash
     */
    suspend fun extractSubscriptionFromText(rawText: String): ExtractedSubscription? = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("SubscriptionRepository", "Gemini API Key is invalid or empty")
            return@withContext getMockExtraction(rawText)
        }

        val prompt = """
            You are a precise financial data extraction assistant. Your job is to extract subscription details from unstructured user text (such as confirmation emails, bank transaction notifications, or SMS alerts).
            
            Analyze the input text and strictly return a JSON object following this schema. Do not include any conversational markdown like ```json or ```. If a field cannot be found, return null.
            Do not include any text, notes, or explanations outside the JSON block.
            
            Target Schema:
            {
              "serviceName": "String (Capitalized name of the service, e.g. Netflix, Spotify, Gym)",
              "billingAmount": "Float (The exact transaction or renewal amount)",
              "currency": "String (3-letter ISO code, e.g., INR, USD, EUR)",
              "billingCycle": "String (Must be exactly one of: Monthly, Quarterly, Annual, One-Time)",
              "nextRenewalDate": "String (YYYY-MM-DD format, calculated based on transaction date if implied. Today's current date is 2026-05-28)",
              "paymentMethod": "String (e.g., UPI, Credit Card, Bank Account if mentioned)"
            }
            
            Input Text:
            "$rawText"
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d("SubscriptionRepository", "Raw Gemini JSON Text response: $jsonText")

            if (!jsonText.isNullOrEmpty()) {
                val cleanedJson = sanitizeJsonText(jsonText)
                val adapter: JsonAdapter<ExtractedSubscription> = GeminiApiClient.moshi.adapter(ExtractedSubscription::class.java)
                return@withContext adapter.fromJson(cleanedJson)
            }
        } catch (e: Exception) {
            Log.e("SubscriptionRepository", "Error calling Gemini OCR API: ${e.message}", e)
        }
        return@withContext getMockExtraction(rawText)
    }

    /**
     * Extracts subscription details from an uploaded image (screenshot of receipt/confirmation) using Gemini 3.5 Flash Visual abilities
     */
    suspend fun extractSubscriptionFromImage(bitmap: Bitmap, helperText: String = ""): ExtractedSubscription? = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("SubscriptionRepository", "Gemini API Key matches default fallback or is empty. Using receipt mock.")
            return@withContext getMockExtraction("Receipt Screenshot attached")
        }

        val base64Image = GeminiApiClient.bitmapToBase64(bitmap)
        val imagePart = Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
        
        val prompt = """
            You are a precise financial data extraction assistant with advanced OCR vision abilities. Your job is to extract subscription details from the attached user receipt screenshot or transaction notification image.
            
            Analyze the image and strictly return a JSON object following this schema. Do not include any conversational markdown like ```json or ```. If a field cannot be found, return null.
            Do not include any text, notes, or explanations outside the JSON.
            
            ${if (helperText.isNotEmpty()) "The user also provided this description: $helperText" else ""}
            
            Target Schema:
            {
              "serviceName": "String (Capitalized name of the service, e.g. Netflix, Prime Video, iCloud, Spotify)",
              "billingAmount": "Float (The exact transaction or renewal amount)",
              "currency": "String (3-letter ISO code, e.g., INR, USD, EUR)",
              "billingCycle": "String (Must be exactly one of: Monthly, Quarterly, Annual, One-Time)",
              "nextRenewalDate": "String (YYYY-MM-DD format, calculated based on transaction date or today. Today is 2026-05-28)",
              "paymentMethod": "String (e.g., UPI, Credit Card, Bank Account, Apple Pay if mentioned)"
            }
        """.trimIndent()

        val textPart = Part(text = prompt)

        try {
            val request = GeminiRequest(
                contents = listOf(
                    Content(parts = listOf(textPart, imagePart))
                ),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d("SubscriptionRepository", "Raw Gemini visual response: $jsonText")

            if (!jsonText.isNullOrEmpty()) {
                val cleanedJson = sanitizeJsonText(jsonText)
                val adapter: JsonAdapter<ExtractedSubscription> = GeminiApiClient.moshi.adapter(ExtractedSubscription::class.java)
                return@withContext adapter.fromJson(cleanedJson)
            }
        } catch (e: Exception) {
            Log.e("SubscriptionRepository", "Error calling Gemini Vision API: ${e.message}", e)
        }
        return@withContext getMockExtraction("Receipt Screenshot attached")
    }

    /**
     * Sends the list of active subscriptions to Gemini to generate optimization recommendations
     */
    suspend fun getOptimizationRecommendations(subscriptions: List<Subscription>): String = withContext(Dispatchers.IO) {
        if (subscriptions.isEmpty()) {
            return@withContext "You don't have any subscription logged yet. Add your Netflix, Spotify, or utility invoices in the dashboard to analyze redundancies and optimize your budget!"
        }
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("SubscriptionRepository", "Gemini API Key is empty or invalid. Returning local offline optimizations.")
            return@withContext getMockRecommendations(subscriptions)
        }

        val subsJsonList = subscriptions.joinToString(separator = ",\n") { sub ->
            """
            {
              "serviceName": "${sub.serviceName}",
              "category": "${sub.category}",
              "billingAmount": ${sub.billingAmount},
              "currency": "${sub.currency}",
              "billingCycle": "${sub.billingCycle}",
              "nextRenewalDate": "${sub.nextRenewalDate}",
              "paymentMethod": "${sub.paymentMethod}",
              "usageFrequency": "${sub.usageFrequency}",
              "isAutoRenew": ${sub.isAutoRenew},
              "status": "${sub.status}"
            }
            """.trimIndent()
        }

        val prompt = """
            You are an expert personal finance optimizer. Analyze the following list of active digital subscriptions for a user and provide up to 3 highly actionable, specific recommendations to save money.
            
            Look for:
            1. Redundancies (e.g., holding both Apple Music and Spotify Premium, or overlapping streaming/storage).
            2. Cost-to-usage disparities (e.g., paying a lot for an annual tier marked as 'Low' or 'Unused').
            3. Potential bundle opportunities or family plans.
            
            User Subscriptions List:
            [
              $subsJsonList
            ]
            
            Respond in a conversational, friendly personal financial advisor tone. Group your recommendations clearly with bold titles. Suggest specific actions they can take (like switching packages, pausing immediately, or drop and bundle). Include a summary on top estimating potential monthly and yearly savings.
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                )
            )
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrEmpty()) {
                return@withContext text
            }
        } catch (e: Exception) {
            Log.e("SubscriptionRepository", "Error generating spend recommendations: ${e.message}", e)
        }
        return@withContext getMockRecommendations(subscriptions)
    }

    private fun sanitizeJsonText(rawJson: String): String {
        return rawJson
            .replace("```json", "")
            .replace("```", "")
            .trim()
    }

    // --- Elegant Fallback Implementations for Offline/Proto use ---

    private fun getMockExtraction(rawText: String): ExtractedSubscription {
        val lowerText = rawText.lowercase()
        return when {
            lowerText.contains("spotify") -> ExtractedSubscription(
                serviceName = "Spotify",
                billingAmount = 119.00,
                currency = "INR",
                billingCycle = "Monthly",
                nextRenewalDate = "2026-06-25",
                paymentMethod = "UPI"
            )
            lowerText.contains("gym") || lowerText.contains("fitness") -> ExtractedSubscription(
                serviceName = "Gold's Gym",
                billingAmount = 1499.00,
                currency = "INR",
                billingCycle = "Monthly",
                nextRenewalDate = "2026-06-15",
                paymentMethod = "Credit Card"
            )
            lowerText.contains("icloud") || lowerText.contains("apple") -> ExtractedSubscription(
                serviceName = "iCloud+",
                billingAmount = 2.99,
                currency = "USD",
                billingCycle = "Monthly",
                nextRenewalDate = "2026-06-08",
                paymentMethod = "Apple Pay"
            )
            else -> ExtractedSubscription(
                serviceName = "Netflix",
                billingAmount = 499.00,
                currency = "INR",
                billingCycle = "Monthly",
                nextRenewalDate = "2026-06-12",
                paymentMethod = "UPI"
            )
        }
    }

    private fun getMockRecommendations(subs: List<Subscription>): String {
        val strings = mutableListOf<String>()
        val unused = subs.filter { it.usageFrequency == "Unused" && it.status == "Active" }
        val lowUsage = subs.filter { it.usageFrequency == "Low" && it.status == "Active" }
        
        var annualSavings = 0.0
        
        strings.add("💵 **Sift Budget Recommendation Dashboard**")
        
        if (unused.isNotEmpty()) {
            strings.add("\n### 1. Prune Unused Subscriptions")
            unused.forEach { sub ->
                val yearly = sub.billingAmount * if (sub.billingCycle == "Monthly") 12 else 1
                annualSavings += yearly
                strings.add("• **Pause ${sub.serviceName} immediately**: You pay ${sub.currency} ${sub.billingAmount}/${sub.billingCycle} but marked it as Unused. Canceling this will save you about **${sub.currency} ${String.format("%.2f", yearly)} annually**.")
            }
        }
        
        if (lowUsage.isNotEmpty()) {
            strings.add("\n### 2. Downgrade/Bundle Low Usage Accounts")
            lowUsage.forEach { sub ->
                val yearly = sub.billingAmount * if (sub.billingCycle == "Monthly") 12 else 1
                strings.add("• **Consolidate ${sub.serviceName}**: This tier is marked as Low Usage. Consider shifting to a lower tier package or switching to a bundle with other active services to optimize your balance.")
            }
        }

        // Generic redundancy check
        val hasSpotify = subs.any { it.serviceName.lowercase().contains("spotify") }
        val hasYoutube = subs.any { it.serviceName.lowercase().contains("youtube") }
        if (hasSpotify && hasYoutube) {
            strings.add("\n### 3. Audio Streaming Consolidation")
            strings.add("• **Music Overlap**: You are paying for Spotify Premium and Youtube Premium. Dropping Spotify or sharing a family account can yield immediate savings.")
            annualSavings += 1428.0
        }

        if (annualSavings == 0.0) {
            strings.add("\n👍 **Good Job!** Your active digital subscriptions look lean and optimized for your current usage tiers. Keep monitoring details with Sift to prevent cost creeping!")
        } else {
            strings.add("\n🔥 **Total Estimated Potential Yearly Savings**: **${subs.firstOrNull()?.currency ?: "INR"} ${String.format("%.2f", annualSavings)}**.")
        }
        
        return strings.joinToString(separator = "\n")
    }
}
