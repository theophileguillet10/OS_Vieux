package com.elderlyos.vieuxos

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class YoutubeActivity : ComponentActivity() {

    // Custom view shown when the user taps a video (YouTube's fullscreen player)
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            NoInternetGuard {
                YoutubeScreen(
                    onShowCustomView = { view, callback -> showCustomView(view, callback) },
                    onHideCustomView = { hideCustomView() }
                )
            }
        }
    }

    // Called by YouTube when the user taps a video — adds the player as a full-window overlay
    fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }
        customView = view
        customViewCallback = callback
        val frame = window.decorView as FrameLayout
        frame.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        view.keepScreenOn = true
    }

    // Called when the video exits fullscreen — removes the overlay
    fun hideCustomView() {
        customView?.let { view ->
            (window.decorView as FrameLayout).removeView(view)
            view.keepScreenOn = false
        }
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    // Back press: dismiss fullscreen video first, then navigate WebView history
    override fun onBackPressed() {
        if (customView != null) { hideCustomView(); return }
        super.onBackPressed()
    }
}

@Composable
fun YoutubeScreen(
    onShowCustomView: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onHideCustomView: () -> Unit
) {
    var webView  by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableStateOf(0) }

    BackHandler {
        if (webView?.canGoBack() == true) webView?.goBack()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // WebView + progress bar overlaid in a Box so the bar never shifts the WebView
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled              = true
                        domStorageEnabled              = true
                        mediaPlaybackRequiresUserGesture = false
                        cacheMode                      = WebSettings.LOAD_DEFAULT
                        useWideViewPort                = true
                        loadWithOverviewMode           = true
                        setSupportZoom(false)           // YouTube controls its own zoom
                        setSupportMultipleWindows(true) // needed for video pop-ups
                        allowContentAccess             = true
                        allowFileAccess                = true
                        @Suppress("DEPRECATION")
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        // Chrome Mobile UA — avoids "browser not supported" block
                        userAgentString =
                            "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/124.0.0.0 Mobile Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            // Auto-accept Google/YouTube consent wall
                            view?.evaluateJavascript("""
                                (function() {
                                    var btns = document.querySelectorAll('button');
                                    for (var b of btns) {
                                        var t = (b.innerText || b.textContent || '').trim();
                                        if (t === 'Accept all' || t === 'Accepter tout' ||
                                            t === 'Accepteer alles' || t === 'Alle akzeptieren') {
                                            b.click(); return;
                                        }
                                    }
                                    for (var b of btns) {
                                        var t = (b.innerText || b.textContent || '').trim();
                                        if (t.startsWith('Accept') || t.startsWith('Accepter') ||
                                            t.startsWith('Accepteer')) {
                                            b.click(); return;
                                        }
                                    }
                                })();
                            """.trimIndent(), null)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }

                        // ── Required for video playback to work ──────────────
                        override fun onShowCustomView(
                            view: View,
                            callback: CustomViewCallback
                        ) {
                            onShowCustomView(view, callback)
                        }

                        override fun onHideCustomView() {
                            onHideCustomView()
                        }

                        // YouTube opens a new window for the video player —
                        // load it in the same WebView so it renders correctly.
                        override fun onCreateWindow(
                            view: WebView,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: android.os.Message
                        ): Boolean {
                            val newWebView = WebView(view.context)
                            newWebView.settings.javaScriptEnabled = true
                            newWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                                    view.loadUrl(r.url.toString())
                                    return true
                                }
                            }
                            val transport = resultMsg.obj as WebView.WebViewTransport
                            transport.webView = newWebView
                            resultMsg.sendToTarget()
                            return true
                        }
                    }

                    val cm = android.webkit.CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(this, true)
                    // Pre-accept Google/YouTube consent so the cookie wall is skipped
                    cm.setCookie(".youtube.com", "SOCS=CAISNQgDEitib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmljZXNfMjAyMzA4MDItMF9SQzEaAnhh")
                    cm.setCookie(".google.com",  "SOCS=CAISNQgDEitib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmljZXNfMjAyMzA4MDItMF9SQzEaAnhh")

                    loadUrl("https://m.youtube.com")
                    webView = this
                }
            }
        )

        // Loading bar floats at the top of the WebView — never shifts content
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress  = { progress / 100f },
                modifier  = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color      = Color(0xFFFF0000),
                trackColor = Color.Transparent
            )
        }

        } // end Box

        BottomNavBar(
            onLeft  = { webView?.goBack() },
            onRight = { webView?.goForward() }
        )
    }
}
