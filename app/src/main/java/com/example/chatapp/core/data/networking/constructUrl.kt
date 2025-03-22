package com.example.chatapp.core.data.networking

import com.example.chatapp.BuildConfig
//
//
//fun constructUrl(url: String): String {
//    return when {
//        url.contains(BuildConfig.BASE_URL) -> url
//        url.startsWith("/") -> BuildConfig.BASE_URL + url.drop(1) // we drop the first '/' as the base url string already contains a '/' at the end
//        else -> BuildConfig.BASE_URL + url // Just in case we pass a path without the first slash
//    }
//}