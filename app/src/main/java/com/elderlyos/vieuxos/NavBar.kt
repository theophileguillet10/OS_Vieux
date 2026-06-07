package com.elderlyos.vieuxos

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavBar(
    onBack:  (() -> Unit)? = null,   // null = finish current activity
    onLeft:  (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onHome:  (() -> Unit)? = null    // null = launch MainActivity
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFE0E0E0),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            NavIconButton(Icons.Filled.ArrowBack, Modifier.weight(1f)) {
                if (onBack != null) onBack()
                else (context as? Activity)?.finish()
            }
            // Left
            NavIconButton(Icons.Filled.KeyboardArrowLeft, Modifier.weight(1f)) {
                onLeft?.invoke()
            }
            // Home
            NavIconButton(Icons.Filled.Home, Modifier.weight(1f)) {
                if (onHome != null) {
                    onHome()
                } else {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                }
            }
            // Right
            NavIconButton(Icons.Filled.KeyboardArrowRight, Modifier.weight(1f)) {
                onRight?.invoke()
            }
        }
    }
}

@Composable
fun NavIconButton(icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCCCCCC)),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF222222),
            modifier = Modifier.size(48.dp)
        )
    }
}
