package com.example.ui.tabs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Contact
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.CallGreen
import com.example.ui.theme.ElectricCyan

@Composable
fun ContactsTab(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onCallClick: (String, Contact) -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContactClick,
                containerColor = ElectricCyan,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .testTag("add_contact_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Contact",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (contacts.isEmpty()) {
            ContactsEmptyState(onAddContact = onAddContactClick)
        } else {
            // Group contacts by first letter
            val grouped = contacts.groupBy {
                it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            }

            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("contacts_list")
            ) {
                grouped.forEach { (letter, contactList) ->
                    item(key = "header_$letter") {
                        ContactSectionHeader(letter = letter)
                    }

                    items(contactList, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            onClick = { onContactClick(contact) },
                            onCallClick = { onCallClick(contact.phoneNumber, contact) },
                            onToggleFavorite = { onToggleFavorite(contact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactSectionHeader(letter: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ElectricCyan,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("contact_item_${contact.id}")
    ) {
        ContactAvatar(
            contact = contact,
            size = 46.dp,
            showFavoriteBadge = false
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${contact.label} • ${contact.phoneNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Favorite Toggle
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(36.dp)
                .testTag("contact_fav_btn_${contact.id}")
        ) {
            Icon(
                imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = if (contact.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (contact.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Quick Call
        IconButton(
            onClick = onCallClick,
            modifier = Modifier
                .size(36.dp)
                .testTag("contact_call_btn_${contact.id}")
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CallGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call ${contact.name}",
                    tint = CallGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactsEmptyState(onAddContact: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("contacts_empty_state")
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ElectricCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PersonSearch,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Contacts Found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Try a different search or add a new contact.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
