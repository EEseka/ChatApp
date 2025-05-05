package com.example.chatapp.chat.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.example.chatapp.R
import com.example.chatapp.ui.theme.ChatAppTheme

@Composable
fun EditableField(
    label: String,
    value: TextFieldValue,
    isLoading: Boolean,
    isError: Boolean,
    @StringRes supportingText: Int?,
    onValueChange: (TextFieldValue) -> Unit,
    isEditing: Boolean,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isEditing) {
        TextField(
            value = value,
            label = { Text(text = label) },
            onValueChange = { onValueChange(it) },
            isError = isError,
            supportingText = {
                supportingText?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSaveClick
                    focusManager.clearFocus()
                }
            ),
            trailingIcon = {
                IconButton(onClick = onSaveClick) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.save)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEditClick, enabled = !isLoading) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.edit)
                )
            }
        }
    }
}

@PreviewLightDark
@Preview(showBackground = true)
@Composable
fun EditableFieldPreview() {
    ChatAppTheme {
        EditableField(
            value = TextFieldValue("John Doe"),
            label = "Name",
            isError = false,
            supportingText = null,
            onValueChange = {},
            isEditing = false,
            focusRequester = FocusRequester(),
            focusManager = LocalFocusManager.current,
            onEditClick = {},
            onSaveClick = {},
            isLoading = false
        )
    }
}
