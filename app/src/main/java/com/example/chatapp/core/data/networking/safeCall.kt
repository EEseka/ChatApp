package com.example.chatapp.core.data.networking

import com.example.chatapp.core.domain.util.NetworkError
import com.example.chatapp.core.domain.util.Result
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import kotlin.coroutines.coroutineContext

suspend inline fun <reified T> safeCall(execute: () -> HttpResponse): Result<T, NetworkError> {
    val response = try {
        execute()
    } catch (e: UnresolvedAddressException) {
        return Result.Error(NetworkError.NO_INTERNET)
    } catch (e: SerializationException) {
        return Result.Error(NetworkError.SERIALIZATION)
    } catch (e: Exception) {
        coroutineContext.ensureActive() // Avoids cancelled coroutines from being caught and not propagated up
        return Result.Error(NetworkError.UNKNOWN)
    }

    return responseToResult(response)
}
// Checks for errors while fetching the response and then calls line 23 whose function checks for errors after the response is fetched