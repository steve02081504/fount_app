package com.steve02081504.fount

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class FountVoiceInteractionSessionService : VoiceInteractionSessionService() {
	override fun onNewSession(args: Bundle): VoiceInteractionSession {
		return FountVoiceInteractionSession(this, args) // Pass args to the session
	}
}
