package com.example.chatapp.authentication.presentation.welcome

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.chatapp.R
import com.example.chatapp.ui.theme.ChatAppTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Animations for alpha (fade-in), scale (zoom-in), rotation, and exit alpha (fade-out).
    val alphaAnimation = remember { Animatable(0f) }
    val scaleAnimation = remember { Animatable(0.5f) }
    val rotationAnimation = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    // Infinite transition for shadow elevation pulsing effect.
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val shadowElevation = infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    // Launching animations for fade-in, zoom-in, and rotation.
    LaunchedEffect(Unit) {
        alphaAnimation.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
        scaleAnimation.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        // Continuous rotation animation for the image.
        while (true) {
            rotationAnimation.animateTo(
                targetValue = rotationAnimation.value + 360f,
                animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
            )
        }
    }

    // Launching fade-out animation and triggering the next screen.
    LaunchedEffect(Unit) {
        delay(2000) // Delay to keep splash screen visible for 2 seconds.
        exitAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 500))
        onSplashFinished() // Trigger the next screen when animation ends.
    }

    // Main container for the splash screen content.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceDim,
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )
            )
            .alpha(exitAlpha.value), // Applying fade-out effect.
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated logo image.
            Image(
                painter = painterResource(id = R.drawable.chatapp_img),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .shadow(
                        elevation = shadowElevation.value.dp,
                        shape = CircleShape,
                        ambientColor = MaterialTheme.colorScheme.primary,
                        spotColor = MaterialTheme.colorScheme.primary
                    )
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                            ),
                            radius = 200f
                        ),
                        shape = CircleShape
                    )
                    .alpha(alphaAnimation.value) // Fade-in effect.
                    .scale(scaleAnimation.value) // Zoom-in effect.
                    .rotate(rotationAnimation.value) // Continuous rotation.
                    .wrapContentSize()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated app name text.
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .alpha(alphaAnimation.value) // Fade-in effect.
                    .scale(scaleAnimation.value) // Zoom-in effect.
            )
        }
    }
}

@Preview(
    showBackground = true, backgroundColor = 0xFF000000, showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    ChatAppTheme {
        SplashScreen { }
    }
}