package com.steve02081504.fount
import android.app.Application
import android.content.Context

class FountApplication : Application() {

	companion object {
		private lateinit var appContext: Context

		fun getAppContext(): Context {
			return appContext
		}
	}

	override fun onCreate() {
		super.onCreate()
		appContext = applicationContext
	}
}
