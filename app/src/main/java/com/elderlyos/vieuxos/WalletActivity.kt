package com.elderlyos.vieuxos

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Dynamically find ALL NFC payment apps registered on this device ──────────
// Any payment app (Google Wallet, Samsung Pay, bank apps, etc.) must register an
// android.nfc.cardemulation.action.HOST_APDU_SERVICE — so we query that instead
// of maintaining a fragile hardcoded list.
fun installedPaymentApps(context: Context): List<Pair<String, String>> {
    val pm = context.packageManager
    val intent = Intent("android.nfc.cardemulation.action.HOST_APDU_SERVICE")
    @Suppress("DEPRECATION")
    val services = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)

    val seen   = mutableSetOf<String>()
    val result = mutableListOf<Pair<String, String>>()
    for (ri in services) {
        val pkg = ri.serviceInfo.packageName
        if (pkg !in seen && pkg != context.packageName) {
            seen.add(pkg)
            val label = try {
                pm.getApplicationLabel(
                    pm.getApplicationInfo(pkg, 0)
                ).toString()
            } catch (_: Exception) { pkg }
            result.add(pkg to label)
        }
    }
    return result
}

class WalletActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WalletScreen() }
    }
}

@Composable
fun WalletScreen() {
    val context = LocalContext.current
    val prefs   = context.getSharedPreferences("vieuxos_prefs", Context.MODE_PRIVATE)

    val walletName = prefs.getString("wallet_name", null)
    val configured = !walletName.isNullOrBlank()

    val nfcAdapter  = remember { NfcAdapter.getDefaultAdapter(context) }
    val nfcAvailable = nfcAdapter != null
    val nfcEnabled   = nfcAdapter?.isEnabled == true

    // Overall readiness
    val ready = configured && nfcAvailable && nfcEnabled

    val bgColor     = if (ready) Color(0xFF00695C) else Color(0xFF37474F)
    val headerColor = if (ready) Color(0xFF004D40) else Color(0xFF263238)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Wallet",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.weight(1f))

        when {
            // ── Case 1: not configured ────────────────────────────────────────
            !configured -> {
                ErrorState(
                    icon    = Icons.Filled.CreditCardOff,
                    title   = "Wallet not configured",
                    message = "Ask a family member to configure the wallet\nin the app settings (long-press SOS)."
                )
            }

            // ── Case 2: NFC not available ─────────────────────────────────────
            !nfcAvailable -> {
                ErrorState(
                    icon    = Icons.Filled.WifiOff,
                    title   = "NFC not available",
                    message = "This device does not support contactless payment."
                )
            }

            // ── Case 3: NFC disabled ──────────────────────────────────────────
            !nfcEnabled -> {
                ErrorState(
                    icon    = Icons.Filled.WifiOff,
                    title   = "NFC is turned off",
                    message = "Please enable NFC to use contactless payment."
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        context.startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
                    },
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF546E7A))
                ) {
                    Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Enable NFC", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Case 4: everything ready ──────────────────────────────────────
            else -> {
                ReadyState(walletName = walletName!!)
            }
        }

        Spacer(Modifier.weight(1f))
        BottomNavBar()
    }
}

// ── Ready state: big pulsing card icon + "hold near terminal" ─────────────────
@Composable
private fun ReadyState(walletName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        // Big circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color(0xFF00897B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color.White
            )
        }

        Text(
            text = "Ready to pay",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Hold your phone near\nthe payment terminal",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )

        // NFC status badge
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFF388E3C)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Nfc, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text(
                    text = "NFC active · $walletName",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Error state ───────────────────────────────────────────────────────────────
@Composable
private fun ErrorState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(Color(0xFF546E7A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Text(
            text = message,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
    }
}
