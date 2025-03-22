package com.example.chatapp.authentication.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.chatapp.R

@Composable
fun PasswordInputField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    passwordNotEmpty: Boolean,
    onVisibilityIconClicked: () -> Unit,
    passwordVisible: Boolean,
    isError: Boolean,
    @StringRes passwordError: Int?,
    passwordFocusRequester: FocusRequester?,
    imeAction: ImeAction,
    onKeyboardNextClicked: () -> Unit = {},
    onKeyboardDoneClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it) },
        label = {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (passwordNotEmpty) {
                IconButton(onClick = onVisibilityIconClicked) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) {
                            stringResource(R.string.hide_password)
                        } else {
                            stringResource(R.string.show_password)
                        }
                    )
                }
            }
        },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        isError = isError,
        supportingText = {
            passwordError?.let {
                Text(
                    text = stringResource(it),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = passwordFocusRequester?.let {
            modifier
                .fillMaxWidth()
                .focusRequester(it)
        } ?: modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction,
            keyboardType = KeyboardType.Password
        ),
        keyboardActions = when (imeAction) {
            ImeAction.Done -> KeyboardActions(onDone = { onKeyboardDoneClicked() })
            ImeAction.Next -> KeyboardActions(onNext = { onKeyboardNextClicked() })
            else -> KeyboardActions()
        },
        singleLine = true
    )
}