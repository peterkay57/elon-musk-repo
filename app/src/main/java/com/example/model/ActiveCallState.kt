package com.example.model

enum class CallStatus {
    DIALING,
    CONNECTING,
    ACTIVE,
    HOLD,
    ENDED
}

data class ActiveCallState(
    val phoneNumber: String,
    val contactName: String?,
    val contact: Contact? = null,
    val status: CallStatus = CallStatus.DIALING,
    val durationSeconds: Long = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isOnHold: Boolean = false,
    val isKeypadOpen: Boolean = false,
    val dtmfDigits: String = ""
) {
    val formattedDuration: String
        get() {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }

    val displayTitle: String
        get() = contactName ?: contact?.name ?: phoneNumber
}
