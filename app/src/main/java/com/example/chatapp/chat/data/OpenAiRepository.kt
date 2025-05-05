package com.example.chatapp.chat.data

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.aallam.openai.api.audio.AudioResponseFormat
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.exception.AuthenticationException
import com.aallam.openai.api.exception.InvalidRequestException
import com.aallam.openai.api.exception.PermissionException
import com.aallam.openai.api.exception.RateLimitException
import com.aallam.openai.api.exception.UnknownAPIException
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.chatapp.R
import com.example.chatapp.chat.data.local.TrendingSearchDao
import com.example.chatapp.chat.data.local.mappers.toTrendingSearch
import com.example.chatapp.chat.data.local.mappers.toTrendingSearchEntity
import com.example.chatapp.chat.data.mappers.toChatRole
import com.example.chatapp.chat.domain.AiDataSource
import com.example.chatapp.chat.domain.models.ChatPreference
import com.example.chatapp.chat.domain.models.Message
import com.example.chatapp.core.data.openai.safeOpenAiNetworkCall
import com.example.chatapp.core.domain.util.NetworkError
import com.example.chatapp.core.domain.util.Result
import com.example.chatapp.core.domain.util.map
import com.example.chatapp.core.domain.util.onError
import com.example.chatapp.core.domain.util.onSuccess
import com.example.chatapp.core.domain.utils.FileManager
import com.example.chatapp.core.domain.utils.ImageCompressor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.io.files.Path

