package com.example.hybridtodo.ui

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.hybridtodo.R
import com.example.hybridtodo.databinding.ActivityMainBinding
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.example.hybridtodo.widget.TodoListWidget
import com.example.hybridtodo.data.local.CookieStore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
    }

    private fun setupWebView() {
        binding.webview.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            
            // Ensure links open within the WebView
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return false // Let WebView handle the URL
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    android.webkit.CookieManager.getInstance().flush()

                    // Save Cookie to SharedPreferences
                    val cookie = android.webkit.CookieManager.getInstance().getCookie(url)
                    if (!cookie.isNullOrEmpty()) {
                        val prefs = getSharedPreferences("widget_auth", Context.MODE_PRIVATE)
                        prefs.edit().putString("cookie", cookie).apply()
                        
                        // Also update CookieStore for consistency if needed, but relying on prefs now
                        CookieStore.saveCookie(cookie)

                        // Notify Widget to refresh data (trigger onDataSetChanged)
                        val appWidgetManager = AppWidgetManager.getInstance(application)
                        val ids = appWidgetManager.getAppWidgetIds(ComponentName(application, TodoListWidget::class.java))
                        
                        // This triggers onDataSetChanged in WidgetRemoteViewsFactory
                        appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
                    }
                }
            }

            // Load the Next.js app URL
            loadUrl("http://20.193.248.140/") // Replace with actual URL
        }
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
