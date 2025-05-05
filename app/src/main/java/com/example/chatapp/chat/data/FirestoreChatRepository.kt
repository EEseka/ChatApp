package com.example.chatapp.chat.data

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.chatapp.chat.data.mappers.toChatSummary
import com.example.chatapp.chat.domain.ChatDatabase
import com.example.chatapp.chat.domain.models.Chat
import com.example.chatapp.chat.domain.models.ChatSummary
import com.example.chatapp.chat.domain.models.Message
import com.example.chatapp.chat.domain.models.Role
import com.example.chatapp.core.data.firebase.utils.safeFirebaseFirestoreCall
import com.example.chatapp.core.data.firebase.utils.safeFirebaseStorageCall
import com.example.chatapp.core.domain.MediaStorage
import com.example.chatapp.core.domain.util.FirebaseFirestoreError
import com.example.chatapp.core.domain.util.FirebaseStorageError
import com.example.chatapp.core.domain.util.Result
import com.example.chatapp.core.domain.util.onError
import com.example.chatapp.core.domain.util.onSuccess
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

class FirestoreChatRepository(
    private val firestore: FirebaseFirestore,
    private val storage: MediaStorage,
    private val auth: FirebaseAuth
) : ChatDatabase {
    // StateFlow for observing the current user state
    private val _currentUserFlow: StateFlow<FirebaseUser?> = callbackFlow {
        val authStateListener = AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(user)

            // This is added to enable the app detect account deletion apart from sign out and sign in
            user?.reload()?.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    trySend(null) // Force emit null if the user is no longer valid
                }
            }
        }

        auth.addAuthStateListener(authStateListener)
        // Ensure we emit the current user immediately
        // For example when there is a new collector
        trySend(auth.currentUser)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        started = SharingStarted.Eagerly,
        initialValue = auth.currentUser
    )
    private val currentUserId: String?
        get() = _currentUserFlow.value?.uid

    override val user: FirebaseUser?
        get() = _currentUserFlow.value

    override suspend fun saveChat(chat: Chat): Result<Unit, FirebaseFirestoreError> {
        if (currentUserId == null) {
            return Result.Error(FirebaseFirestoreError.UNAUTHENTICATED)
        }

        val chatSummary = chat.toChatSummary(updatedAt = System.currentTimeMillis())
        return safeFirebaseFirestoreCall {
            val chatData = mapOf(
                "id" to chat.id,
                "userId" to currentUserId!!,
                "title" to chat.title,
                "createdAt" to chat.createdAt,
                "messages" to chat.messages.map { message ->
                    mapOf(
                        "id" to message.id,
                        "content" to message.content,
                        "image" to getSecureImageUrl(
                            chat.id,
                            message.id,
                            message.image?.toUri()
                        ),
                        "role" to message.role.name,
                        "timestamp" to message.timestamp
                    )
                },
            )
            val chatSummaryData = mapOf(
                "id" to chatSummary.id,
                "userId" to currentUserId!!,
                "title" to chatSummary.title,
                "createdAt" to chatSummary.createdAt,
                "updatedAt" to chatSummary.updatedAt,
            )

            firestore.collection("users")
                .document(currentUserId!!)
                .collection("chats")
                .document(chat.id)
                .set(chatData)
                .await()
            firestore.collection("users")
                .document(currentUserId!!)
                .collection("chatSummaries")
                .document(chatSummary.id)
                .set(chatSummaryData)
                .await()
            Unit
        }
    }

    override suspend fun getChatById(chatId: String): Result<Chat, FirebaseFirestoreError> {
        if (currentUserId == null) {
            return Result.Error(FirebaseFirestoreError.UNAUTHENTICATED)
        }

        val document = firestore.collection("users")
            .document(currentUserId!!)
            .collection("chats")
            .document(chatId)
            .get()
            .await()

        if (!document.exists()) {
            return Result.Error(FirebaseFirestoreError.NOT_FOUND)
        }

        return safeFirebaseFirestoreCall {
            val id = document.getString("id") ?: ""
            val title = document.getString("title") ?: ""
            val createdAt = document.getLong("createdAt") ?: 0L
            val messagesData = document.get("messages") as? List<Map<String, Any>> ?: emptyList()
            val messages = messagesData.map { messageData ->
                val roleName = messageData["role"] as? String ?: ""
                val role = when (roleName) {
                    "SYSTEM" -> Role.SYSTEM
                    "USER" -> Role.USER
                    "ASSISTANT" -> Role.ASSISTANT
                    else -> {}
                } as Role
                val messageId = messageData["id"] as? String ?: ""
                var image = messageData["image"] as? String
                // Try to get the image from storage as it might not be saved properly in Firestore
                if (image == null) {
                    val path =
                        "${CHAT_IMAGE_STORAGE_PATH_START}${currentUserId!!}/$chatId${CHAT_IMAGE_STORAGE_PATH_END}/${messageId}"
                    storage.getPicture(path)
                        .onSuccess { secureUrl ->
                            image = secureUrl
                        }.onError { error ->
                            Log.e(TAG, "Failed to get image to Firebase Storage: $error")
                            Result.Error(FirebaseStorageError.IO_ERROR)
                        }
                }
                Message(
                    id = messageId,
                    role = role,
                    content = messageData["content"] as? String ?: "",
                    image = image,
                    timestamp = messageData["timestamp"] as? Long ?: 0L
                )
            }
            Chat(id = id, title = title, messages = messages, createdAt = createdAt)
        }
    }


    override suspend fun getAllChats(): Result<List<ChatSummary>, FirebaseFirestoreError> {
        if (currentUserId == null) {
            return Result.Error(FirebaseFirestoreError.UNAUTHENTICATED)
        }

        return safeFirebaseFirestoreCall {
            val querySnapshot = firestore.collection("users")
                .document(currentUserId!!)
                .collection("chatSummaries")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val chats = querySnapshot.documents.map { document ->
                val id = document.getString("id") ?: ""
                val title = document.getString("title") ?: ""
                val createdAt = document.getLong("createdAt") ?: 0L
                val updatedAt = document.getLong("updatedAt") ?: 0L
                ChatSummary(
                    id = id,
                    title = title,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            }
            chats
        }

    }

    override suspend fun deleteChat(chatId: String): Result<Unit, FirebaseFirestoreError> {
        if (currentUserId == null) {
            return Result.Error(FirebaseFirestoreError.UNAUTHENTICATED)
        }

        safeFirebaseStorageCall {
            val chatImagesPath =
                "${CHAT_IMAGE_STORAGE_PATH_START}${currentUserId!!}/$chatId${CHAT_IMAGE_STORAGE_PATH_END}"
            storage.deleteAllPictures(chatImagesPath)
                .onSuccess {
                    Log.i(TAG, "Successfully deleted all images for chat: $chatId")
                }
                .onError { error ->
                    Log.e(TAG, "Error deleting images for chat $chatId: $error")
                    return@safeFirebaseStorageCall Result.Error(FirebaseStorageError.IO_ERROR)
                }
        }

        return safeFirebaseFirestoreCall {
            firestore.collection("users")
                .document(currentUserId!!)
                .collection("chats")
                .document(chatId)
                .delete()
                .await()
            firestore.collection("users")
                .document(currentUserId!!)
                .collection("chatSummaries")
                .document(chatId)
                .delete()
                .await()
            Unit
        }
    }

    private suspend fun getSecureImageUrl(
        chatId: String,
        messageId: String,
        image: Uri?
    ): String? {
        var finalImageUrl: String? = null
        image?.let { imageUri ->
            safeFirebaseStorageCall {
                val path =
                    "${CHAT_IMAGE_STORAGE_PATH_START}${currentUserId!!}/$chatId${CHAT_IMAGE_STORAGE_PATH_END}/${messageId}"
                storage.uploadPicture(path, imageUri)
                    .onSuccess { secureUrl ->
                        finalImageUrl = secureUrl
                    }.onError { error ->
                        Log.e(TAG, "Failed to upload image to Firebase Storage: $error")
                        Result.Error(FirebaseStorageError.IO_ERROR)
                    }
            }
        }
        return finalImageUrl
    }

    companion object {
        private const val TAG = "FirestoreChatRepository"
        private const val CHAT_IMAGE_STORAGE_PATH_START = "chats/"
        private const val CHAT_IMAGE_STORAGE_PATH_END = "/images"
    }
}