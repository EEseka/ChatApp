package com.example.chatapp.chat.presentation.components

import android.content.ClipData
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chatapp.R
import kotlinx.coroutines.launch

@Composable
fun UserMessageText(
    text: String,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDropdownMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboard.current

    Box {
        Text(
            text = text,
            modifier = modifier
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showDropdownMenu = true }
                )
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )

        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null
                    )
                },
                onClick = {
                    scope.launch {
                        clipboardManager.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText(
                                    "",
                                    text
                                )
                            )
                        )
                    }
                    showDropdownMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )
                },
                onClick = {
                    onEditClick()
                    showDropdownMenu = false
                }
            )
        }
    }
}