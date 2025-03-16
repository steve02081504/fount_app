package com.steve02081504.fount

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import org.json.JSONArray
import org.json.JSONObject

object AssistDataParser {
	fun extractText(structure: AssistStructure?): String {
		if (structure == null) {
			return ""
		}

		val textContent = StringBuilder()
		val windowCount = structure.windowNodeCount

		for (i in 0..<windowCount) {
			val windowNode = structure.getWindowNodeAt(i)
			val rootViewNode = windowNode.rootViewNode
			extractTextFromViewNode(rootViewNode, textContent)
		}

		return textContent.toString()
	}

	private fun extractTextFromViewNode(node: ViewNode?, textContent: StringBuilder) {
		if (node == null) {
			return
		}

		val text = node.text
		if (text != null && text.length > 0) {
			textContent.append(text).append(" ")
		}

		// Check for content description
		val contentDescription = node.contentDescription
		if (contentDescription != null && contentDescription.length > 0) {
			textContent.append(contentDescription).append(" ")
		}


		val childCount = node.childCount
		for (i in 0..<childCount) {
			val childNode = node.getChildAt(i)
			extractTextFromViewNode(childNode, textContent)
		}
	}

	fun extractViewHierarchy(structure: AssistStructure?): String {
		if (structure == null) {
			return "{}" // Return empty JSON object
		}

		try {
			val rootJson = JSONObject()
			val windowCount = structure.windowNodeCount
			val windowsArray = JSONArray()

			for (i in 0..<windowCount) {
				val windowNode = structure.getWindowNodeAt(i)
				val rootViewNode = windowNode.rootViewNode
				val windowJson = viewNodeToJson(rootViewNode)
				windowsArray.put(windowJson)
			}

			rootJson.put("windows", windowsArray)
			return rootJson.toString(2) // Use toString(2) for pretty printing
		} catch (e: Exception) {
			e.printStackTrace()
			return "{\"error\": \"Failed to parse view hierarchy\"}"
		}
	}

	@Throws(Exception::class)
	private fun viewNodeToJson(node: ViewNode): JSONObject {
		val nodeJson = JSONObject()
		nodeJson.put("class", node.className)
		nodeJson.put("text", node.text)
		nodeJson.put("contentDescription", node.contentDescription)
		nodeJson.put("id", node.idEntry) // Use getIdEntry() for resource name
		nodeJson.put("visibility", node.visibility)


		val childrenArray = JSONArray()
		for (i in 0..<node.childCount) {
			val childNode = node.getChildAt(i)
			childrenArray.put(viewNodeToJson(childNode))
		}
		nodeJson.put("children", childrenArray)

		return nodeJson
	}
}
