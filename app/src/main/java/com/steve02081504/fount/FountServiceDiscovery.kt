package com.steve02081504.fount

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.net.NetworkInterface
import java.net.Inet4Address
import androidx.core.content.edit


const val DEFAULT_FOUNT_PORT = 8931
private const val TAG = "FountServiceDiscovery" // 用于日志记录的标签

fun getLocalIpAddress(): String? {
	try {
		val networkInterfaces = NetworkInterface.getNetworkInterfaces()
		for (networkInterface in networkInterfaces) {
			val inetAddresses = networkInterface.inetAddresses
			for (inetAddress in inetAddresses) {
				if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
					return inetAddress.hostAddress
				}
			}
		}
	} catch (e: Exception) {
		e.printStackTrace()
	}
	return null
}

object FountServiceDiscovery {  // 使用 object 创建单例
	private val client = OkHttpClient.Builder()
		.connectTimeout(500, TimeUnit.MILLISECONDS) // 设置连接超时
		.readTimeout(500, TimeUnit.MILLISECONDS) // 设置读取超时
		.build()

	// 验证 IPv4 地址
	private fun isValidIPv4Address(ip: String): Boolean {
		Log.d(TAG, "[isValidIPv4Address] Validating IP: $ip")
		val regex = """^(\d{1,3}\.){3}\d{1,3}$""".toRegex()
		val isValid = regex.matches(ip) && ip.split(".").all { it.toInt() in 0..255 }
		Log.d(TAG, "[isValidIPv4Address] IP $ip is valid: $isValid")
		return isValid
	}

	// 从 URL 字符串中提取 IP 地址和端口号
	private fun extractIpAndPortFromUrl(urlString: String): Pair<String, Int>? {
		Log.d(TAG, "[extractIpAndPortFromUrl] Extracting IP and port from URL: $urlString")
		return try {
			val url = urlString.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL")
			val ip = url.host
			val port = url.port.let { if (it != -1) it else DEFAULT_FOUNT_PORT }
			Log.d(TAG, "[extractIpAndPortFromUrl] Extracted IP: $ip, Port: $port")

			Pair(ip, port)
		} catch (error: Exception) {
			Log.e(TAG, "[extractIpAndPortFromUrl] Error extracting from $urlString: ${error.message}")
			null
		}
	}

	// 测试 Fount 服务是否可用
	suspend fun isFountServiceAvailable(host: String): Boolean = withContext(Dispatchers.IO) {
		try {
			val url = host.toHttpUrlOrNull()?.newBuilder()?.addPathSegment("api")?.addPathSegment("ping")?.build()
				?: return@withContext false

			val request = Request.Builder().url(url).get().build()
			val response: Response = client.newCall(request).execute()

			if (!response.isSuccessful) return@withContext false

			val responseBody = response.body?.string() ?: return@withContext false
			val data = JSONObject(responseBody)
			if (data.optString("cilent_name") != "fount") return@withContext false

			Log.d(TAG, "[isFountServiceAvailable] Fount service at $host is available.")
			true
		} catch (e: Exception) {
			Log.d(TAG, "isFountServiceAvailable failed with exception: ${e.message}")
			false // 任何错误都表示不可用
		}
	}


	// 扫描本地网络以查找 Fount 服务
	private suspend fun scanLocalNetworkForFount(baseIP: String, port: Int): String? =
		withContext(Dispatchers.IO) {
			Log.d(TAG, "[scanLocalNetworkForFount] Scanning with base IP: $baseIP, Port: $port")
			val batchSize = 8
			for (i in 0..255 step batchSize) {
				val deferreds = (0 until batchSize).mapNotNull { j ->
					if (i + j <= 255) {
						val ip = baseIP.replace(Regex("""\.\d+$"""), ".${i + j}")
						val host = "http://$ip:$port"
						async {
							if (isFountServiceAvailable(host)) host else null
						}
					} else null
				}
				val results = deferreds.awaitAll().filterNotNull() // awaitAll 并过滤掉 null 结果
				if (results.isNotEmpty()) {
					Log.i(TAG, "[scanLocalNetworkForFount] Fount service found at: ${results.first()}")
					return@withContext results.first() // 返回找到的第一个
				}

			}
			Log.w(TAG, "[scanLocalNetworkForFount] Fount service not found on $baseIP, Port: $port")
			null
		}

	// 在 IPv4 网络上映射 Fount 主机
	private suspend fun mapFountHostOnIPv4(hostUrl: String): String? = withContext(Dispatchers.IO) {
		Log.d(TAG, "[mapFountHostOnIPv4] Mapping Fount host on IPv4 for URL: $hostUrl")

		val (ip, port) = extractIpAndPortFromUrl(hostUrl) ?: return@withContext null

		var foundHost = scanLocalNetworkForFount(ip, port)
		if (foundHost != null) return@withContext foundHost

		if (port != DEFAULT_FOUNT_PORT) {
			Log.d(TAG, "[mapFountHostOnIPv4] Trying default port $DEFAULT_FOUNT_PORT")
			foundHost = scanLocalNetworkForFount(ip, DEFAULT_FOUNT_PORT)
			if (foundHost != null) return@withContext foundHost
		}
		Log.w(TAG, "[mapFountHostOnIPv4] Fount service not found for $hostUrl")
		null
	}


