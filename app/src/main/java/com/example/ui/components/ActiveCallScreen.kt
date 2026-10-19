package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveCallState
import com.example.model.CallStatus
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EndCallRed

@Composable
fun ActiveCallScreen(
    callState: ActiveCallState,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onToggleKeypad: () -> Unit,
    onSendDtmf: (String) -> Unit,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for avatar
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (callState.status == CallStatus.ACTIVE && !callState.isOnHold) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        color = DarkBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("active_call_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            DarkBackground,
                            Color(0xFF030712)
                        )
                    )
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Header with minimize button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier.testTag("minimize_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Minimize call",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Call status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when {
                                callState.status == CallStatus.DIALING -> "Dialing..."
                                callState.status == CallStatus.CONNECTING -> "Connecting..."
                                callState.isOnHold -> "Call on Hold"
                                callState.status == CallStatus.ENDED -> "Call Ended"
                                else -> callState.formattedDuration
                            },
                            color = when {
                                callState.isOnHold -> Color(0xFFF59E0B)
                                callState.status == CallStatus.ACTIVE -> ElectricCyan
                                else -> Color.White.copy(alpha = 0.8f)
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(48.dp)) // balance layout
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Pulsing Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.15f))
                    )

                    val contact = callState.contact
                    val colorIndex = contact?.avatarColorIndex ?: 0
                    val avatarColor = AvatarColors[colorIndex % AvatarColors.size]

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact?.initials ?: "?",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Name & Number
                Text(
                    text = callState.displayTitle,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = callState.phoneNumber,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )

                if (callState.dtmfDigits.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DTMF: ${callState.dtmfDigits}",
                        fontSize = 14.sp,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Keypad DTMF or standard in-call controls
                AnimatedVisibility(
                    visible = callState.isKeypadOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    InCallDtmfKeypad(onSendDigit = onSendDtmf)
                }

                if (!callState.isKeypadOpen) {
                    // 2 rows of in-call control buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            InCallControlItem(
                                icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                label = if (callState.isMuted) "Muted" else "Mute",
                                isActive = callState.isMuted,
                                onClick = onToggleMute,
                                testTag = "call_action_mute"
                            )

                            InCallControlItem(
                                icon = Icons.Default.Dialpad,
                                label = "Keypad",
                                isActive = callState.isKeypadOpen,
                                onClick = onToggleKeypad,
                                testTag = "call_action_keypad"
                            )

                            InCallControlItem(
                                icon = if (callState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                label = if (callState.isSpeakerOn) "Speaker" else "Earpiece",
                                isActive = callState.isSpeakerOn,
                                onClick = onToggleSpeaker,
                                testTag = "call_action_speaker"
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            InCallControlItem(
                                icon = Icons.Default.PersonAdd,
                                label = "Add call",
                                isActive = false,
                                onClick = onMinimize,
                                testTag = "call_action_add"
                            )

                            InCallControlItem(
                                icon = Icons.Default.Pause,
                                label = if (callState.isOnHold) "Unhold" else "Hold",
                                isActive = callState.isOnHold,
                                onClick = onToggleHold,
                                testTag = "call_action_hold"
                            )

                            InCallControlItem(
                                icon = Icons.Outlined.KeyboardArrowDown,
                                label = "Minimize",
                                isActive = false,
                                onClick = onMinimize,
                                testTag = "call_action_minimize"
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        InCallControlItem(
                            icon = Icons.Default.Dialpad,
                            label = "Hide Keypad",
                            isActive = true,
                            onClick = onToggleKeypad,
                            testTag = "hide_dtmf_keypad"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Large Red End Call Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(EndCallRed)
                        .clickable { onEndCall() }
                        .testTag("end_call_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun InCallControlItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) ElectricCyan else Color.White.copy(alpha = 0.12f)
                )
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun InCallDtmfKeypad(onSendDigit: (String) -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { onSendDigit(digit) }
                            .testTag("dtmf_key_$digit"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
