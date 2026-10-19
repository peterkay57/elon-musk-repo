package com.example.model

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED
}

data class CallRecord(
    val id: String,
    val name: String?,
    val phoneNumber: String,
    val type: CallType,
    val timestamp: Long,
    val durationSeconds: Long = 0,
    val contactId: String? = null
) {
    val displayName: String
        get() = if (!name.isNullOrBlank()) name else phoneNumber

    val formattedDuration: String
        get() {
            if (durationSeconds <= 0) {
                return if (type == CallType.MISSED) "Missed" else if (type == CallType.REJECTED) "Declined" else "0s"
            }
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        }
}
