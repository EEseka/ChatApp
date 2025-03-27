package com.example.chatapp.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.example.chatapp.authentication.data.CheckFirstInstallDataSource
import com.example.chatapp.authentication.data.FirebaseAuthRepositoryImpl
import com.example.chatapp.authentication.domain.CheckFirstInstallUseCase
import com.example.chatapp.authentication.domain.UserAuthUseCase
import com.example.chatapp.authentication.presentation.AuthEventBus
import com.example.chatapp.authentication.presentation.signin.SignInViewModel
import com.example.chatapp.authentication.presentation.signup.SignUpViewModel
import com.example.chatapp.authentication.presentation.welcome.WelcomeViewModel
import com.example.chatapp.chat.data.UserRepoImpl
import com.example.chatapp.chat.domain.UserRepoUseCase
import com.example.chatapp.chat.presentation.MainEventBus
import com.example.chatapp.chat.presentation.settings.SettingsViewModel
import com.example.chatapp.core.data.FileManagerImpl
import com.example.chatapp.core.data.ImageCompressorImpl
import com.example.chatapp.core.domain.FileManager
import com.example.chatapp.core.domain.ImageCompressor
import com.example.chatapp.core.domain.validation.ValidateDisplayName
import com.example.chatapp.authentication.domain.validation.ValidateEmail
import com.example.chatapp.authentication.domain.validation.ValidatePassword
import com.example.chatapp.authentication.domain.validation.ValidateRepeatedPassword
import com.example.chatapp.authentication.domain.validation.ValidateSignInPassword
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val authModule = module {
    single { FirebaseAuth.getInstance() }

    singleOf(::FirebaseAuthRepositoryImpl).bind<UserAuthUseCase>()
    singleOf(::UserRepoImpl).bind<UserRepoUseCase>()
    // Equivalent verbose approach: single<UserAuthUseCase> { FirebaseAuthRepositoryImpl(get()) }
    singleOf(::CheckFirstInstallDataSource).bind<CheckFirstInstallUseCase>()
    singleOf(::ImageCompressorImpl).bind<ImageCompressor>()
    singleOf(::FileManagerImpl).bind<FileManager>()
    single { ValidateEmail() }
    single { ValidatePassword() }
    single { ValidateRepeatedPassword() }
    single { ValidateSignInPassword() }
    single { ValidateDisplayName() }
    single { AuthEventBus() }
    single { MainEventBus() }
    single { CredentialManager.create(get<Context>()) }

    viewModelOf(::SignUpViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::WelcomeViewModel)
    viewModelOf(::SettingsViewModel)
}