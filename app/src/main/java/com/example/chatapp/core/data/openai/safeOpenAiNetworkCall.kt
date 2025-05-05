package com.example.chatapp.core.data.openai

import android.util.Log
import com.example.chatapp.core.domain.util.NetworkError
import com.example.chatapp.core.domain.util.Result
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlin.coroutines.coroutineContext

suspend inline fun <T> safeOpenAiNetworkCall(crossinline execute: suspend () -> T): Result<T, NetworkError> {
    return try {
        val response = withTimeout(30000L) { execute() }
        Result.Success(response)
    } catch (_: UnresolvedAddressException) {
        Result.Error(NetworkError.NO_INTERNET)
    } catch (_: SerializationException) {
        Result.Error(NetworkError.SERIALIZATION)
    } catch (_: TimeoutCancellationException) {
        Result.Error(NetworkError.REQUEST_TIMEOUT)
    } catch (e: Exception) {
        Log.e("OpenAiRepository", "Exception: ${e.message}")
        coroutineContext.ensureActive() // Avoids cancelled coroutines from being caught and not propagated up
        Result.Error(NetworkError.UNKNOWN)
    }
}
