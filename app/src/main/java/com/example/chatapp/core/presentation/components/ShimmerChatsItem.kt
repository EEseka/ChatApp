package com.example.chatapp.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.chatapp.ui.theme.ChatAppTheme

@Composable
fun ShimmerChatsItem(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(60.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .shimmerEffect()
                    .align(Alignment.CenterEnd)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(70.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .shimmerEffect()
                    .align(Alignment.CenterStart)
            )
        }
    }
}

@PreviewLightDark
@Composable
fun ShimmerChatsItemPreview() {
    ChatAppTheme {
        ShimmerChatsItem()
    }
}