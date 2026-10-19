package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Contact
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.CallGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.VibrantGreen

data class KeypadKey(
    val digit: String,
    val letters: String
)

val KeypadRows = listOf(
    listOf(KeypadKey("1", ""), KeypadKey("2", "ABC"), KeypadKey("3", "DEF")),
    listOf(KeypadKey("4", "GHI"), KeypadKey("5", "JKL"), KeypadKey("6", "MNO")),
    listOf(KeypadKey("7", "PQRS"), KeypadKey("8", "TUV"), KeypadKey("9", "WXYZ")),
    listOf(KeypadKey("*", ""), KeypadKey("0", "+"), KeypadKey("#", ""))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadTab(
    dialedNumber: String,
    t9Matches: List<Contact>,
    onDigitPress: (String) -> Unit,
    onBackspacePress: () -> Unit,
    onClearAllPress: () -> Unit,
    onCallPress: (String) -> Unit,
    onAddContactPress: (String) -> Unit,
    onContactSelect: (Contact) -> Unit,
    onBiometricUnlock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag("keypad_tab")
    ) {
        // T9 Predictive Matches Row (shows matching contacts as user types digits)
        AnimatedVisibility(
            visible = dialedNumber.isNotBlank() && t9Matches.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.height(72.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("t9_matches_row")
            ) {
                items(t9Matches, key = { it.id }) { contact ->
                    T9MatchChip(
                        contact = contact,
                        onClick = { onContactSelect(contact) },
                        onCall = { onCallPress(contact.phoneNumber) }
                    )
                }
            }
        }

        if (dialedNumber.isBlank() || t9Matches.isEmpty()) {
            Spacer(modifier = Modifier.height(72.dp))
        }

        // Add to Contacts prompt (if dialed number is not blank and not exactly matching)
        AnimatedVisibility(
            visible = dialedNumber.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AssistChip(
                onClick = { onAddContactPress(dialedNumber) },
                label = { Text("Add to Contacts", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = ElectricCyan
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_dialed_to_contacts")
            )
        }

        // Dialed Number Display
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = dialedNumber.ifEmpty { "" },
                fontSize = if (dialedNumber.length > 14) 26.sp else 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("dialed_number_display")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3x4 Tactile Keypad
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            KeypadRows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { key ->
                        KeypadButton(
                            key = key,
                            onClick = { onDigitPress(key.digit) },
                            onLongClick = {
                                if (key.digit == "0") {
                                    onDigitPress("+")
                                } else if (key.digit == "1") {
                                    // Voicemail speed dial
                                    onCallPress("1")
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Actions: Spacer/Empty | Call Button | Backspace Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Biometric Vault Unlock Button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onBiometricUnlock() }
                    .testTag("biometric_unlock_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Unlock Vault with Biometrics",
                    tint = ElectricCyan,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Call Button (Glowing Emerald)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(CallGreen, VibrantGreen)
                        )
                    )
                    .clickable {
                        if (dialedNumber.isNotBlank()) {
                            onCallPress(dialedNumber)
                        }
                    }
                    .testTag("keypad_call_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Make Phone Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Backspace Button
            Box(
                modifier = Modifier.size(68.dp),
                contentAlignment = Alignment.Center
            ) {
                if (dialedNumber.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = onBackspacePress,
                                onLongClick = onClearAllPress
                            )
                            .testTag("keypad_backspace_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadButton(
    key: KeypadKey,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("keypad_btn_${key.digit}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = key.digit,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 30.sp
            )
            if (key.letters.isNotBlank()) {
                Text(
                    text = key.letters,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun T9MatchChip(
    contact: Contact,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("t9_match_${contact.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            ContactAvatar(contact = contact, size = 36.dp)

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = onCall,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = CallGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
