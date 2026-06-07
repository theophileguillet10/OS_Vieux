package com.elderlyos.vieuxos

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage

private const val PHOTOS_PER_PAGE = 9

sealed class MediaItem {
    data class Photo(val uri: android.net.Uri) : MediaItem()
    data class Video(val uri: android.net.Uri, val id: Long) : MediaItem()
}

class GalleryActivity : ComponentActivity() {

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    else
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) showGallery()
        else ActivityCompat.requestPermissions(this, permissions, 200)
    }

    private fun showGallery() { setContent { GalleryScreen() } }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.any { it == PackageManager.PERMISSION_GRANTED })
            showGallery()
    }
}

private fun loadMedia(context: android.content.Context): List<MediaItem> {
    val items = mutableListOf<Pair<Long, MediaItem>>()
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
        null, null, null
    )?.use { c ->
        val idCol   = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        while (c.moveToNext()) {
            val id = c.getLong(idCol)
            items.add(c.getLong(dateCol) to MediaItem.Photo(
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            ))
        }
    }
    context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DATE_ADDED),
        null, null, null
    )?.use { c ->
        val idCol   = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        while (c.moveToNext()) {
            val id = c.getLong(idCol)
            items.add(c.getLong(dateCol) to MediaItem.Video(
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id), id
            ))
        }
    }
    return items.sortedByDescending { it.first }.map { it.second }
}

@Composable
fun GalleryScreen() {
    val context   = LocalContext.current
    val media     = remember { loadMedia(context) }
    val pageCount = if (media.isEmpty()) 1 else (media.size + PHOTOS_PER_PAGE - 1) / PHOTOS_PER_PAGE

    var page          by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    if (selectedIndex != null) {
        val item = media[selectedIndex!!]
        if (item is MediaItem.Video) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(item.uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
            selectedIndex = null
        } else {
            PhotoViewer(
                media         = media,
                currentIndex  = selectedIndex!!,
                onIndexChange = { selectedIndex = it },
                onBack        = { selectedIndex = null }
            )
            return
        }
    }

    val pageItems = media.drop(page * PHOTOS_PER_PAGE).take(PHOTOS_PER_PAGE)

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF57C00))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text("📷  Gallery", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            }
            if (pageCount > 1) {
                Text(
                    text = "${page + 1} / $pageCount",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        if (media.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No photos yet", color = Color(0xFF666666), fontSize = 20.sp)
            }
        } else {
            val cols = 3
            val rows = 3
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until cols) {
                            val localIdx    = row * cols + col
                            val globalIndex = page * PHOTOS_PER_PAGE + localIdx
                            if (localIdx < pageItems.size) {
                                val item = pageItems[localIdx]
                                val uri  = if (item is MediaItem.Photo) item.uri
                                else (item as MediaItem.Video).uri
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { selectedIndex = globalIndex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (item is MediaItem.Video) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        BottomNavBar(
            onLeft  = if (page > 0) ({ page-- }) else null,
            onRight = if (page < pageCount - 1) ({ page++ }) else null
        )
    }
}

@Composable
fun PhotoViewer(
    media:         List<MediaItem>,
    currentIndex:  Int,
    onIndexChange: (Int) -> Unit,
    onBack:        () -> Unit
) {
    val uri = (media[currentIndex] as? MediaItem.Photo)?.uri ?: run { onBack(); return }

    val photoIndices = media.indices.filter { media[it] is MediaItem.Photo }
    val prevIndex    = photoIndices.lastOrNull  { it < currentIndex }
    val nextIndex    = photoIndices.firstOrNull { it > currentIndex }

    var scale   by remember(currentIndex) { mutableStateOf(1f) }
    var offsetX by remember(currentIndex) { mutableStateOf(0f) }
    var offsetY by remember(currentIndex) { mutableStateOf(0f) }

    // Container size in pixels — needed to compute pan bounds
    var containerW by remember { mutableStateOf(0f) }
    var containerH by remember { mutableStateOf(0f) }

    val panStep = 200f

    /**
     * Maximum translation so the image edge never goes past the container edge.
     *
     * When scale > 1, the image is (scale × containerW) wide.
     * The extra pixels on each side = (scale - 1) × containerW / 2.
     * We can pan up to that amount in either direction.
     *
     *   maxOffsetX = (scale - 1) × containerW / 2
     *   maxOffsetY = (scale - 1) × containerH / 2
     */
    fun clampOffset(ox: Float, oy: Float): Pair<Float, Float> {
        val maxX = ((scale - 1f) * containerW / 2f).coerceAtLeast(0f)
        val maxY = ((scale - 1f) * containerH / 2f).coerceAtLeast(0f)
        return ox.coerceIn(-maxX, maxX) to oy.coerceIn(-maxY, maxY)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    containerW = size.width.toFloat()
                    containerH = size.height.toFloat()
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX       = scale,
                        scaleY       = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )

            // ── Zoom buttons — right side ─────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OverlayButton("+") {
                    scale = (scale + 0.5f).coerceAtMost(6f)
                    // Re-clamp after zoom — bounds shrink when zooming out
                    val (cx, cy) = clampOffset(offsetX, offsetY)
                    offsetX = cx; offsetY = cy
                }
                Text(
                    text = "${"%.1f".format(scale)}×",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OverlayButton("−") {
                    scale = (scale - 0.5f).coerceAtLeast(1f)
                    if (scale == 1f) { offsetX = 0f; offsetY = 0f }
                    else {
                        val (cx, cy) = clampOffset(offsetX, offsetY)
                        offsetX = cx; offsetY = cy
                    }
                }
            }

            // ── D-pad — left side (only when zoomed in) ───────────────────────
            if (scale > 1f) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ▲ — scroll up → offsetY increases (image shifts down = viewport moves up)
                    OverlayIconButton(Icons.Filled.KeyboardArrowUp) {
                        val (cx, cy) = clampOffset(offsetX, offsetY + panStep)
                        offsetX = cx; offsetY = cy
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // ◀ — scroll left → offsetX increases
                        OverlayIconButton(Icons.Filled.KeyboardArrowLeft) {
                            val (cx, cy) = clampOffset(offsetX + panStep, offsetY)
                            offsetX = cx; offsetY = cy
                        }
                        // ⊙ — recenter
                        OverlayButton("⊙") {
                            offsetX = 0f; offsetY = 0f
                        }
                        // ▶ — scroll right → offsetX decreases
                        OverlayIconButton(Icons.Filled.KeyboardArrowRight) {
                            val (cx, cy) = clampOffset(offsetX - panStep, offsetY)
                            offsetX = cx; offsetY = cy
                        }
                    }
                    // ▼ — scroll down → offsetY decreases
                    OverlayIconButton(Icons.Filled.KeyboardArrowDown) {
                        val (cx, cy) = clampOffset(offsetX, offsetY - panStep)
                        offsetX = cx; offsetY = cy
                    }
                }
            }
        }

        BottomNavBar(
            onBack  = onBack,
            onLeft  = prevIndex?.let { idx -> { onIndexChange(idx) } },
            onRight = nextIndex?.let { idx -> { onIndexChange(idx) } }
        )
    }
}

@Composable
private fun OverlayIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
    }
}

@Composable
private fun OverlayButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(60.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}