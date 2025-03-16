package com.steve02081504.fount

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.appcompat.app.AppCompatActivity
import android.webkit.CookieManager

@SuppressLint("SetJavaScriptEnabled")
fun AppCompatActivity.setupWebView(webView: WebView, url: String) {
	webView.isVerticalScrollBarEnabled = false
	webView.isHorizontalScrollBarEnabled = false
	val webSettings = webView.settings
	webSettings.javaScriptEnabled = true
	webSettings.domStorageEnabled = true
	webSettings.loadWithOverviewMode = true
	webSettings.useWideViewPort = true
	webSettings.builtInZoomControls = true
	webSettings.displayZoomControls = false
	webSettings.setSupportZoom(false)
	webSettings.defaultTextEncodingName = "utf-8"

	val cookieManager = CookieManager.getInstance()
	cookieManager.setAcceptCookie(true)
	cookieManager.setAcceptThirdPartyCookies(webView, true)

	webView.webChromeClient = FountWebChromeClient()
	webView.addJavascriptInterface(WebAppInterface(), "Android")

	webView.webViewClient = object : WebViewClient() {
		override fun onPageFinished(view: WebView?, url: String?) {
			super.onPageFinished(view, url)
			getColorFromCSS(webView)
		}
	}
	webView.loadUrl(url)
}

fun AppCompatActivity.getColorFromCSS(webView: WebView) {
	webView.evaluateJavascript(
		"""
		(function() {
			var color = getComputedStyle(document.documentElement).getPropertyValue('--color-base-100').trim();
			Android.updateColor(color);
		})();
		""".trimIndent()
	) {}
}

class WebAppInterface {
	@JavascriptInterface
	fun updateColor(colorString: String) {
		// Since we don't have the activity context here, we need to use a different approach
		Log.d("WebAppInterface", "Received color: $colorString")
		// We will update the system bars color from MainActivity after parsing
		colorUpdateCallback?.invoke(colorString)
	}

	companion object {
		var colorUpdateCallback: ((String) -> Unit)? = null
	}
}

fun AppCompatActivity.parseColor(colorString: String): Int {
	return try {
		Color.parseColor(colorString) // Try standard parsing first
	} catch (e: IllegalArgumentException) {
		Log.d("MainActivity", "Standard color parsing failed, trying OKLCH: $colorString")
		parseOklchColor(colorString)
	}
}

fun AppCompatActivity.parseOklchColor(colorString: String): Int {
	// MODIFIED REGEX - handles spaces, optional commas
	val oklchPattern = """oklch\(\s*([\d.]+)%\s*[,\s]*([\d.]+)\s*[,\s]*([\d.]+)\s*\)""".toRegex()
	val matchResult = oklchPattern.find(colorString)

	if (matchResult != null) {
		try {
			val (l, c, h) = matchResult.destructured
			// Convert L string to Float (no need to divide by 100 anymore, regex removes %)
			val lFloat = l.toFloat()
			val cFloat = c.toFloat()
			val hFloat = h.toFloat()
			return convertOklchToArgb(lFloat / 100f, cFloat, hFloat)

		} catch (e: NumberFormatException) {
			Log.e("MainActivity", "Invalid OKLCH number format: $colorString", e)
			throw IllegalArgumentException("Invalid OKLCH number format")
		}
	} else {
		Log.e("MainActivity", "Invalid OKLCH color format: $colorString")
		throw IllegalArgumentException("Invalid OKLCH color format")
	}
}

fun AppCompatActivity.convertOklchToArgb(l: Float, c: Float, h: Float): Int {
	// More concise conversion
	val a = c * kotlin.math.cos(Math.toRadians(h.toDouble())).toFloat()
	val b = c * kotlin.math.sin(Math.toRadians(h.toDouble())).toFloat()
	return ColorUtils.LABToColor(l.toDouble(), a.toDouble(), b.toDouble())
}

fun AppCompatActivity.updateSystemBarsColor(color: Int) {
	window.statusBarColor = color
	window.navigationBarColor = color

	val isLightColor = ColorUtils.calculateLuminance(color) > 0.5
	WindowCompat.getInsetsController(window, window.decorView).apply {
		isAppearanceLightStatusBars = isLightColor
		isAppearanceLightNavigationBars = isLightColor
	}
}