class OpenAiRepository(
    private val openAI: OpenAI,
    private val dao: TrendingSearchDao,
    private val fileManager: FileManager,
    private val imageCompressor: ImageCompressor,
    private val context: Context
) : AiDataSource {
    override suspend fun chatWithAi(
        prompt: List<Message>,
        preference: ChatPreference
    ): Result<Flow<String>, NetworkError> = try {
        val initialMessages = buildMessages(prompt, preference)
        val image = prompt.lastOrNull()?.image
        if (!image.isNullOrBlank()) {
            initialMessages.add(
                ChatMessage(
                    role = ChatRole.User,
                    content = "Please use the context from this image: $image"
                )
            )
        }
        val modelId = when {
            preference.isReasoningEnabled && preference.isOnlineSearchEnabled -> "gpt-4-turbo"
            preference.isReasoningEnabled -> "gpt-4.1-mini"
            preference.isOnlineSearchEnabled -> "gpt-4o-mini-search-preview"
            else -> "gpt-3.5-turbo"
        }

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(modelId),
            messages = initialMessages.toList()
        )

        safeOpenAiNetworkCall {
            openAI.chatCompletions(chatCompletionRequest)
                .map { it.choices.first().delta?.content.orEmpty() }
        }

    } catch (e: RateLimitException) {
        Log.e(TAG, "RateLimitException: ${e.message}")
        Result.Success(flowOf(context.getString(R.string.friendly_rate_limit_message)))
    } catch (e: InvalidRequestException) {
        Log.e(TAG, "InvalidRequestException: ${e.error.detail?.message ?: e.message}")
        Result.Success(flowOf(context.getString(R.string.friendly_invalid_request_message)))
    } catch (e: AuthenticationException) {
        Log.e(TAG, "AuthenticationException: ${e.message}")
        Result.Success(flowOf(context.getString(R.string.friendly_service_error)))
    } catch (e: PermissionException) {
        Log.e(TAG, "PermissionException: ${e.message}")
        Result.Success(flowOf(context.getString(R.string.friendly_service_error)))
    } catch (e: UnknownAPIException) {
        Log.e(TAG, "UnknownAPIException: ${e.message}")
        Result.Success(flowOf(context.getString(R.string.friendly_service_error)))
    } catch (e: Exception) {
        Log.e(TAG, "Exception: ${e.message}")
        Result.Success(flowOf(context.getString(R.string.error_unknown)))
    }

    override suspend fun generateImage(prompt: String): Result<List<String>, NetworkError> = try {
        safeOpenAiNetworkCall {
            openAI.imageURL(
                creation = ImageCreation(
                    prompt = prompt,
                    model = ModelId("dall-e-3"),
                    n = 1,
                    size = ImageSize.is1024x1024
                )
            )
        }.map { images ->
            var downloadedImageUri: String? = null
            fileManager.downloadImageFromUrl(images.first().url)?.let { byteArray ->
                val uri = fileManager.saveImageToCache(
                    byteArray,
                    "generated_image_${System.currentTimeMillis()}.png"
                )
                downloadedImageUri = uri.toString()
            }
            downloadedImageUri?.let {
                imageCompressor.compressImage(it.toUri(), MAX_IMAGE_SIZE)
                    .onSuccess { compressedUri ->
                        downloadedImageUri = fileManager.saveImageToCache(
                            compressedUri,
                            "compressed_generated_image_${System.currentTimeMillis()}.png"
                        ).toString()
                    }
                    .onError { error ->
                        Log.e(TAG, "Error compressing image: $error")
                    }
            }
            downloadedImageUri?.let {
                listOf(it)
            } ?: emptyList()
        }
    } catch (e: RateLimitException) {
        Log.e(TAG, "RateLimitException: ${e.message}")
        Result.Success(emptyList())
    } catch (e: InvalidRequestException) {
        Log.e(TAG, "InvalidRequestException: ${e.error.detail?.message}")
        Result.Success(emptyList())
    } catch (e: AuthenticationException) {
        Log.e(TAG, "AuthenticationException: ${e.message}")
        Result.Success(emptyList())
    } catch (e: PermissionException) {
        Log.e(TAG, "PermissionException: ${e.message}")
        Result.Success(emptyList())
    } catch (e: UnknownAPIException) {
        Log.e(TAG, "UnknownAPIException: ${e.message}")
        Result.Success(emptyList())
    } catch (e: Exception) {
        Log.e(TAG, "Exception: ${e.message}")
        Result.Success(emptyList())
    }

    override suspend fun transcribeAudio(
        audioPath: String,
        language: String,
        responseFormat: AudioResponseFormat
    ): Result<String, NetworkError> = try {
        val audioFileSource = FileSource(path = Path(audioPath))
        val transcriptionRequest = TranscriptionRequest(
            audio = audioFileSource,
            model = ModelId("whisper-1"),
            language = language,
            responseFormat = responseFormat
        )
        safeOpenAiNetworkCall {
            openAI.transcription(transcriptionRequest)
        }.map { transcription ->
            transcription.text
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception: ${e.message}")
        Result.Success("")
    }

    override suspend fun getTrendingSearches(): Result<List<String>, NetworkError> {
        try {
            // Check if trending searches are already cached
            val cachedSearches = dao.getTrendingSearches()
            if (cachedSearches != null && cachedSearches.toTrendingSearch().isNotEmpty()) {
                val currentTime = System.currentTimeMillis()
                // Check if it's been more than a day since the last update
                if (currentTime - cachedSearches.timestamp > ONE_DAY_IN_MILLIS) {
                    return fetchAndUpdateTrendingSearches()
                }
                return Result.Success(cachedSearches.toTrendingSearch())
            }
            // If not cached, fetch from OpenAI
            return fetchAndUpdateTrendingSearches()
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
            return Result.Success(emptyList())
        }
    }

    override suspend fun generateChatTitle(chatContent: String): Result<String, NetworkError> =
        try {
            val messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "You are an assistant tasked with summarizing chat content and generating concise, descriptive titles."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = "Create a short, descriptive title for the following chat content. Do not include quotes, punctuation, or any extra formatting: $chatContent"
                )
            )
            val chatCompletionRequest = ChatCompletionRequest(
                model = ModelId("gpt-4-turbo"),
                messages = messages
            )
            safeOpenAiNetworkCall {
                openAI.chatCompletion(chatCompletionRequest)
            }.map { chatCompletion ->
                chatCompletion.choices.firstOrNull()?.message?.content.orEmpty().trim()
                    .removePrefix("\"").removeSuffix("\"")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
            Result.Error(NetworkError.UNKNOWN)
        }

    private suspend fun fetchAndUpdateTrendingSearches(): Result<List<String>, NetworkError> = try {
        val messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = "You are a real-time trends analyst specializing in current events, viral topics, and breaking news. Focus on what people are actively discussing, sharing, and searching about right now across social media, news, and popular culture."
            ),
            ChatMessage(
                role = ChatRole.User,
                content = "List 10 currently trending topics that people are actively discussing today. Include viral stories, current events, breaking news, and popular culture moments. Exclude generic terms like app names, company names, or everyday searches. Provide only specific, timely topics in concise phrases without numbering or extra text."
            )
        )
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-4o-mini-search-preview"),
            messages = messages
        )
        safeOpenAiNetworkCall {
            openAI.chatCompletion(chatCompletionRequest)
        }.map { chatCompletion ->
            val content = chatCompletion.choices.firstOrNull()?.message?.content.orEmpty()
            val formattedContent = content.lines()
                .map {
                    it.trim().removePrefix("-").removePrefix("•").removePrefix("\"")
                        .removeSuffix("\"")
                        .replaceFirst(Regex("^\\d+\\.\\s*"), "") // Remove numbering
                }
                .filter { it.isNotBlank() }
                .take(10)
            Log.d(TAG, "Trending searches fetched and cached: $formattedContent")

            dao.clearTrendingSearches()
            dao.insertTrendingSearches(formattedContent.toTrendingSearchEntity())
            dao.getTrendingSearches()?.toTrendingSearch() ?: emptyList()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception: ${e.message}")
        Result.Success(
            listOf(
                "Technology trends",
                "AI advancements",
                "Global news",
                "Health tips",
                "Finance markets"
            )
        )
    }

    // Helper function to build chat messages for a request.
    private fun buildMessages(
        messages: List<Message>,
        prefs: ChatPreference
    ): MutableList<ChatMessage> {
        val moodPrompt = prefs.mood.message

        val reasoningAddon = if (prefs.isReasoningEnabled)
            "Reason using logic and evidence where appropriate."
        else ""

        val searchAddon = if (prefs.isOnlineSearchEnabled)
            "You are allowed to reference online context."
        else ""

        val updatedMessages = mutableListOf(
            ChatMessage(
                role = ChatRole.System,
                content = "$moodPrompt$reasoningAddon$searchAddon"
            )
        )
        // Convert the provided list of Message objects to ChatMessage
        updatedMessages.addAll(
            messages.map { message ->
                ChatMessage(
                    role = message.role.toChatRole(),
                    content = message.content
                )
            }
        )
        return updatedMessages
    }

    companion object {
        private const val TAG = "OpenAiRepository"
        private const val ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
        private const val MAX_IMAGE_SIZE = 1024 * 1024L // 1MB
    }
}


