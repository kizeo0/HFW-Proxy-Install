package com.example.hfwproxyinstall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class ProxyService : Service() {

    private var isRunning = false
    private var serverSocket: ServerSocket? = null
    private val threadPool = Executors.newCachedThreadPool()

    // Configuración
    private val SERVER_PORT = 8080
    private val REDIRECT_URL = "http://hidden-firefly-bfb6.kizeocloud.workers.dev/ps3-updatelist.txt"
    private val CHANNEL_ID = "ProxyChannel"

    companion object {
        const val ACTION_START = "START_SERVICE"
        const val ACTION_STOP = "STOP_SERVICE"
        const val ACTION_LOG = "LOG_UPDATE"
        const val EXTRA_LOG_MSG = "log_msg"

        var serviceRunning = false

        // MEMORIA DE LOGS: Para que no se borren al minimizar
        var logBuffer = StringBuilder()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                if (!isRunning) {
                    // Limpiamos logs viejos al iniciar de cero
                    logBuffer.setLength(0)
                    createNotificationChannel()
                    startForeground(1, buildNotification())
                    startServer()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    private fun startServer() {
        isRunning = true
        serviceRunning = true
        broadcastLog("========================================")
        broadcastLog("        PS3 PROXY - SERVER ON           ")
        broadcastLog("========================================")
        broadcastLog("Puerto: $SERVER_PORT")

        Thread {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                while (isRunning) {
                    val client = serverSocket?.accept()
                    if (client != null) {
                        client.soTimeout = 15000
                        threadPool.execute { handleClient(client) }
                    }
                }
            } catch (e: Exception) {
                if (isRunning) broadcastLog("[!] Error socket: ${e.message}")
            }
        }.start()
    }

    private fun stopServer() {
        isRunning = false
        serviceRunning = false
        try { serverSocket?.close() } catch (e: Exception) { }
        broadcastLog("\n[!] SERVIDOR DETENIDO")
    }

    private fun handleClient(client: Socket) {
        try {
            val input = BufferedReader(InputStreamReader(client.getInputStream()))
            val output = client.getOutputStream()

            val requestLine = input.readLine()
            if (requestLine.isNullOrEmpty()) {
                client.close()
                return
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val urlString = parts[1]

            // Leer Headers
            val clientHeaders = HashMap<String, String>()
            var line = input.readLine()
            while (!line.isNullOrEmpty()) {
                val split = line.split(":", limit = 2)
                if (split.size == 2) {
                    clientHeaders[split[0].trim()] = split[1].trim()
                }
                line = input.readLine()
            }

            if (method == "CONNECT") {
                output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                output.flush()
                client.close()
                return
            }

            val isUpdateList = urlString.contains("ps3-updatelist.txt")
            val isMyCustomLink = urlString.contains("kizeocloud") || urlString.contains("workers.dev")

            if (isUpdateList && !isMyCustomLink) {
                broadcastLog("\n[!] INTERCEPTADO: ${client.inetAddress.hostAddress}")
                broadcastLog(">>> REDIRIGIENDO A HFW...")

                val response = "HTTP/1.1 302 Found\r\n" +
                        "Location: $REDIRECT_URL\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(response.toByteArray())
                output.flush()
                client.close()
            } else {
                // Si el log es muy extenso puede causar lag, pero aquí lo dejamos para propósitos de depuración.
                // broadcastLog("Passthrough: $urlString")
                proxyPassThrough(urlString, method, clientHeaders, output)
                client.close()
            }

        } catch (e: Exception) {
            try { client.close() } catch (ex: Exception){}
        }
    }

    private fun proxyPassThrough(urlString: String, method: String, headers: Map<String, String>, clientOutput: OutputStream) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            for ((k, v) in headers) {
                if (!k.equals("Proxy-Connection", true)) {
                    connection.setRequestProperty(k, v)
                }
            }

            val responseCode = connection.responseCode
            val responseMsg = connection.responseMessage
            val statusLine = "HTTP/1.1 $responseCode $responseMsg\r\n"
            clientOutput.write(statusLine.toByteArray())

            for ((key, values) in connection.headerFields) {
                if (key != null) {
                    for (value in values) {
                        if (!key.equals("Transfer-Encoding", true) && !key.equals("Connection", true)) {
                            clientOutput.write("$key: $value\r\n".toByteArray())
                        }
                    }
                }
            }
            clientOutput.write("Connection: close\r\n".toByteArray())
            clientOutput.write("\r\n".toByteArray())

            if (method == "HEAD") return

            val remoteInput = try { connection.inputStream } catch (e: Exception) { connection.errorStream }
            remoteInput?.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    clientOutput.write(buffer, 0, bytesRead)
                }
            }
            clientOutput.flush()
        } catch (e: Exception) {
            // Puedes añadir un log de fallo aquí si es necesario
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, ProxyService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_desc))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, getString(R.string.btn_stop), stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "HFW Proxy Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    // --- CORRECCIÓN DEL LOG: Uso de intent.setPackage(packageName) ---
    private fun broadcastLog(msg: String) {
        // 1. Guardar en memoria (Buffer)
        logBuffer.append("\n$msg")

        // 2. Enviar a la UI
        val intent = Intent(ACTION_LOG)
        intent.putExtra(EXTRA_LOG_MSG, msg)

        // Esto fuerza la entrega del intent a tu propia aplicación.
        intent.setPackage(packageName)

        sendBroadcast(intent)
    }
}