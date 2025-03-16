package com.steve02081504.fount

import android.content.Context
import android.os.Bundle
import android.util.Log
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FountApiClient {
	private val client = OkHttpClient()
	private val executor: ExecutorService = Executors.newSingleThreadExecutor()


	interface FountApiResponseListener {
		fun onResponse(response: String?, expectInput: Boolean)
		fun onError(error: String?)
	}


	fun sendDataToFount(
		sessionId: String?, query: String?, contextText: String?,  /*String viewHierarchy,*/
		structuredData: String?, assistData: Bundle?, listener: FountApiResponseListener
	) {
		executor.execute {
			try {
				val requestBodyJson = JSONObject()
				requestBodyJson.put("sessionId", sessionId)
				requestBodyJson.put("query", query)

				val contextJson = JSONObject()
				contextJson.put("text", contextText)
				//  contextJson.put("viewHierarchy", new JSONObject(viewHierarchy)); // If sending view hierarchy
				contextJson.put("structuredData", structuredData)
				if (assistData != null) {
					val assistDataJson = JSONObject()
					for (key in assistData.keySet()) {
						val value = assistData[key]
						//需要对value进行类型判断
						if (value is String) {
							assistDataJson.put(key, value)
						} else if (value is Int) {
							assistDataJson.put(key, value)
						} else if (value is Boolean) {
							assistDataJson.put(key, value)
						} else if (value is Long) {
							assistDataJson.put(key, value)
						} else {
							// Handle other types or log a warning
							Log.w(
								"FountApiClient",
								"Unsupported assist data type for key: $key"
							)
						}
					}
					contextJson.put("assistData", assistDataJson)
				}


				requestBodyJson.put("context", contextJson)
				requestBodyJson.put("timestamp", System.currentTimeMillis())


				val requestBody: RequestBody =
					requestBodyJson.toString().toRequestBody(JSON) // 使用 toRequestBody 扩展函数
				//你需要在StartActivity中获取到fountHostUrl
				val fountHostUrl =
					"http://your-fount-host:port/api/chat" // Replace with actual URL  记得替换成你自己的！

				//或者使用之前保存在SharedPreferences的fountHostUrl
				val request = Request.Builder()
					.url(fountHostUrl)
					.post(requestBody)
					.build()

				client.newCall(request).execute().use { response ->
					if (!response.isSuccessful) {
						throw IOException("Unexpected code $response")
					}
					val responseBody = response.body!!.string()
					val jsonResponse = JSONObject(responseBody)
					val fountResponse = jsonResponse.getString("response")
					val expectInput = jsonResponse.getBoolean("expectInput")

					// Use getContext().getMainExecutor() to run on the main thread
					(context as FountVoiceInteractionSessionService).mainExecutor.execute {
						listener.onResponse(
							fountResponse,
							expectInput
						)
					}
				}
			} catch (e: Exception) {
				(context as FountVoiceInteractionSessionService).mainExecutor.execute {
					listener.onError(
						e.message
					)
				}
			}
		}
	}
	private val context: Context
		get() = FountApplication.getAppContext()
	companion object {
		private val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
	}
}
