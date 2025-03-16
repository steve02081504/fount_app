package com.steve02081504.fount

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.steve02081504.fount.FountServiceDiscovery.isFountServiceAvailable
import kotlinx.coroutines.launch

class StartActivity : AppCompatActivity() {

	private lateinit var hostAddressEditText: EditText
	private lateinit var statusTextView: TextView
	private lateinit var fountStatusTextView: TextView
	private lateinit var settingsButton: Button
	private lateinit var continueButton: Button
	private lateinit var sharedPreferences: SharedPreferences

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_start)

		hostAddressEditText = findViewById(R.id.host_address)
		statusTextView = findViewById(R.id.status_text)
		fountStatusTextView = findViewById(R.id.fount_status_text)
		settingsButton = findViewById(R.id.settings_button)
		continueButton = findViewById(R.id.continue_button)
		sharedPreferences = getSharedPreferences("fount_prefs", Context.MODE_PRIVATE)

		settingsButton.setOnClickListener {
			startActivity(Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS))
		}
		checkDefaultAssistApp()
		setupContinueButton()
		setupHostAddressInput()
		startFountServiceDiscovery() // 启动 Fount 服务发现

		WindowCompat.setDecorFitsSystemWindows(window, false)
	}

	private fun checkDefaultAssistApp() {
		val defaultAssist = Settings.Secure.getString(contentResolver, "assistant")
		val ourComponent = ComponentName(this, this::class.java) // Use StartActivity
		val isDefault = defaultAssist == ourComponent.flattenToString()

		if (isDefault) {
			statusTextView.text = "Fount is set as the default assistive app."
		} else {
			statusTextView.text = "Fount is not the default assistive app."
			settingsButton.visibility = View.VISIBLE
		}
	}

	private fun setupContinueButton() {
		continueButton.setOnClickListener {
			val hostAddress = hostAddressEditText.text.toString()
			if (hostAddress.isNotEmpty()) {
				sharedPreferences.edit { putString("fountHostUrl", hostAddress) }
				val intent = Intent(this, MainActivity::class.java)
				intent.putExtra("fountHostUrl", hostAddress)
				startActivity(intent)
			}
		}
		continueButton.isEnabled = false // Initially disable the button
	}

	private fun setupHostAddressInput() {
		hostAddressEditText.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				// Check if the text is empty and disable/enable the button accordingly
				continueButton.isEnabled = !s.isNullOrEmpty()
			}

			override fun afterTextChanged(s: Editable?) {}
		})
	}

	private fun startFountServiceDiscovery() {
		lifecycleScope.launch {
			validateAndSetHost(FountServiceDiscovery.getFountHostUrl(sharedPreferences))
		}
	}

	private suspend fun validateAndSetHost(host: String?) {
		if (host != null) {
			if (isFountServiceAvailable(host)) {
				hostAddressEditText.setText(host) // Ensure UI update on main thread
				continueButton.isEnabled = true // Enable continue button
				sharedPreferences.edit { putString("fountHostUrl", host) }
				fountStatusTextView.text = "Fount service found at $host."
			} else {
				hostAddressEditText.text.clear() // Clear invalid host
				continueButton.isEnabled = false // Disable continue button
				sharedPreferences.edit { remove("fountHostUrl") }
				fountStatusTextView.text = "Fount service not available at saved address."
			}
		} else {
			continueButton.isEnabled = false
			fountStatusTextView.text = "No saved Fount address. Please enter one."
		}
	}
}
