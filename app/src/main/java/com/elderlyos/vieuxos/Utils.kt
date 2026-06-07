package com.elderlyos.vieuxos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared Up / Down scroll buttons.
 * Used in MessagesActivity and GalleryActivity.
 */
@Composable
fun ScrollButtons(onUp: () -> Unit, onDown: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onUp,
            modifier = Modifier.weight(1f).height(58.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
        ) { Text("▲", fontSize = 18.sp, color = Color.White) }

        Button(
            onClick = onDown,
            modifier = Modifier.weight(1f).height(58.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
        ) { Text("▼", fontSize = 18.sp, color = Color.White) }
    }
}