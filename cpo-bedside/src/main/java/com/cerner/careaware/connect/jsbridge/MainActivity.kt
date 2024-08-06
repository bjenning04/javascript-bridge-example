package com.cerner.careaware.connect.jsbridge

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import com.cerner.careaware.connect.jsbridge.ui.theme.CPOBedsideTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

private val TAG = MainActivity::class.simpleName

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPOBedsideTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    MainView(
                        modifier = Modifier.padding(it),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("SetJavaScriptEnabled", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainView(modifier: Modifier = Modifier) {
    val permissionState =
        rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.RECORD_AUDIO,
            ),
        )
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    if (!permissionState.allPermissionsGranted) {
        LaunchedEffect(Unit) {
            permissionState.launchMultiplePermissionRequest()
        }
    } else {
        Scaffold(modifier = modifier, snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }) {
            AndroidView(
                factory = {
                    WebView(it).apply {
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )

                        settings.javaScriptEnabled = true
                        webViewClient =
                            object : WebViewClient() {
                                override fun onPageFinished(
                                    view: WebView?,
                                    url: String?,
                                ) {
                                    super.onPageFinished(view, url)
                                    Log.d(TAG, "$url")

                                    val uri = Uri.parse(url)
                                    uri.getQueryParameter("session")?.let {
                                        context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
                                            .edit { putString("session", it) }
                                    }

                                    CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
                                    CookieManager.getInstance().flush()

                                    if (uri.pathSegments.lastOrNull() == "home") {
                                        val params =
                                            Gson().toJson(
                                                mapOf(
                                                    "test1" to "one",
                                                    "test2" to 2,
                                                    "test3" to context.getString(R.string.long_text),
                                                ),
                                            )
                                        view?.loadUrl("javascript:setMeetingInfo($params)")
                                    }
                                }
                            }
                        webChromeClient =
                            object : WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                    coroutineScope.launch {
                                        consoleMessage?.message()?.let { message ->
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                    return super.onConsoleMessage(consoleMessage)
                                }
                            }
                    }
                },
                update = {
                    val urlBuilder = Uri.parse(BuildConfig.BASE_URL).buildUpon()
                    it.context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
                        .getString("session", null)
                        ?.let { session -> urlBuilder.appendQueryParameter("session", session) }
                    it.loadUrl(urlBuilder.build().toString())
                },
            )
        }
    }
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
