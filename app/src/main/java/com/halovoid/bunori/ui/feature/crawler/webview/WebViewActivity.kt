package com.halovoid.bunori.ui.feature.crawler.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.halovoid.bunori.api.core.network.NetworkClient
import com.halovoid.bunori.ui.core.theme.BunoriTheme
import com.halovoid.bunori.ui.core.theme.SecondaryText

class WebViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url") ?: "https://google.com"
        val host = intent.getStringExtra("host") ?: ""

        setContent {
            BunoriTheme {
                WebViewScreen(url = url) { success, userAgent ->
                    if (success && userAgent != null && host.isNotEmpty()) {
                        WebViewResolverImpl.getInstance().saveUserAgent(host, userAgent)
                    }
                    WebViewResolverImpl.onResolutionResult(host, success)
                    finish()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        super.onDestroy()
        CookieManager.getInstance().flush()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String, onFinished: (Boolean, String?) -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(url) }
    var pageTitle by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        CookieManager.getInstance().flush()
                        onFinished(true, NetworkClient.currentUserAgent)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { 
                    Column {
                        Text(
                            text = pageTitle.ifBlank { "WebView" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentUrl, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = SecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { 
                            CookieManager.getInstance().flush()
                            val cookies = CookieManager.getInstance().getCookie(currentUrl) ?: ""
                            android.util.Log.i("WebViewActivity", "=== [WEBVIEW DONE CLICKED] url=$currentUrl ===")
                            android.util.Log.i("WebViewActivity", "Flushed Cookies (${cookies.length} chars): $cookies")
                            onFinished(true, NetworkClient.currentUserAgent) 
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DONE")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.userAgentString = NetworkClient.currentUserAgent
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                url?.let { currentUrl = it }
                                pageTitle = view?.title ?: ""
                                
                                val cookies = CookieManager.getInstance().getCookie(url) ?: ""
                                CookieManager.getInstance().flush()
                                android.util.Log.i("WebViewActivity", "=== [WEBVIEW LOADED] url=$url ===")
                                android.util.Log.i("WebViewActivity", "Cookies (${cookies.length} chars): $cookies")
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                return false 
                            }
                        }
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter))
            }
        }
    }
}
