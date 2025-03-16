package com.steve02081504.fount

import android.app.AlertDialog
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.EditText

internal class FountWebChromeClient : WebChromeClient() {
	override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
		AlertDialog.Builder(view.context)
			.setTitle("Alert") // 可选，设置对话框标题
			.setMessage(message)
			.setPositiveButton(
				android.R.string.ok
			) {
				dialog, which ->
				result.confirm() // 重要！确认对话框
				dialog.dismiss()
			}
			.setCancelable(false) // 防止用户取消对话框
			.create()
			.show()
		return true // 返回 true 表示我们处理了该事件
	}

	override fun onJsConfirm(
		view: WebView,
		url: String,
		message: String,
		result: JsResult
	): Boolean {
		AlertDialog.Builder(view.context)
			.setTitle("Confirm")
			.setMessage(message)
			.setPositiveButton(
				android.R.string.ok
			) { dialog, which ->
				result.confirm() // 用户点击了确认
				dialog.dismiss()
			}
			.setNegativeButton(
				android.R.string.cancel
			) { dialog, which ->
				result.cancel() // 用户点击了取消
				dialog.dismiss()
			}
			.setCancelable(false)
			.create()
			.show()
		return true
	}

	override fun onJsPrompt(
		view: WebView,
		url: String,
		message: String,
		defaultValue: String,
		result: JsPromptResult
	): Boolean {
		val input = EditText(view.context)
		input.setText(defaultValue)

		AlertDialog.Builder(view.context)
			.setTitle("Prompt")
			.setMessage(message)
			.setView(input) // 添加输入框
			.setPositiveButton(
				android.R.string.ok
			) { dialog, whichButton ->
				val value = input.text.toString()
				result.confirm(value) // 将用户输入的值传递回去
				dialog.dismiss()
			}
			.setNegativeButton(
				android.R.string.cancel
			) { dialog, whichButton ->
				result.cancel()
				dialog.dismiss()
			}
			.setCancelable(false)
			.create()
			.show()
		return true
	}
}
