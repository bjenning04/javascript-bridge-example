package com.cerner.careaware.connect.jsbridge

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Base64
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.cerner.careaware.connect.jsbridge.ui.theme.JavaScriptBridgeExampleTheme
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JavaScriptBridgeExampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    MainView(
                        modifier = Modifier.padding(it)
                    )
                }
            }
        }
    }
}

@Composable
fun MainView(modifier: Modifier = Modifier, executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()) {
    AndroidView(
        modifier = modifier,
        factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = {
            it.settings.javaScriptEnabled = true
            it.addJavascriptInterface(WebAppInterface(it.context), "Android")
            it.loadUrl("file:///android_asset/index.html")

            executor.scheduleWithFixedDelay({
                Handler(it.context.mainLooper).post { it.loadUrl("javascript:addStuff('${System.currentTimeMillis()}')") }
            }, 0, 1, TimeUnit.SECONDS)
        },
    )
}

class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun getIdentifier(): String {
        return UUID.randomUUID().toString()
    }
}