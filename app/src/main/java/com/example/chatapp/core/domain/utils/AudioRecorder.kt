package com.example.chatapp.core.domain.utils

import java.io.File

interface AudioRecorder {
    fun start(outputFile: File)
    fun stop()
}