	suspend fun mappingFountHostUrl(hostUrl: String): String = withContext(Dispatchers.IO) {

		Log.d(
			TAG,
			"[mappingFountHostUrl] Attempting to map Fount host URL. Initial hostUrl: $hostUrl"
		)

		if (isFountServiceAvailable("http://localhost:$DEFAULT_FOUNT_PORT")) {
			Log.i(TAG, "[mappingFountHostUrl] Fount service is available at localhost")
			return@withContext "http://localhost:$DEFAULT_FOUNT_PORT"
		}

		if (isFountServiceAvailable("http://10.0.2.2:$DEFAULT_FOUNT_PORT")) {
			Log.i(TAG, "[mappingFountHostUrl] Fount service is available at 10.0.2.2")
			return@withContext "http://10.0.2.2:$DEFAULT_FOUNT_PORT"
		}
		// 新增：获取本机IP并扫描
		val localIp = getLocalIpAddress()
		if (localIp != null) {
			val baseIp = localIp.substringBeforeLast(".") + ".0"  // 获取本机IP的前三段
			Log.i(TAG, "[mappingFountHostUrl] Trying local IP range: $baseIp")
			val localIpRangeResult = mapFountHostOnIPv4("http://$baseIp:$DEFAULT_FOUNT_PORT") // 使用mapFountHostOnIPv4进行扫描
			if (localIpRangeResult != null) {
				Log.i(TAG, "[mappingFountHostUrl] Fount service is available at localIp $localIpRangeResult")
				return@withContext localIpRangeResult
			}
		}


		if (isFountServiceAvailable(hostUrl)) {
			Log.i(TAG, "[mappingFountHostUrl] Fount service is available at provided hostUrl: $hostUrl")
			return@withContext hostUrl
		}


		if (isValidIPv4Address(hostUrl)) {
			Log.d(TAG, "[mappingFountHostUrl] hostUrl is a valid IPv4 address. Attempting to map.")
			val result = mapFountHostOnIPv4(hostUrl)
			if (result != null) {
				Log.i(TAG, "[mappingFountHostUrl] Fount service found via IPv4 mapping: $result")
				return@withContext result
			}
		}

		Log.d(TAG, "[mappingFountHostUrl] hostUrl is not valid. Trying common hosts.")
		val commonHosts = listOf(
			"http://192.168.1.0:$DEFAULT_FOUNT_PORT",
			"http://192.168.0.0:$DEFAULT_FOUNT_PORT",
			"http://192.168.2.0:$DEFAULT_FOUNT_PORT",
			"http://192.168.3.0:$DEFAULT_FOUNT_PORT",
			"http://10.0.0.0:$DEFAULT_FOUNT_PORT",
			"http://10.1.1.0:$DEFAULT_FOUNT_PORT",
			"http://172.16.0.0:$DEFAULT_FOUNT_PORT",
			"http://172.31.0.0:$DEFAULT_FOUNT_PORT",
			"http://192.168.4.0:$DEFAULT_FOUNT_PORT",
			"http://192.168.5.0:$DEFAULT_FOUNT_PORT",
			"http://192.168.6.0:$DEFAULT_FOUNT_PORT",
			"http://192.168.7.0:$DEFAULT_FOUNT_PORT"
		)
		for (commonHost in commonHosts) {
			Log.d(TAG, "[mappingFountHostUrl] Trying common host: $commonHost")
			val result = mapFountHostOnIPv4(commonHost)
			if (result != null) {
				Log.i(TAG, "[mappingFountHostUrl] Fount service found via common host: $result")
				return@withContext result
			}
		}


		Log.w(
			TAG,
			"[mappingFountHostUrl] Could not determine Fount host URL. Returning initial hostUrl: $hostUrl"
		)
		hostUrl // 即使找不到也返回原始值
	}
	suspend fun getFountHostUrl(
		sharedPreferences: SharedPreferences,
		hostUrl: String? = null
	): String = withContext(Dispatchers.IO) {
		// 从 SharedPreferences 或传入参数中获取 hostUrl
		val initialHostUrl = hostUrl
			?: sharedPreferences.getString("fountHostUrl", null)
			?: "" // 默认值为空字符串

		val result = mappingFountHostUrl(initialHostUrl)
		sharedPreferences.edit() { putString("fountHostUrl", result) } // 异步存储
		result
	}
}
