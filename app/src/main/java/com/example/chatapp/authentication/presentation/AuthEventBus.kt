package com.example.chatapp.authentication.presentation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class AuthEventBus {
    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    suspend fun send(event: AuthEvent) {
        _events.send(event)
    }
}