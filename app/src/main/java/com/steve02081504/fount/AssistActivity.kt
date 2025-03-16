package com.steve02081504.fount

import android.Manifest
import android.app.Activity
import android.app.assist.AssistContent
import android.content.Context
import android.app.assist.AssistStructure
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.steve02081504.fount.FountOverlayWindow.InputListener

class AssistActivity : Activity() {

	private val REQUEST_CODE_SYSTEM_ALERT_WINDOW = 1000

	private lateinit var overlayWindow: FountOverlayWindow

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// Keep the window on top
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		// Make the activity's background transparent
		setTheme(android.R.style.Theme_Translucent_NoTitleBar)

		checkOverlayPermission()

		// Handle the assist intent
		onNewIntent(intent)
	}

	private fun checkOverlayPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!Settings.canDrawOverlays(this)) {
				Log.i("AssistActivity", "Requesting overlay permission")
				val intent = Intent(
					Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
					Uri.parse("package:$packageName")
				)
				startActivityForResult(intent, REQUEST_CODE_SYSTEM_ALERT_WINDOW)
			} else {
				showOverlayWindow()
			}
		} else {
			showOverlayWindow()
		}
	}

	private fun showOverlayWindow() {
		overlayWindow = FountOverlayWindow(this, this)
		overlayWindow.show()
	}


	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_CODE_SYSTEM_ALERT_WINDOW) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (Settings.canDrawOverlays(this)) {
					Log.i("AssistActivity", "Overlay permission granted")
					showOverlayWindow()
				} else {
					Log.e("AssistActivity", "Overlay permission denied")
					// Handle the case where the user denies the permission
				}
			}
		}
	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		super.onNewIntent(intent)
		Log.i("AssistActivity", "onNewIntent: $intent")

		val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
		if (text != null) {
			overlayWindow.displayText("Text from Intent: $text")
		}
	}

	override fun onProvideAssistContent(assistContent: AssistContent) {
		val webUri: Uri? = assistContent.webUri
		Log.i("AssistActivity", "Web URI: $webUri")
		if (webUri != null) {
			overlayWindow.displayText("Web URI: $webUri")
		} else {
			// If no web URI, display a default message or handle accordingly
			if (intent?.getStringExtra(Intent.EXTRA_TEXT) == null) {
				overlayWindow.displayText("No text or web URI found.")
			}
		}
	}

	override fun onProvideAssistData(data: Bundle) {

	}
	override fun onResume() {
		super.onResume()
	}

	override fun onPause() {
		super.onPause()
	}

	override fun onDestroy() {
		super.onDestroy()
		overlayWindow.destroy()
	}
}
