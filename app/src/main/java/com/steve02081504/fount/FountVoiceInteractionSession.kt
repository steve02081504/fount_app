package com.steve02081504.fount

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Base64
import android.view.View
import java.io.ByteArrayOutputStream
import java.util.UUID

class FountVoiceInteractionSession(
	context: Context?, // Store initial arguments
	private val initialArgs: Bundle
) :
	VoiceInteractionSession(context) {
	private var overlayWindow: FountOverlayWindow? = null
	private var apiClient: FountApiClient? = null

	override fun onCreate() {
		super.onCreate()
		apiClient = FountApiClient() // Initialize API client (implementation later)
	}

	override fun onCreateContentView(): View? {
		// We'll create and manage the overlay window ourselves.
		// overlayWindow = FountOverlayWindow(context) // Initialize in onShow instead
		return View(context) // Return an empty view since the overlay is added in onShow
	}


	override fun onShow(args: Bundle?, showFlags: Int) {
		super.onShow(args, showFlags)
		// Instantiate FountOverlayWindow here, passing the activity context
		overlayWindow = FountOverlayWindow(context!!, activityContext = context!!)
		overlayWindow?.show()


		if ((showFlags and SHOW_WITH_ASSIST) != 0) {
			// Assist data will be available in onHandleAssist
		}
		if ((showFlags and SHOW_WITH_SCREENSHOT) != 0) {
			// Screenshot will be available in onHandleScreenshot
		}
	}

	override fun onHide() {
		super.onHide()
		overlayWindow?.hide()
	}

	override fun onHandleAssist(state: AssistState) {
		super.onHandleAssist(state)
		val structure = state.assistStructure
		val content = state.assistContent

		// Extract data and send to Fount server
		val sessionId = generateSessionId() // Implement this method
		val userQuery: String? = overlayWindow?.getUserInput() // Get user input from overlay, safely

		// (Implementation for parsing AssistStructure and AssistContent later)
		val extractedText: String = AssistDataParser.extractText(structure)
		//String viewHierarchyJson = AssistDataParser.extractViewHierarchy(structure); // Example
		val structuredData = if (content != null) content.structuredData else ""
		val assistData = state.assistData

		overlayWindow?.displayFountResponse("正在处理...") // Show a loading message, safely

		apiClient?.sendDataToFount( // Safe call on apiClient
			sessionId,
			userQuery,
			extractedText,  /*viewHierarchyJson,*/
			structuredData,
			assistData,
			object : FountApiClient.FountApiResponseListener {
				override fun onResponse(response: String?, expectInput: Boolean) {
					overlayWindow?.displayFountResponse(response) //safely
					if (expectInput) {
						overlayWindow?.waitForInput(object : FountOverlayWindow.InputListener { //safely
							override fun onInputReceived(input: String) {
								// Recursively call a method similar to onHandleAssist
								//  to process the new input, maintaining the session ID.
								processNextInput(sessionId, input, structure, content) //New method
							}
						})
					}
				}

				override fun onError(error: String?) {
					overlayWindow?.displayFountResponse("Error: $error") //safely
				}
			})
	}

	// New method to handle subsequent inputs in a conversation
	private fun processNextInput(
		sessionId: String,
		input: String,
		structure: AssistStructure?,
		content: AssistContent?
	) {
		val extractedText: String = AssistDataParser.extractText(structure)
		//String viewHierarchyJson = AssistDataParser.extractViewHierarchy(structure); // Example
		val structuredData = if (content != null) content.structuredData else ""
		val assistData = Bundle() //TODO GET RIGHT ASSISTDATA

		overlayWindow?.displayFountResponse("正在处理...") // Show a loading message, safely

		apiClient?.sendDataToFount( //safe call
			sessionId,
			input,
			extractedText,  /*viewHierarchyJson,*/
			structuredData,
			assistData,
			object : FountApiClient.FountApiResponseListener {
				override fun onResponse(response: String?, expectInput: Boolean) {
					overlayWindow?.displayFountResponse(response) //safely
					if (expectInput) {
						overlayWindow?.waitForInput(object : FountOverlayWindow.InputListener {  //safely
							override fun onInputReceived(input: String) {
								// Recursively call a method similar to onHandleAssist
								//  to process the new input, maintaining the session ID.
								processNextInput(
									sessionId,
									input,
									structure,
									content
								) //Recursive call
							}
						})
					}
				}

				override fun onError(error: String?) {
					overlayWindow?.displayFountResponse("Error: $error") //safely
				}
			})
	}
	override fun onHandleScreenshot(screenshot: Bitmap?) {
		super.onHandleScreenshot(screenshot)
		overlayWindow?.setScreenshot(screenshot)
	}

	override fun onDestroy() {
		super.onDestroy()
		if (overlayWindow != null) {
			overlayWindow!!.destroy()
		}
	}

	private fun generateSessionId(): String {
		// Implement a method to generate a unique session ID (e.g., using UUID)
		return UUID.randomUUID().toString()
	}

	private fun bitmapToBase64(bitmap: Bitmap): String {
		// Implement a method to convert a Bitmap to a Base64 string
		val byteArrayOutputStream = ByteArrayOutputStream()
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
		val byteArray = byteArrayOutputStream.toByteArray()
		return Base64.encodeToString(byteArray, Base64.DEFAULT)
	}
}
