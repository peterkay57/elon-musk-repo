package com.example.model

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val label: String = "Mobile",
    val isFavorite: Boolean = false,
    val avatarColorIndex: Int = 0,
    val lookupKey: String? = null
) {
    val initials: String
        get() {
            val parts = name.trim().split(" ").filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts[0].take(1).uppercase()
                else -> "${parts[0].take(1)}${parts[parts.lastIndex].take(1)}".uppercase()
            }
        }
}
