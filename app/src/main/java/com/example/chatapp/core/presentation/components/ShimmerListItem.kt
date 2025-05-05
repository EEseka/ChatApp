package com.example.chatapp.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.chatapp.ui.theme.ChatAppTheme

@Composable
fun ShimmerListItem(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(20.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .shimmerEffect()
            )
        }
    }
}

@PreviewLightDark
@Composable
fun ShimmerListItemPreview() {
    ChatAppTheme {
        ShimmerListItem()
    }
}