package com.example.chatapp.chat.presentation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainEventBus {
    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    suspend fun send(event: MainEvent) {
        _events.send(event)
    }
}