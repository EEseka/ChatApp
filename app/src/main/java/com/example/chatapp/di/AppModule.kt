package com.example.chatapp.di

import android.app.Application
import androidx.credentials.CredentialManager
import androidx.room.Room
import com.aallam.openai.client.OpenAI
import com.example.chatapp.BuildConfig
import com.example.chatapp.authentication.data.CheckFirstInstallDataSource
import com.example.chatapp.authentication.data.FirebaseAuthRepositoryImpl
import com.example.chatapp.authentication.domain.CheckFirstInstallUseCase
import com.example.chatapp.authentication.domain.UserAuthUseCase
import com.example.chatapp.authentication.domain.validation.ValidateEmail
import com.example.chatapp.authentication.domain.validation.ValidatePassword
import com.example.chatapp.authentication.domain.validation.ValidateRepeatedPassword
import com.example.chatapp.authentication.domain.validation.ValidateSignInPassword
import com.example.chatapp.authentication.presentation.AuthEventBus
import com.example.chatapp.authentication.presentation.signin.SignInViewModel
import com.example.chatapp.authentication.presentation.signup.SignUpViewModel
import com.example.chatapp.authentication.presentation.welcome.WelcomeViewModel
import com.example.chatapp.chat.data.FirestoreChatRepository
import com.example.chatapp.chat.data.OpenAiRepository
import com.example.chatapp.chat.data.UserRepoImpl
import com.example.chatapp.chat.data.local.TrendingSearchDao
import com.example.chatapp.chat.data.local.TrendingSearchDatabase
import com.example.chatapp.chat.domain.AiDataSource
import com.example.chatapp.chat.domain.ChatDatabase
import com.example.chatapp.chat.domain.UserRepoUseCase
import com.example.chatapp.chat.presentation.MainEventBus
import com.example.chatapp.chat.presentation.home.ChatViewModel
import com.example.chatapp.chat.presentation.settings.SettingsViewModel
import com.example.chatapp.core.data.firebase.FirebaseMediaStorage
import com.example.chatapp.core.data.utils.AndroidAudioRecorder
import com.example.chatapp.core.data.utils.FileManagerImpl
import com.example.chatapp.core.data.utils.ImageCompressorImpl
import com.example.chatapp.core.domain.MediaStorage
import com.example.chatapp.core.domain.utils.AudioRecorder
import com.example.chatapp.core.domain.utils.FileManager
import com.example.chatapp.core.domain.utils.ImageCompressor
import com.example.chatapp.core.domain.validation.ValidateDisplayName
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }
    single { OpenAI(BuildConfig.OPENAI_API_KEY) }
    // Room
    single {
        Room.databaseBuilder(
            get<Application>(), TrendingSearchDatabase::class.java, "trending_searches_db"
        ).fallbackToDestructiveMigration(false).build()
    }
    single<TrendingSearchDao> { get<TrendingSearchDatabase>().dao }

    singleOf(::OpenAiRepository).bind<AiDataSource>()
    singleOf(::FirebaseAuthRepositoryImpl).bind<UserAuthUseCase>()
    singleOf(::FirestoreChatRepository).bind<ChatDatabase>()
    singleOf(::FirebaseMediaStorage).bind<MediaStorage>()
    singleOf(::UserRepoImpl).bind<UserRepoUseCase>()
    // Equivalent verbose approach: single<UserAuthUseCase> { FirebaseAuthRepositoryImpl(get()) }
    singleOf(::CheckFirstInstallDataSource).bind<CheckFirstInstallUseCase>()
    singleOf(::ImageCompressorImpl).bind<ImageCompressor>()
    singleOf(::FileManagerImpl).bind<FileManager>()
    singleOf(::AndroidAudioRecorder).bind<AudioRecorder>()
    single { ValidateEmail() }
    single { ValidatePassword() }
    single { ValidateRepeatedPassword() }
    single { ValidateSignInPassword() }
    single { ValidateDisplayName() }
    single { AuthEventBus() }
    single { MainEventBus() }
    single { CredentialManager.create(get()) }

    viewModelOf(::SignUpViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::WelcomeViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ChatViewModel)
}