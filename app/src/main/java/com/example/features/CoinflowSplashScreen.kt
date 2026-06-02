package com.example.features

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun CoinflowSplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Start local timer animations
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3200) // Total duration of the splash display (matches video length)
        onSplashFinished()
    }

    // Spring scaling for the logo mark, creating an organic springy pop-in
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.4f,
        animationSpec = spring(
            dampingRatio = 0.55f, // Bouncy and lissome, matching the springy physics of liquid ribbon paths
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    // Smooth fade-in for the logo mark structure
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 850, easing = LinearOutSlowInEasing),
        label = "logo_alpha"
    )

    // Gentle floating translation for the logomark icon
    val logoTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -25f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "logo_translation"
    )

    // Fade-in and slide-up for the premium brand header text "COINFLOW"
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 1000, easing = FastOutSlowInEasing),
        label = "text_alpha"
    )

    val textTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 35f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 1000, easing = FastOutSlowInEasing),
        label = "text_translation"
    )

    // Delayed luxury fade-in for tagline "YOUR EXPENSE TRACKER"
    val taglineAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 1700, easing = LinearOutSlowInEasing),
        label = "tagline_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEBEA)), // Exact matching warm light-grey background of the shared animation video
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Container for the beautiful brand vector mark
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .offset(y = logoTranslationY.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Coinflow Logo Mark",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Premium brand text header separating "COIN" and "FLOW" with corporate design color pairing
            Row(
                modifier = Modifier
                    .offset(y = textTranslationY.dp)
                    .alpha(textAlpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COIN",
                    color = Color(0xFF1E88E5), // Matches brand cyan-blue ribbon color style
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "FLOW",
                    color = Color(0xFF4CAF50), // Matches brand green arrow and ribbon accent style
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline below the brand name with elegant wide-tracking corporate spacing
            Text(
                text = "YOUR EXPENSE TRACKER",
                color = Color(0xFF6E6D6C), // Deep charcoal grey color matching the animation video text accent
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 4.sp, // Distinct typographic tracking
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(y = (textTranslationY * 0.6f).dp)
                    .alpha(taglineAlpha)
            )
        }
    }
}
