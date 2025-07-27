package com.mentos_koder.remote_lg_tv.view

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.mentos_koder.remote_lg_tv.R

class HelpActivity : AppCompatActivity() {
    private lateinit var webViewHelp: WebView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        webViewHelp = findViewById(R.id.webViewHelp)
        btnBack = findViewById(R.id.btn_back)

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val webSettings: WebSettings = webViewHelp.settings
        webSettings.javaScriptEnabled = true
        webViewHelp.webViewClient = WebViewClient()

        val url = "https://aaaaa.blogspot.com/2024/02/guide-todo-list-app.htm"

        if (isTrustedUrl(url)) {
            webViewHelp.loadUrl(url)
        } else {
            webViewHelp.loadData("<h1>Untrusted URL</h1>", "text/html", "UTF-8")
        }
    }

    private fun isTrustedUrl(url: String): Boolean {
        val trustedDomains = listOf("https://aaaaa.blogspot.com")
        return trustedDomains.any { url.startsWith(it) }
    }
}