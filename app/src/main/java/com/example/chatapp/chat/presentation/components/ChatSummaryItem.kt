package com.example.chatapp.chat.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.chatapp.chat.presentation.home.ChatSummaryUi
import com.example.chatapp.core.domain.util.formatDate

@Composable
fun ChatSummaryItem(
    chat: ChatSummaryUi,
    onClick: () -> Unit
) {
    if (chat.isDeleting) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    } else {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RectangleShape,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatDate(chat.updatedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}