package com.atharok.btremote.data.dlna

import com.atharok.btremote.domain.entity.dlna.DlnaDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.nio.charset.StandardCharsets

class DlnaController {

    companion object {
        private const val SSDP_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val DISCOVERY_TIMEOUT_MS = 5000L
        private const val SOCKET_TIMEOUT_MS = 3000

        private const val DEVICE_TYPE_MEDIA_RENDERER = "urn:schemas-upnp-org:device:MediaRenderer:1"
        private const val SERVICE_TYPE_AV_TRANSPORT = "urn:schemas-upnp-org:service:AVTransport:1"

        private const val SOAP_ACTION_SET_AV_TRANSPORT_URI = "\"urn:schemas-upnp-org:service:AVTransport:1#SetAVTransportURI\""
        private const val SOAP_ACTION_PLAY = "\"urn:schemas-upnp-org:service:AVTransport:1#Play\""
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun discoverMediaRenderers(): List<DlnaDevice> = withContext(Dispatchers.IO) {
        val discoveredLocations = mutableSetOf<String>()
        val devices = mutableListOf<DlnaDevice>()

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = SOCKET_TIMEOUT_MS
            }

            val searchMessage = buildSsdpSearchMessage()
            val sendPacket = DatagramPacket(
                searchMessage.toByteArray(StandardCharsets.UTF_8),
                searchMessage.length,
                InetAddress.getByName(SSDP_ADDRESS),
                SSDP_PORT
            )
            socket.send(sendPacket)

            val buffer = ByteArray(1024)
            val deadline = System.currentTimeMillis() + DISCOVERY_TIMEOUT_MS

            while (System.currentTimeMillis() < deadline) {
                val receivePacket = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(receivePacket)
                } catch (_: IOException) {
                    continue
                }

                val response = String(receivePacket.data, 0, receivePacket.length, StandardCharsets.UTF_8)
                val location = parseHeaderValue(response, "LOCATION") ?: continue

                if (discoveredLocations.add(location)) {
                    val device = fetchMediaRendererDevice(location)
                    if (device != null) {
                        devices.add(device)
                    }
                }
            }
        } finally {
            socket?.close()
        }

        devices
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun castToDevice(
        device: DlnaDevice,
        streamUri: String,
        timeoutMs: Long = 10000L
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            withTimeout(timeoutMs) {
                val setUriResult = sendSoapAction(
                    controlUrl = device.controlUrl,
                    soapAction = SOAP_ACTION_SET_AV_TRANSPORT_URI,
                    body = buildSetAvTransportUriBody(streamUri)
                )
                if (setUriResult.isFailure) {
                    return@withTimeout Result.failure(setUriResult.exceptionOrNull() ?: Exception("SetAVTransportURI failed"))
                }

                val playResult = sendSoapAction(
                    controlUrl = device.controlUrl,
                    soapAction = SOAP_ACTION_PLAY,
                    body = buildPlayBody()
                )
                if (playResult.isFailure) {
                    return@withTimeout Result.failure(playResult.exceptionOrNull() ?: Exception("Play failed"))
                }

                Result.success(Unit)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IOException("Cast timeout"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSsdpSearchMessage(): String {
        return "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: 5\r\n" +
                "ST: $DEVICE_TYPE_MEDIA_RENDERER\r\n" +
                "\r\n"
    }

    private fun parseHeaderValue(response: String, headerName: String): String? {
        val regex = Regex("$headerName:\\s*(.+)", RegexOption.IGNORE_CASE)
        return regex.find(response)?.groupValues?.get(1)?.trim()
    }

    private fun fetchMediaRendererDevice(location: String): DlnaDevice? {
        return try {
            val url = URL(location)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = SOCKET_TIMEOUT_MS
                readTimeout = SOCKET_TIMEOUT_MS
                setRequestProperty("User-Agent", "BT Remote")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return null
            }

            val xml = connection.inputStream.use { it.bufferedReader().readText() }
            connection.disconnect()

            parseDeviceDescription(xml, location)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDeviceDescription(xml: String, baseUrl: String): DlnaDevice? {
        var deviceName: String? = null
        var deviceType: String? = null
        var controlUrl: String? = null
        var insideDevice = false
        var insideService = false
        var currentServiceType: String? = null
        var currentControlUrl: String? = null

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    when {
                        tagName.equals("device", ignoreCase = true) -> insideDevice = true
                        tagName.equals("service", ignoreCase = true) -> insideService = true
                    }

                    if (insideDevice && tagName.equals("friendlyName", ignoreCase = true)) {
                        deviceName = parser.nextText()
                    } else if (insideDevice && tagName.equals("deviceType", ignoreCase = true)) {
                        deviceType = parser.nextText()
                    } else if (insideService) {
                        when {
                            tagName.equals("serviceType", ignoreCase = true) -> currentServiceType = parser.nextText()
                            tagName.equals("controlURL", ignoreCase = true) -> currentControlUrl = parser.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tagName = parser.name
                    when {
                        tagName.equals("device", ignoreCase = true) -> insideDevice = false
                        tagName.equals("service", ignoreCase = true) -> {
                            if (currentServiceType == SERVICE_TYPE_AV_TRANSPORT && currentControlUrl != null) {
                                controlUrl = resolveUrl(baseUrl, currentControlUrl)
                            }
                            insideService = false
                            currentServiceType = null
                            currentControlUrl = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return if (deviceName != null && controlUrl != null && deviceType == DEVICE_TYPE_MEDIA_RENDERER) {
            DlnaDevice(name = deviceName, controlUrl = controlUrl)
        } else null
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            URL(URL(baseUrl), relativeUrl).toString()
        } catch (_: Exception) {
            relativeUrl
        }
    }

    private fun sendSoapAction(controlUrl: String, soapAction: String, body: String): Result<Unit> {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(controlUrl).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                connectTimeout = SOCKET_TIMEOUT_MS
                readTimeout = SOCKET_TIMEOUT_MS
                setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                setRequestProperty("SOAPAction", soapAction)
            }

            connection.outputStream.use { output ->
                output.write(body.toByteArray(StandardCharsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                connection.inputStream.use { it.readBytes() }
                Result.success(Unit)
            } else {
                val error = connection.errorStream?.use { it.bufferedReader().readText() } ?: "HTTP $responseCode"
                Result.failure(IOException("SOAP error: $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildSetAvTransportUriBody(uri: String): String {
        return "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>" +
                "<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<s:Body>" +
                "<u:SetAVTransportURI xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">" +
                "<InstanceID>0</InstanceID>" +
                "<CurrentURI>${escapeXml(uri)}</CurrentURI>" +
                "<CurrentURIMetaData></CurrentURIMetaData>" +
                "</u:SetAVTransportURI>" +
                "</s:Body>" +
                "</s:Envelope>"
    }

    private fun buildPlayBody(): String {
        return "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>" +
                "<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<s:Body>" +
                "<u:Play xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">" +
                "<InstanceID>0</InstanceID>" +
                "<Speed>1</Speed>" +
                "</u:Play>" +
                "</s:Body>" +
                "</s:Envelope>"
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
