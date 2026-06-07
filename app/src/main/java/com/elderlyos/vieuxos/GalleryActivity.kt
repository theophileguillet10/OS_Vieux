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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage

private const val PHOTOS_PER_PAGE = 9   // 3 × 3 grid

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

// ── Media loading ─────────────────────────────────────────────────────────────

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

// ── Screens ───────────────────────────────────────────────────────────────────

@Composable
fun GalleryScreen() {
    val context   = LocalContext.current
    val s         = getStrings(context)
    val media     = remember { loadMedia(context) }
    val pageCount = if (media.isEmpty()) 1 else (media.size + PHOTOS_PER_PAGE - 1) / PHOTOS_PER_PAGE

    var page          by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // ── Photo viewer ──────────────────────────────────────────────────────────
    if (selectedIndex != null) {
        val item = media[selectedIndex!!]
        if (item is MediaItem.Video) {
            // Open videos in system player, then return to grid
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

    // ── Grid view ─────────────────────────────────────────────────────────────
    val pageItems = media.drop(page * PHOTOS_PER_PAGE).take(PHOTOS_PER_PAGE)

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF57C00))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text(s.galleryTitle, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
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
                Text(s.noMedia, color = Color(0xFF666666), fontSize = 20.sp)
            }
        } else {
            // 3×3 grid — each row takes equal weight so all rows fill the available height
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
                            val localIdx = row * cols + col
                            val globalIndex = page * PHOTOS_PER_PAGE + localIdx
                            if (localIdx < pageItems.size) {
                                val item = pageItems[localIdx]
                                val uri = if (item is MediaItem.Photo) item.uri
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
                                // Empty cell filler so spacing stays consistent
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // ← prev page | home | → next page
        BottomNavBar(
            onLeft  = if (page > 0) ({ page-- }) else null,
            onRight = if (page < pageCount - 1) ({ page++ }) else null
        )
    }
}

@Composable
fun PhotoViewer(
    media:        List<MediaItem>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onBack:       () -> Unit
) {
    val uri = (media[currentIndex] as? MediaItem.Photo)?.uri ?: run { onBack(); return }

    // Photo indices only (skip videos for prev/next navigation)
    val photoIndices = media.indices.filter { media[it] is MediaItem.Photo }
    val prevIndex    = photoIndices.lastOrNull { it < currentIndex }
    val nextIndex    = photoIndices.firstOrNull { it > currentIndex }

    // Reset zoom and pan when photo changes
    var scale  by remember(currentIndex) { mutableStateOf(1f) }
    var offsetX by remember(currentIndex) { mutableStateOf(0f) }
    var offsetY by remember(currentIndex) { mutableStateOf(0f) }

    // How many pixels to pan per button press
    val panStep = 200f

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Photo
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

            // ── Zoom buttons — right side, vertically centred ─────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OverlayButton("+") {
                    scale = (scale + 0.5f).coerceAtMost(6f)
                }
                Text(
                    text = "${"%.1f".format(scale)}×",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OverlayButton("−") {
                    scale = (scale - 0.5f).coerceAtLeast(1f)
                    // Reset pan if fully zoomed out
                    if (scale == 1f) { offsetX = 0f; offsetY = 0f }
                }
            }

            // ── D-pad — left side, vertically centred (only when zoomed in) ──
            if (scale > 1f) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Up   → translationY negative moves image UP
                    OverlayIconButton(Icons.Filled.KeyboardArrowUp)    { offsetY -= panStep }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Left  → translationX negative moves image LEFT
                        OverlayIconButton(Icons.Filled.KeyboardArrowLeft)  { offsetX -= panStep }
                        // Right → translationX positive moves image RIGHT
                        OverlayIconButton(Icons.Filled.KeyboardArrowRight) { offsetX += panStep }
                    }
                    // Down  → translationY positive moves image DOWN
                    OverlayIconButton(Icons.Filled.KeyboardArrowDown)  { offsetY += panStep }
                }
            }
        }

        // Nav bar: back = return to gallery, left = prev photo, home = home, right = next photo
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
