package com.example.chatapp.authentication.presentation.signup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.chatapp.R
import com.example.chatapp.authentication.presentation.components.EmailInputField
import com.example.chatapp.authentication.presentation.components.GoogleSignInButton
import com.example.chatapp.authentication.presentation.components.PasswordInputField
import com.example.chatapp.ui.theme.ChatAppTheme

@Composable
fun SignUpScreen(
    state: SignUpState,
    onEmailValueChange: (String) -> Unit,
    onPasswordValueChange: (String) -> Unit,
    onRepeatedPasswordValueChange: (String) -> Unit,
    onSignUpClicked: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onContinueWithGoogleClicked: () -> Unit
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var repeatedPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    // Automatically focus on the first error field
    LaunchedEffect(state.emailError, state.passwordError, state.repeatedPasswordError) {
        when {
            state.emailError != null -> emailFocusRequester.requestFocus()
            state.passwordError != null -> passwordFocusRequester.requestFocus()
            state.repeatedPasswordError != null -> confirmPasswordFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.create_account),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.sign_up_to_continue),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        EmailInputField(
            value = state.email,
            onValueChange = { onEmailValueChange(it) },
            isError = state.emailError != null,
            emailError = state.emailError,
            emailFocusRequester = emailFocusRequester,
            imeAction = ImeAction.Next,
            onKeyboardNextClicked = { passwordFocusRequester.requestFocus() },
        )
        PasswordInputField(
            value = state.password,
            onValueChange = { onPasswordValueChange(it) },
            label = R.string.password,
            passwordNotEmpty = state.password.isNotBlank(),
            onVisibilityIconClicked = { passwordVisible = !passwordVisible },
            passwordVisible = passwordVisible,
            isError = state.passwordError != null,
            passwordError = state.passwordError,
            passwordFocusRequester = passwordFocusRequester,
            imeAction = ImeAction.Next,
            onKeyboardNextClicked = { confirmPasswordFocusRequester.requestFocus() }
        )
        PasswordInputField(
            value = state.repeatedPassword,
            onValueChange = { onRepeatedPasswordValueChange(it) },
            label = R.string.confirm_password,
            passwordNotEmpty = state.repeatedPassword.isNotBlank(),
            onVisibilityIconClicked = { repeatedPasswordVisible = !repeatedPasswordVisible },
            passwordVisible = repeatedPasswordVisible,
            isError = state.repeatedPasswordError != null,
            passwordError = state.repeatedPasswordError,
            passwordFocusRequester = confirmPasswordFocusRequester,
            imeAction = ImeAction.Done,
            onKeyboardDoneClicked = {
                onSignUpClicked()
                focusManager.clearFocus()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSignUpClicked()
                focusManager.clearFocus()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank() &&
                    state.repeatedPassword.isNotBlank()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = stringResource(R.string.sign_up),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign in navigation text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.already_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.sign_in),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable { onNavigateToSignIn() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        GoogleSignInButton(
            onClick = onContinueWithGoogleClicked,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        )
    }
}

@Preview(
    showBackground = true, backgroundColor = 0xFF000000, showSystemUi = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES or android.content.res.Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpScreenPreview() {
    ChatAppTheme {
        SignUpScreen(
            state = SignUpState(),
            onEmailValueChange = {},
            onPasswordValueChange = {},
            onRepeatedPasswordValueChange = {},
            onSignUpClicked = {},
            onNavigateToSignIn = {},
            onContinueWithGoogleClicked = {}
        )
    }
}