package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Contact
import com.example.ui.theme.AvatarColors

@Composable
fun ContactAvatar(
    contact: Contact,
    size: Dp = 48.dp,
    showFavoriteBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val baseColor = AvatarColors[contact.avatarColorIndex % AvatarColors.size]
    val darkerColor = baseColor.copy(alpha = 0.85f)
    val textSize = (size.value * 0.4f).sp

    Box(
        modifier = modifier
            .size(size)
            .testTag("contact_avatar_${contact.id}"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(baseColor, darkerColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.initials,
                color = Color.White,
                fontSize = textSize,
                fontWeight = FontWeight.Bold
            )
        }

        if (showFavoriteBadge && contact.isFavorite) {
            Box(
                modifier = Modifier
                    .size(size * 0.35f)
                    .offset(x = size * 0.35f, y = size * 0.35f)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.22f)
                )
            }
        }
    }
}

@Composable
fun InitialsAvatar(
    name: String?,
    size: Dp = 48.dp,
    colorIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val initials = when {
        name.isNullOrBlank() -> "?"
        name.trim().split(" ").size > 1 -> {
            val parts = name.trim().split(" ").filter { it.isNotBlank() }
            "${parts[0].take(1)}${parts[parts.lastIndex].take(1)}".uppercase()
        }
        else -> name.take(1).uppercase()
    }
    val baseColor = AvatarColors[colorIndex % AvatarColors.size]
    val textSize = (size.value * 0.4f).sp

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(baseColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = textSize,
            fontWeight = FontWeight.Bold
        )
    }
}
