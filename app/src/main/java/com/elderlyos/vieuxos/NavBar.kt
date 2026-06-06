package com.elderlyos.vieuxos

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavBar(
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavButton("←") {
            (context as? Activity)?.finish()
        }

        NavButton("◀") {
            onLeft?.invoke()
        }

        NavButton(
            label = "🏠",
            onLongPress = {
                val intent = Intent(context, FamilySetupActivity::class.java)
                context.startActivity(intent)
            }
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        }

        NavButton("▶") {
            onRight?.invoke()
        }

        NavButton("✕") {
            (context as? Activity)?.finish()
        }
    }
}

@Composable
fun NavButton(
    label: String,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress?.invoke() },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = label,
                fontSize = 22.sp,
                color = Color.White
            )
        }
    }
}