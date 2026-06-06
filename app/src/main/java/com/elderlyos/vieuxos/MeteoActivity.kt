package com.elderlyos.vieuxos

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

data class DailyForecast(
    val date: String,
    val tempMin: Double,
    val tempMax: Double,
    val weatherCode: Int
)

data class WeatherData(
    val city: String,
    val tempCurrent: Double,
    val tempMin: Double,
    val tempMax: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val forecast: List<DailyForecast> = emptyList()
)

class MeteoActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            setContent { MeteoScreen() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            setContent { MeteoScreen() }
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }
}

private suspend fun fetchLocation(context: android.content.Context): Location? =
    withContext(Dispatchers.IO) {
        try {
            val lm = context.getSystemService(LocationManager::class.java)
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            providers.mapNotNull { provider ->
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    @Suppress("MissingPermission")
                    lm?.getLastKnownLocation(provider)
                } else null
            }.maxByOrNull { it.accuracy }
        } catch (e: Exception) { null }
    }

private suspend fun fetchWeather(lat: Double, lon: Double): WeatherData? =
    withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current_weather=true" +
                    "&daily=temperature_2m_max,temperature_2m_min,weathercode" +
                    "&timezone=auto&forecast_days=7"
            val json = JSONObject(URL(url).readText())
            val current = json.getJSONObject("current_weather")
            val daily = json.getJSONObject("daily")

            val dates = daily.getJSONArray("time")
            val maxTemps = daily.getJSONArray("temperature_2m_max")
            val minTemps = daily.getJSONArray("temperature_2m_min")
            val codes = daily.getJSONArray("weathercode")

            // Skip index 0 = today, show days 1–6
            val forecast = (1 until dates.length()).map { i ->
                DailyForecast(
                    date = formatForecastDate(dates.getString(i)),
                    tempMin = minTemps.getDouble(i),
                    tempMax = maxTemps.getDouble(i),
                    weatherCode = codes.getInt(i)
                )
            }

            WeatherData(
                city = "",
                tempCurrent = current.getDouble("temperature"),
                tempMin = minTemps.getDouble(0),
                tempMax = maxTemps.getDouble(0),
                weatherCode = current.getInt("weathercode"),
                windSpeed = current.getDouble("windspeed"),
                forecast = forecast
            )
        } catch (e: Exception) { null }
    }

private fun formatForecastDate(dateStr: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val output = SimpleDateFormat("EEEE d MMM", Locale.getDefault())
        val date = input.parse(dateStr) ?: return dateStr
        output.format(date).replaceFirstChar { it.uppercaseChar() }
    } catch (e: Exception) { dateStr }
}

private fun cityFromLocation(context: android.content.Context, lat: Double, lon: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        addresses?.firstOrNull()?.let { addr ->
            addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
        } ?: ""
    } catch (e: Exception) { "" }
}

private fun weatherLabel(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    51, 53, 55 -> "Drizzle"
    61, 63, 65 -> "Rain"
    66, 67 -> "Freezing rain"
    71, 73, 75 -> "Snow"
    77 -> "Snow grains"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Unknown"
}

private fun weatherIcon(code: Int): ImageVector = when (code) {
    0, 1 -> Icons.Filled.WbSunny
    2, 3 -> Icons.Filled.Cloud
    45, 48 -> Icons.Filled.Cloud
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Filled.WaterDrop
    66, 67, 71, 73, 75, 77, 85, 86 -> Icons.Filled.AcUnit
    95, 96, 99 -> Icons.Filled.Thunderstorm
    else -> Icons.Filled.WbSunny
}

private fun weatherColor(code: Int): Color = when (code) {
    0, 1 -> Color(0xFFFFA000)
    2, 3 -> Color(0xFF546E7A)
    45, 48 -> Color(0xFF78909C)
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> Color(0xFF1565C0)
    66, 67, 71, 73, 75, 77, 85, 86 -> Color(0xFF0277BD)
    95, 96, 99 -> Color(0xFF4A148C)
    else -> Color(0xFF1976D2)
}

@Composable
fun MeteoScreen() {
    val context = LocalContext.current
    var weather by remember { mutableStateOf<WeatherData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        val location = fetchLocation(context)
        if (location == null) {
            error = "Could not get location.\nMake sure GPS is enabled."
            loading = false
            return@LaunchedEffect
        }
        val data = fetchWeather(location.latitude, location.longitude)
        if (data == null) {
            error = "Could not load weather.\nCheck your internet connection."
            loading = false
            return@LaunchedEffect
        }
        val city = withContext(Dispatchers.IO) {
            cityFromLocation(context, location.latitude, location.longitude)
        }
        weather = data.copy(city = city)
        loading = false
    }

    val bgColor = weather?.let { weatherColor(it.weatherCode) } ?: Color(0xFF1565C0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Weather", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        when {
            loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 4.dp)
                        Spacer(Modifier.height(20.dp))
                        Text("Fetching weather…", color = Color.White, fontSize = 20.sp)
                    }
                }
            }
            error != null -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = error!!,
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            weather != null -> {
                val w = weather!!
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(4.dp))

                    // City + current weather in a compact row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${w.tempCurrent.toInt()}°C",
                                color = Color.White,
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 68.sp
                            )
                            Text(weatherLabel(w.weatherCode), color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                        }
                        Icon(weatherIcon(w.weatherCode), null, tint = Color.White, modifier = Modifier.size(80.dp))
                    }

                    Spacer(Modifier.height(8.dp))

                    // Today stats
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            WeatherStat(Icons.Filled.KeyboardArrowDown, "Min", "${w.tempMin.toInt()}°C")
                            WeatherStat(Icons.Filled.KeyboardArrowUp, "Max", "${w.tempMax.toInt()}°C")
                            WeatherStat(Icons.Filled.Air, "Wind", "${w.windSpeed.toInt()} km/h")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Forecast header
                    Text(
                        text = "Next 6 days",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(Modifier.height(6.dp))

                    // Forecast rows — fills remaining space
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            w.forecast.forEachIndexed { index, day ->
                                ForecastRow(day)
                                if (index < w.forecast.lastIndex) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        BottomNavBar()
    }
}

@Composable
fun ForecastRow(day: DailyForecast) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = day.date,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(weatherIcon(day.weatherCode), null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${day.tempMin.toInt()}° / ${day.tempMax.toInt()}°",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun WeatherStat(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
    }
}
