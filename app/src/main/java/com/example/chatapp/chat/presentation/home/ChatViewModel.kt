package com.example.chatapp.chat.presentation.home

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.R
import com.example.chatapp.chat.domain.AiDataSource
import com.example.chatapp.chat.domain.ChatDatabase
import com.example.chatapp.chat.domain.models.Chat
import com.example.chatapp.chat.domain.models.Message
import com.example.chatapp.chat.domain.models.Role
import com.example.chatapp.chat.presentation.MainEvent
import com.example.chatapp.chat.presentation.MainEventBus
import com.example.chatapp.core.domain.util.onError
import com.example.chatapp.core.domain.util.onSuccess
import com.example.chatapp.core.domain.utils.AudioRecorder
import com.example.chatapp.core.domain.utils.FileManager
import com.example.chatapp.core.domain.utils.ImageCompressor
import com.example.chatapp.core.presentation.util.toStringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatViewModel(
    private val aiDataSource: AiDataSource,
    private val chatDatabase: ChatDatabase,
    private val fileManager: FileManager,
    private val imageCompressor: ImageCompressor,
    private val audioRecorder: AudioRecorder,
    private val mainEventBus: MainEventBus,
    private val appContext: Context
) : ViewModel() {
    private val name = chatDatabase.user?.displayName ?: ""
    private val photoUri = chatDatabase.user?.photoUrl

    private val _state = MutableStateFlow(ChatListState(name = name, profilePictureUri = photoUri))
    val state = _state
        .onStart {
            getAllChats()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            ChatListState(name = name, profilePictureUri = photoUri)
        )

    fun onEvent(event: ChatEvents) {
        when (event) {
            is ChatEvents.OnDeleteChat -> deleteChat(event.chatId)
            is ChatEvents.OnChatSelected -> {
                loadChat(event.chatId)
                loadTrendingTopics()
            }

            ChatEvents.OnCreateNewChat -> createNewChat()
            ChatEvents.OnMessageSent -> sendMessage(_state.value.selectedChat?.currentInput!!)
            is ChatEvents.OnEditedMessageSent -> sendMessage(
                _state.value.selectedChat?.currentInput!!,
                event.messageId
            )

            is ChatEvents.OnInputChanged -> {
                _state.update { it.copy(selectedChat = it.selectedChat?.copy(currentInput = event.input)) }
            }

            is ChatEvents.OnImageSelected -> selectImage(event.uri, event.extension)
            ChatEvents.OnRemoveImage -> {
                _state.update { it.copy(selectedChat = it.selectedChat?.copy(selectedImageUri = null)) }
            }

            is ChatEvents.OnTrendingTopicSelected -> {
                _state.update { it.copy(selectedChat = it.selectedChat?.copy(currentInput = event.topic)) }
                sendMessage(event.topic, isTrending = true)
            }

            is ChatEvents.OnChatTitleChanged -> {
                _state.update {
                    it.copy(
                        selectedChat = it.selectedChat?.copy(
                            chat = it.selectedChat.chat.copy(
                                title = event.title
                            )
                        )
                    )
                }
            }

            ChatEvents.OnSaveTitleEdit -> saveChat()
            ChatEvents.OnToggleReasoning -> {
                _state.update {
                    it.copy(
                        selectedChat = it.selectedChat?.copy(
                            chatPreference = it.selectedChat.chatPreference.copy(
                                isReasoningEnabled = !it.selectedChat.chatPreference.isReasoningEnabled
                            )
                        )
                    )
                }
            }

            is ChatEvents.OnMoodSelected -> {
                _state.update {
                    it.copy(
                        selectedChat = it.selectedChat?.copy(
                            chatPreference = it.selectedChat.chatPreference.copy(
                                mood = event.mood
                            )
                        )
                    )
                }
            }

            ChatEvents.OnToggleOnlineSearch -> {
                _state.update {
                    it.copy(
                        selectedChat = it.selectedChat?.copy(
                            chatPreference = it.selectedChat.chatPreference.copy(
                                isOnlineSearchEnabled = !it.selectedChat.chatPreference.isOnlineSearchEnabled
                            )
                        )
                    )
                }
            }

            ChatEvents.OnToggleImageGeneration -> {
                _state.update {
                    it.copy(
                        selectedChat = it.selectedChat?.copy(
                            shouldGenerateImage = !it.selectedChat.shouldGenerateImage
                        )
                    )
                }
            }

            is ChatEvents.OnEditMessage -> editMessage(event.messageId)
            ChatEvents.OnStartAudioRecording -> startRecording()
            ChatEvents.OnStopAudioRecording -> stopRecording()
            ChatEvents.OnCancelAudioRecording -> stopRecording(true)
        }
    }

    private fun createNewChat() {
        val newChat = Chat(
            id = UUID.randomUUID().toString(),
            title = "${appContext.getString(R.string.new_chat)} ${
                SimpleDateFormat(
                    "MMM d, HH:mm",
                    Locale.getDefault()
                ).format(Date())
            }",
            messages = emptyList(),
            createdAt = System.currentTimeMillis()
        )
        _state.update {
            it.copy(selectedChat = ChatState(chat = newChat))
        }
        loadTrendingTopics()
    }

    private fun loadChat(chatId: String) {
        viewModelScope.launch {
            _state.update { it.copy(selectedChat = ChatState(isChatLoading = true)) }
            chatDatabase.getChatById(chatId)
                .onSuccess { chat ->
                    _state.update {
                        it.copy(
                            selectedChat = it.selectedChat?.copy(
                                chat = chat,
                                isChatLoading = false
                            ),
                        )
                    }
                }
                .onError { error ->
                    _state.update {
                        it.copy(
                            selectedChat = it.selectedChat?.copy(isChatLoading = false)
                        )
                    }
                    mainEventBus.send(MainEvent.DatabaseError(error))
                }
        }
    }

    private fun sendMessage(
        content: String,
        editedMessageId: String? = null,
        isTrending: Boolean = false
    ) {
        val selectedImage = _state.value.selectedChat?.selectedImageUri
        if (content.isBlank() && selectedImage == null) return
        if (content.length > MAX_MESSAGE_LENGTH) {
            _state.update {
                it.copy(
                    selectedChat = it.selectedChat?.copy(
                        messageLengthError = R.string.message_too_long
                    )
                )
            }
            return
        }

        // Check if the message already exists and remove it if it does as we are editing
        editedMessageId?.let {
            checkIfMessageExistsAndRemoveIt(it)
        }

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            content = content.trim(),
            image = selectedImage?.toString(),
            role = Role.USER,
            timestamp = System.currentTimeMillis()
        )

        _state.update {
            it.copy(
                selectedChat = it.selectedChat?.copy(
                    chat = it.selectedChat.chat.copy(messages = it.selectedChat.chat.messages + userMessage),
                    currentInput = "",
                    selectedImageUri = null,
                    messageLengthError = null,
                    imageCompressionError = null
                )
            )
        }

        viewModelScope.launch {
            if (state.value.selectedChat?.shouldGenerateImage == true) {
                generateImage(content)
            } else {
                generateChatText(userMessage, isTrending)
            }
        }
    }

    private fun checkIfMessageExistsAndRemoveIt(id: String) {
        val currentMessages = _state.value.selectedChat?.chat?.messages?.toMutableList() ?: return
        val existingUserMessage = currentMessages.find { it.role == Role.USER && it.id == id }

        if (existingUserMessage != null) {
            val userMessageIndex = currentMessages.indexOf(existingUserMessage)

            // Remove user message
            currentMessages.removeAt(userMessageIndex)

            // After removal, the AI response (if it exists) will now be at the same index
            if (userMessageIndex < currentMessages.size &&
                currentMessages[userMessageIndex].role == Role.ASSISTANT
            ) {
                currentMessages.removeAt(userMessageIndex)  // Use same index!
            }

            _state.update {
                it.copy(
                    selectedChat = it.selectedChat?.copy(
                        chat = it.selectedChat.chat.copy(messages = currentMessages)
                    )
                )
            }
        }
    }

    private fun selectImage(uri: Uri, extension: String) {
        viewModelScope.launch {
            _state.update { it.copy(selectedChat = it.selectedChat?.copy(isSelectingImage = true)) }

            imageCompressor.compressImage(uri, MAX_IMAGE_SIZE)
                .onSuccess { byteArray ->
                    val compressedUri = fileManager.saveImageToCache(
                        byteArray,
                        "chat_image_${System.currentTimeMillis()}.$extension"
                    )
                    _state.update {
                        it.copy(
                            selectedChat = it.selectedChat?.copy(
                                selectedImageUri = compressedUri,
                                isSelectingImage = false
                            )
                        )
                    }
                }
                .onError { error ->
                    _state.update {
                        it.copy(
                            selectedChat = it.selectedChat?.copy(
                                imageCompressionError = error.toStringRes(),
                                isSelectingImage = false
                            )
                        )
                    }
                }
        }
    }

    private fun loadTrendingTopics() {
        viewModelScope.launch {
            _state.update { it.copy(selectedChat = it.selectedChat?.copy(isTrendingLoading = true)) }

            aiDataSource.getTrendingSearches()
                .onSuccess { topics ->
                    if (topics.isEmpty()) {
                        _state.update {
                            it.copy(
                                selectedChat = it.selectedChat?.copy(
                                    trendingTopics = listOf(
                                        "Technology trends",
                                        "AI advancements",
                                        "Global news",
                                        "Health tips",
                                        "Finance markets"
                                    ),
                                    isTrendingLoading = false
                                )
                            )
                        }
                        return@onSuccess
                    }
                    _state.update {
                        it.copy(
                            selectedChat = it.selectedChat?.copy(
                                trendingTopics = topics,
                                isTrendingLoading = false
                            )
                        )
                    }
                }
                .onError { error ->
                    _state.update { it.copy(selectedChat = it.selectedChat?.copy(isTrendingLoading = false)) }
                    mainEventBus.send(MainEvent.NetworkingError(error))
                    Log.e(TAG, "Error loading trending topics: $error")
                }
        }
    }

    private fun saveChat() {
        val chat = _state.value.selectedChat?.chat!!

        viewModelScope.launch {
            chatDatabase.saveChat(chat)
                .onSuccess {
                    Log.i(TAG, "Chat saved successfully")
                    getAllChats()
                }
                .onError { error ->
                    Log.e(TAG, "Error saving chat: $error")
                    mainEventBus.send(MainEvent.DatabaseError(error))
                }
        }
    }

    private fun generateChatText(userMessage: Message, isTrending: Boolean = false) {
        _state.update { it.copy(selectedChat = it.selectedChat?.copy(isTyping = true)) }

        val responseBuilder = StringBuilder()
        val currentMessages = _state.value.selectedChat?.chat?.messages!!
        val prefs =
            if (isTrending) _state.value.selectedChat?.chatPreference?.copy(isOnlineSearchEnabled = true)!!
            else _state.value.selectedChat?.chatPreference!!

        // Create a temporary streaming message ID once
        val streamingMessageId = UUID.randomUUID().toString()

        viewModelScope.launch {
            aiDataSource.chatWithAi(currentMessages, prefs)
                .onSuccess { flow ->
                    flow.collect { response ->
                        if (response.isNotEmpty()) {
                            responseBuilder.append(response)
                            _state.update { currentState ->
                                currentState.copy(
                                    selectedChat = currentState.selectedChat?.copy(
                                        chat = currentState.selectedChat.chat.copy(
                                            // Filter out previous streaming message and add updated one
                                            messages = currentState.selectedChat.chat.messages
                                                .filter { it.id != streamingMessageId } +
                                                    Message(
                                                        id = streamingMessageId,
                                                        content = responseBuilder.toString(),
                                                        image = null,
                                                        role = Role.ASSISTANT,
                                                        timestamp = System.currentTimeMillis()
                                                    )
                                        ),
                                        isTyping = true
                                    )
                                )
                            }
                        }
                    }

                    // Flow collection is complete, update with final message
                    _state.update { currentState ->
                        currentState.copy(
                            selectedChat = currentState.selectedChat?.copy(
                                chat = currentState.selectedChat.chat.copy(
                                    messages = currentState.selectedChat.chat.messages
                                        .filter { it.id != streamingMessageId } +
                                            Message(
                                                id = streamingMessageId,
                                                content = responseBuilder.toString(),
                                                image = null,
                                                role = Role.ASSISTANT,
                                                timestamp = System.currentTimeMillis()
                                            )
                                ),
                                isTyping = false
                            )
                        )
                    }
                    saveChat()
                    // If this is a new chat with 2 messages (user + AI), generate title
                    val shouldGenerateTitle =
                        _state.value.selectedChat?.chat?.messages?.count { it.role == Role.USER || it.role == Role.ASSISTANT } == 2
                                && _state.value.selectedChat?.chat?.title?.startsWith(
                            appContext.getString(
                                R.string.new_chat
                            )
                        ) == true
                    if (shouldGenerateTitle) generateChatTitle(userMessage.content)
                }.onError { error ->
                    _state.update { it.copy(selectedChat = it.selectedChat?.copy(isTyping = false)) }
                    editMessage(_state.value.selectedChat?.chat?.messages?.last()?.id!!)
                    checkIfMessageExistsAndRemoveIt(_state.value.selectedChat?.chat?.messages?.last()?.id!!)
                    mainEventBus.send(MainEvent.NetworkingError(error))
                }
        }
    }

    private fun generateImage(prompt: String) {
        _state.update { it.copy(selectedChat = it.selectedChat?.copy(isTyping = true)) }

        viewModelScope.launch {
            aiDataSource.generateImage(prompt)
                .onSuccess { images ->
                    val messages = images.map { image ->
                        Message(
                            id = UUID.randomUUID().toString(),
                            content = "",
                            image = image,
                            role = Role.ASSISTANT,
                            timestamp = System.currentTimeMillis()
                        )
                    }.ifEmpty {
                        listOf(
                            Message(
                                id = UUID.randomUUID().toString(),
                                content = appContext.getString(R.string.image_generation_failed),
                                image = null,
                                role = Role.ASSISTANT,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    _state.update {
                        it.copy(
                            selectedChat = it.selectedChat?.copy(
                                chat = it.selectedChat.chat.copy(messages = it.selectedChat.chat.messages + messages),
                                isTyping = false
                            )
                        )
                    }
                    saveChat()
                }.onError { error ->
                    _state.update { it.copy(selectedChat = it.selectedChat?.copy(isTyping = false)) }
                    editMessage(_state.value.selectedChat?.chat?.messages?.last()?.id!!)
                    checkIfMessageExistsAndRemoveIt(_state.value.selectedChat?.chat?.messages?.last()?.id!!)
                    mainEventBus.send(MainEvent.NetworkingError(error))
                }
        }
    }

    private fun transcribeAudio(audioPath: String) {
        _state.update { it.copy(selectedChat = it.selectedChat?.copy(isTranscriptionLoading = true)) }

        viewModelScope.launch {
            aiDataSource.transcribeAudio(audioPath)
                .onSuccess { transcription ->
                    if (transcription.isBlank()) {
                        Log.e(TAG, "Transcription is empty")
                        _state.update {
                            it.copy(
                                selectedChat = it.selectedChat?.copy(
                                    isAudioRecording = false,
                                    isTranscriptionLoading = false
                                )
                            )
                        }
                        return@onSuccess
                    }
                    _state.update {
                        it.copy(
                            selectedChat = it.selectedChat?.copy(
                                currentInput = transcription.trim(),
                                isAudioRecording = false,
                                isTranscriptionLoading = false
                            )
                        )
                    }
                }.onError { error ->
                    _state.update {
                        it.copy(
                            selectedChat = it.selectedChat?.copy(
                                isAudioRecording = false,
                                isTranscriptionLoading = false
                            )
                        )
                    }
                    mainEventBus.send(MainEvent.NetworkingError(error))
                }
        }
    }

    private fun editMessage(messageId: String) {
        val messageToEdit = _state.value.selectedChat?.chat?.messages?.find { it.id == messageId }
        messageToEdit?.let { message ->
            _state.update {
                it.copy(
                    selectedChat = it.selectedChat?.copy(
                        currentInput = message.content,
                        selectedImageUri = message.image?.toUri()
                    )
                )
            }
        }
    }

    private fun getAllChats() {
        viewModelScope.launch {
            _state.update { it.copy(isChatListLoading = true) }
            chatDatabase.getAllChats()
                .onSuccess { chats ->
                    _state.update { it.copy(chats = chats, isChatListLoading = false) }
                }
                .onError { error ->
                    _state.update { it.copy(isChatListLoading = false) }
                    mainEventBus.send(MainEvent.DatabaseError(error))
                }
        }
    }

    private fun deleteChat(chatId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isChatDeleting = true) }
            val shouldMakeSelectedChatNull = _state.value.selectedChat?.chat?.id == chatId
            chatDatabase.deleteChat(chatId)
                .onSuccess {
                    _state.update { it.copy(isChatDeleting = false) }
                    if (shouldMakeSelectedChatNull) {
                        _state.update { it.copy(selectedChat = null) }
                    }
                    getAllChats()
                }
                .onError { error ->
                    _state.update { it.copy(isChatDeleting = false) }
                    mainEventBus.send(MainEvent.DatabaseError(error))
                }
        }
    }

    private fun generateChatTitle(userMessage: String) {
        viewModelScope.launch {
            aiDataSource.generateChatTitle(userMessage)
                .onSuccess { title ->
                    if (title.isBlank()) return@onSuccess
                    _state.update {
                        it.copy(
                            selectedChat = it.selectedChat?.copy(
                                chat = it.selectedChat.chat.copy(
                                    title = title
                                )
                            )
                        )
                    }
                    saveChat()
                }
                .onError { error ->
                    mainEventBus.send(MainEvent.NetworkingError(error))
                    Log.e(TAG, "Error generating chat title: $error")
                }
        }
    }

    private fun startRecording() {
        viewModelScope.launch {
            val audioFile = fileManager.createFile("prompt-audio.wav") {
                audioRecorder.start(it)
            }
            _state.update {
                it.copy(
                    selectedChat = it.selectedChat?.copy(
                        isAudioRecording = true,
                        audioPath = audioFile?.absolutePath
                    )
                )
            }
        }
    }

    private fun stopRecording(isCancelling: Boolean = false) {
        viewModelScope.launch {
            if (isCancelling) {
                audioRecorder.stop()
                _state.update {
                    it.copy(
                        selectedChat = it.selectedChat?.copy(isAudioRecording = false)
                    )
                }
            } else {
                audioRecorder.stop()
                _state.value.selectedChat?.audioPath?.let {
                    transcribeAudio(it)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ChatViewModel"
        private const val MAX_IMAGE_SIZE = 1024 * 1024L // 1MB
        private const val MAX_MESSAGE_LENGTH = 4096
    }
}