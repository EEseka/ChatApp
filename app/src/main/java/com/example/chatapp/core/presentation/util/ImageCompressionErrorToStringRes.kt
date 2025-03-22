package com.example.chatapp.core.presentation.util

import com.example.chatapp.R
import com.example.chatapp.core.domain.util.ImageCompressionError

fun ImageCompressionError.toStringRes(): Int {
    val resId = when (this) {
        ImageCompressionError.FILE_NOT_FOUND -> R.string.error_file_not_found
        ImageCompressionError.FILE_IO_ERROR -> R.string.error_file_io_error
        ImageCompressionError.FILE_NOT_IMAGE -> R.string.error_file_not_image
        ImageCompressionError.COMPRESSION_ERROR -> R.string.error_compression_error
    }
    return resId
}