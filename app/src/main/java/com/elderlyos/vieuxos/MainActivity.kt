package com.elderlyos.vieuxos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomeScreen() }
    }
}

// Pages: list of tile lists. Add more pages by adding more lists here.
private val PAGE_COUNT = 2

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(0) }
    var slideDirection by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEEEEE))
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                slideInHorizontally { it * slideDirection } togetherWith
                        slideOutHorizontally { -it * slideDirection }
            },
            modifier = Modifier.weight(1f),
            label = "page"
        ) { page ->
            when (page) {
                0 -> PageOne(context)
                1 -> PageTwo(context)
                else -> PageOne(context)
            }
        }

        // Page number indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDDDDDD))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${currentPage + 1} / $PAGE_COUNT",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }

        BottomNavBar(
            onLeft = {
                if (currentPage > 0) {
                    slideDirection = -1
                    currentPage--
                }
            },
            onRight = {
                if (currentPage < PAGE_COUNT - 1) {
                    slideDirection = 1
                    currentPage++
                }
            },
            onHome = {
                if (currentPage != 0) {
                    slideDirection = -1
                    currentPage = 0
                }
            }
        )
    }
}

@Composable
fun PageOne(context: android.content.Context) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            AppTile("Call Family", Icons.Filled.Phone, Color(0xFF2E7D32)) {
                context.startActivity(Intent(context, PhoneActivity::class.java))
            }
        }
        item {
            AppTile("Take Me Home", Icons.Filled.Home, Color(0xFF00838F)) {
                context.startActivity(Intent(context, GoHomeActivity::class.java))
            }
        }
        item {
            AppTile("Camera", Icons.Filled.PhotoCamera, Color(0xFF6A1B9A)) {
                context.startActivity(Intent(context, CameraActivity::class.java))
            }
        }
        item {
            AppTile("Gallery", Icons.Filled.PhotoLibrary, Color(0xFFF57C00)) {
                context.startActivity(Intent(context, GalleryActivity::class.java))
            }
        }
        item {
            AppTile("Messages", Icons.Filled.Chat, Color(0xFF4527A0)) {
                context.startActivity(Intent(context, MessagesActivity::class.java))
            }
        }
        item {
            AppTile(
                label = "SOS",
                icon = Icons.Filled.Warning,
                color = Color(0xFFC62828),
                onLongClick = {
                    context.startActivity(Intent(context, FamilySetupActivity::class.java))
                }
            ) {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
            }
        }
        }

        MedicationBanner(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun PageTwo(context: android.content.Context) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppTile("YouTube", Icons.Filled.PlayCircle, Color(0xFFCC0000)) {
                    context.startActivity(Intent(context, YoutubeActivity::class.java))
                }
            }
            item {
                AppTile("Internet", Icons.Filled.Language, Color(0xFF1A73E8)) {
                    context.startActivity(Intent(context, ChromeActivity::class.java))
                }
            }
            item {
                AppTile("Weather", Icons.Filled.WbSunny, Color(0xFFFFA000)) {
                    context.startActivity(Intent(context, MeteoActivity::class.java))
                }
            }
        }

        BirthdayBanner(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    label: String,
    icon: ImageVector,
    color: Color,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(color, RoundedCornerShape(8.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val bannerColors = listOf(
    Color(0xFFE53935),
    Color(0xFF8E24AA),
    Color(0xFF1E88E5),
    Color(0xFF00897B),
    Color(0xFFF4511E),
    Color(0xFF3949AB),
)

@Composable
fun MedicationBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val medications = remember { loadMedications(context) }
    var currentIndex by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "banner_color")
    val animatedColor by infiniteTransition.animateColor(
        initialValue = bannerColors[0],
        targetValue = bannerColors[1],
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_color"
    )

    LaunchedEffect(medications.size) {
        while (medications.isNotEmpty()) {
            delay(3000)
            currentIndex = (currentIndex + 1) % medications.size
        }
    }

    Box(
        modifier = modifier.background(animatedColor, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (medications.isEmpty()) {
            Text("No medications", color = Color.White, fontSize = 20.sp)
        } else {
            val med = medications[currentIndex]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Medication, null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(6.dp))
                Text(med.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(med.meal, color = Color.White.copy(alpha = 0.9f), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun BirthdayBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val allEvents = remember { loadEvents(context) }
    val todaysEvents = remember {
        val cal = java.util.Calendar.getInstance()
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        eventsForDay(allEvents, month, day)
    }

    Box(
        modifier = modifier
            .background(Color(0xFF3949AB), RoundedCornerShape(16.dp))
            .clickable { context.startActivity(Intent(context, CalendarActivity::class.java)) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text("Today's Events", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            if (todaysEvents.isEmpty()) {
                Text("No events today", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            } else {
                todaysEvents.forEach { event ->
                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Tap to see the week →", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        }
    }
}

@Composable
fun BigButton(label: String, color: Color, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
