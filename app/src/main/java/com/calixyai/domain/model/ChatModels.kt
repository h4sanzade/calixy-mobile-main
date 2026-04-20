package com.calixyai.domain.model

enum class Sender { BOT, USER }

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val sender: Sender,
    val text: String,
    val type: MessageType = MessageType.TEXT,
    /** Which step produced this user message — used for edit re-entry */
    val editableStep: ChatStep? = null
)

enum class MessageType { TEXT, BMI_CARD, ANALYSIS_CARD, TYPING }

enum class ChatStep {
    FIRST_NAME,
    LAST_NAME,
    GENDER,
    AGE,
    HEIGHT_WEIGHT,
    ACTIVITY,
    GOAL,
    WEIGHT_DIRECTION,   // new: ask gain / lose / maintain intent
    ALLERGIES,
    DIETARY,
    COMPLETE
}

data class SetupProfile(
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "",
    val age: Int? = null,
    val heightCm: Int? = null,
    val weightKg: Float? = null,
    val bmi: Float? = null,
    val activityLevel: String = "",
    val goal: String = "",
    val weightDirection: String = "",   // "gain" | "lose" | "maintain"
    val allergies: List<String> = emptyList(),
    val dietaryRules: List<String> = emptyList(),
    val customAllergy: String? = null
)