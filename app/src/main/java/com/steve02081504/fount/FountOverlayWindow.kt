package com.steve02081504.fount

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView

class FountOverlayWindow(private val context: Context, private val activityContext: Context) {

	companion object {
		private var isShowing = false
	}

	private val windowManager: WindowManager =
		context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
	private val rootView: View
	private val responseTextView: TextView
	private val inputEditText: EditText
	private val screenshotImageView: ImageView
	private val sendButton: Button
	private var inputListener: InputListener? = null

	private val params: WindowManager.LayoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		WindowManager.LayoutParams(
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
			PixelFormat.TRANSLUCENT
		)
	} else {
		WindowManager.LayoutParams(
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.TYPE_PHONE,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
			PixelFormat.TRANSLUCENT
		)
	}.apply {
		gravity = Gravity.TOP or Gravity.START
		x = 0
		y = 0
	}

	interface InputListener {
		fun onInputReceived(input: String)
	}

	init {
		val inflater = LayoutInflater.from(context)
		rootView = inflater.inflate(R.layout.overlay_layout, null)

		responseTextView = rootView.findViewById(R.id.response_text)
		inputEditText = rootView.findViewById(R.id.input_edit_text)
		screenshotImageView = rootView.findViewById(R.id.screenshot_image)
		sendButton = rootView.findViewById(R.id.send_button)


		sendButton.setOnClickListener {
			inputListener?.onInputReceived(inputEditText.text.toString())
			inputEditText.setText("") // Clear the input field
		}

		// Improved click listener to dismiss the activity
		val controlsContainer = rootView.findViewById<View>(R.id.controls_container)
		rootView.findViewById<View>(R.id.dismiss_view).setOnTouchListener { _, event ->
			if (event.action == MotionEvent.ACTION_DOWN) {
				val controlsRect = Rect()
				controlsContainer.getGlobalVisibleRect(controlsRect)

				if (!controlsRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
					(activityContext as? Activity)?.finish()
					return@setOnTouchListener true
				}
			}
			false
		}
	}

	fun show() {
		if (isShowing) {
			return // Prevent multiple instances
		}

		windowManager.addView(rootView, params)
		isShowing = true
	}

	fun hide() {
		if (rootView.isAttachedToWindow) {
			windowManager.removeView(rootView)
		}
	}

	fun destroy() {
		hide()
		isShowing = false // Reset the flag
	}

	fun setScreenshot(screenshot: Bitmap?) {
		if (screenshot != null) {
			screenshotImageView.setImageBitmap(screenshot)
			screenshotImageView.visibility = View.VISIBLE
		} else {
			screenshotImageView.visibility = View.GONE
		}
	}

	fun getRootView(): View {
		return rootView
	}

	fun getUserInput(): String {
		return inputEditText.text.toString()
	}

	fun displayFountResponse(response: String?) {
		responseTextView.text = response
	}

	fun waitForInput(listener: InputListener?) {
		inputListener = listener
	}

	fun displayText(text: String?) {
		responseTextView.text = text ?: ""
	}
}
