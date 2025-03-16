package com.steve02081504.fount

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.steve02081504.fount.WebAppInterface.Companion.colorUpdateCallback

class MainActivity : AppCompatActivity() {

	private lateinit var webView: WebView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val hostUrl = intent.getStringExtra("fountHostUrl")
		if (hostUrl != null) {
			loadMainContent(hostUrl)
		} else {
			// Handle the case where no host URL is provided, maybe go back to StartActivity
			finish()
		}
	}

	private fun loadMainContent(hostAddress: String) {
		setContentView(R.layout.activity_main)
		webView = findViewById(R.id.webView)
		setupWebView(webView, hostAddress)

		// Set up the color update callback
		WebAppInterface.colorUpdateCallback = { colorString ->
			runOnUiThread {
				try {
					val color = parseColor(colorString)
					updateSystemBarsColor(color)
				} catch (e: Exception) {
					Log.e("MainActivity", "Error updating color: $colorString", e)
					updateSystemBarsColor(Color.BLACK) // Default color on error
				}
			}
		}
	}

	override fun onBackPressed() {
		if (::webView.isInitialized && webView.canGoBack()) {
			webView.goBack()
		} else {
			super.onBackPressed()
		}
	}
}
