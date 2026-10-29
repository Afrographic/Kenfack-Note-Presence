
package com.example.myapp

import android.app.Activity
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

@Composable
fun LocalWebsite(
    modifier: Modifier = Modifier,
    startPage: String = "index.html"
) {
    var fileCallback by remember {
        mutableStateOf<ValueCallback<Array<Uri>>?>(null)
    }

    var webView by remember {
        mutableStateOf<WebView?>(null)
    }

    var canGoBack by remember {
        mutableStateOf(false)
    }

    // Handle the Android device Back button.
    BackHandler(enabled = canGoBack) {
        val currentWebView = webView

        if (currentWebView?.canGoBack() == true) {
            currentWebView.goBack()
        } else {
            canGoBack = false
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        fileCallback?.onReceiveValue(
            uri?.let { arrayOf(it) }
        )
        fileCallback = null
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->

            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler(
                    "/assets/",
                    WebViewAssetLoader.AssetsPathHandler(context)
                )
                .build()

            WebView(context).apply {

                webView = this

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                addJavascriptInterface(
                    AndroidShare(context as Activity),
                    "AndroidShare"
                )

                webViewClient = object : WebViewClientCompat() {

                    private fun updateBackState(view: WebView) {
                        canGoBack = view.canGoBack()
                    }

                    override fun onPageFinished(
                        view: WebView,
                        url: String
                    ) {
                        super.onPageFinished(view, url)
                        updateBackState(view)
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView,
                        url: String,
                        isReload: Boolean
                    ) {
                        super.doUpdateVisitedHistory(
                            view,
                            url,
                            isReload
                        )
                        updateBackState(view)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(
                            request.url
                        )
                    }

                    @Suppress("DEPRECATION")
                    override fun shouldInterceptRequest(
                        view: WebView,
                        url: String
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(
                            Uri.parse(url)
                        )
                    }
                }

                webChromeClient = object : WebChromeClient() {

                    override fun onShowFileChooser(
                        webView: WebView,
                        callback: ValueCallback<Array<Uri>>,
                        fileChooserParams: FileChooserParams
                    ): Boolean {

                        fileCallback?.onReceiveValue(null)
                        fileCallback = callback

                        filePicker.launch(
                            arrayOf(
                                "application/json",
                                "text/json",
                                "text/plain"
                            )
                        )

                        return true
                    }
                }

                loadUrl(
                    "https://appassets.androidplatform.net/assets/$startPage"
                )
            }
        }
    )
}
