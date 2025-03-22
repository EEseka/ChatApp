package com.example.chatapp.chat.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.chatapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Composable
fun HomeScreen(firebaseUser: FirebaseUser?, modifier: Modifier = Modifier) {
    val user = remember { FirebaseAuth.getInstance().currentUser }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        user?.let {
            Text(
                text = "Welcome ${it.displayName}",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your email is ${it.email}",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your uid is ${it.uid}",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(it.photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.profile_photo),
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}