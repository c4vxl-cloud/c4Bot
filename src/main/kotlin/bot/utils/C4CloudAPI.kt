package de.c4vxl.bot.utils

import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalEncodingApi::class)
class C4CloudAPI(val apiKey: String) {
    companion object {
        var apiURL: URL = URL("https://cloud.c4vxl.de/cloud/api/")
    }

    val uuid: String = if (apiKey.length > 2) Base64.decode(apiKey).decodeToString().split("<?>")[0] else "unknown"
    val accountInformation: MutableMap<String, String> = this.sendRequest("account_info", mutableMapOf("uuid" to this.uuid))["content"] as? MutableMap<String, String> ?: mutableMapOf()
    val profilePicURL: String = "${apiURL.protocol}://${apiURL.host}/cloud/${accountInformation["profilepicture"]}"
    val username: String? = accountInformation["username"]
    val status: String? = accountInformation["status"]
    val isValidKey: Boolean = this.sendRequest("account_is_valid")["success"] as? Boolean ?: false


    fun sendRequest(request: String, vars: MutableMap<Any, Any> = mutableMapOf()): MutableMap<Any, Any> {
        val responseString: String = with(apiURL.openConnection() as HttpURLConnection) {
            useCaches = false
            doInput = true
            doOutput = true
            requestMethod = "POST"
            setRequestProperty("Content-Type", "multipart/form-data; boundary=*****")

            DataOutputStream(outputStream).use {
                vars["request"] = request
                vars["api_key"] = apiKey

                vars.forEach { (key, value) ->
                    it.writeBytes("--*****\r\n")

                    when(value) {
                        is File -> {
                            it.writeBytes("Content-Disposition: form-data; name=\"$key\"; filename=\"$value\"\r\n")
                            it.writeBytes("\r\n")
                            value.inputStream().transferTo(it)
                            it.writeBytes("\r\n")
                        }

                        else -> {
                            it.writeBytes("Content-Disposition: form-data; name=\"$key\"\r\n")
                            it.writeBytes("\r\n")
                            it.writeBytes("$value\r\n")
                        }
                    }
                }

                it.writeBytes("--*****--\r\n")

                it.flush()
                it.close()
            }

            inputStream.bufferedReader().use { it.readText() }
        }

        return mutableMapOf<Any, Any>().apply {
            JSONObject(responseString.asJSON).let {
                it.keySet().forEach { key ->
                    this[key] = (it.get(key) as? JSONObject)?.toMap()?.toMutableMap() ?: it.get(key)
                }
            }
        }
    }

    private val String.asJSON: String get() {
        val start = this.indexOf("{")
        val end = this.lastIndexOf("}")

        if (end == -1 || start == -1) return "{\"content\": \"$this\"}"

        return this.substring(start, end + 1)
    }
}