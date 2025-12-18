package com.example.hfwproxyinstall

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    private lateinit var txtLogs: TextView
    private lateinit var txtIp: TextView
    private lateinit var txtPort: TextView
    private lateinit var txtStatus: TextView
    private lateinit var btnStartStop: Button
    private lateinit var btnExportLogs: Button
    private lateinit var scrollView: ScrollView
    private lateinit var logReceiver: BroadcastReceiver

    // URLs para la funcionalidad de actualización
    private val GITHUB_API_URL = "https://api.github.com/repos/kizeo0/HFW-Proxy-Install/releases/latest"
    private val GITHUB_RELEASES_URL = "https://github.com/kizeo0/HFW-Proxy-Install/releases"

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(txtLogs.text.toString().toByteArray())
                    Toast.makeText(this, getString(R.string.logs_saved), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "${getString(R.string.error_msg)} ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica el tema ANTES de todo
        applyThemeFromPrefs()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Pedir permisos de notificación (Android 13+)
        checkNotificationPermissions()

        val toolbar = findViewById<Toolbar>(R.id.myToolbar)
        setSupportActionBar(toolbar)

        txtLogs = findViewById(R.id.txtLogs)
        txtIp = findViewById(R.id.txtIp)
        txtPort = findViewById(R.id.txtPort)
        txtStatus = findViewById(R.id.txtStatus)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnExportLogs = findViewById(R.id.btnExportLogs)
        scrollView = findViewById(R.id.scrollView)

        txtIp.text = getLocalIpAddress()

        btnStartStop.setOnClickListener {
            if (ProxyService.serviceRunning) {
                stopProxyService()
            } else {
                startProxyService()
            }
        }

        btnExportLogs.setOnClickListener {
            createDocumentLauncher.launch("ps3_proxy_log.txt")
        }

        setupLogReceiver()
    }

    override fun onResume() {
        super.onResume()
        updateUiState(ProxyService.serviceRunning)

        // Cargar todo el historial de logs desde el buffer del servicio
        txtLogs.text = getString(R.string.logs_default) + ProxyService.logBuffer.toString()
        // Scroll al final
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }

        val filter = IntentFilter(ProxyService.ACTION_LOG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(logReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(logReceiver) } catch (e: Exception) {}
    }

    private fun checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    // --- LÓGICA DE ACTUALIZACIÓN ---
    private fun checkForUpdates() {
        Toast.makeText(this, getString(R.string.update_checking), Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val jsonObject = JSONObject(response.toString())
                    // Asume que el tag de GitHub es el nombre de la versión (ej: "v1.1" o "1.1")
                    val latestVersionTag = jsonObject.optString("tag_name", "").trimStart('v', 'V')

                    // Obtener versión actual de la App (CORREGIDO AQUI ABAJO)
                    val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName?.trimStart('v', 'V') ?: ""

                    runOnUiThread {
                        if (latestVersionTag.isNotEmpty() && latestVersionTag > currentVersion) {
                            showUpdateDialog(latestVersionTag)
                        } else {
                            Toast.makeText(this, getString(R.string.update_not_available), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.update_error), Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "${getString(R.string.update_error)}: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showUpdateDialog(newVersion: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_available_title))
            .setMessage(getString(R.string.update_available_msg, newVersion))
            .setPositiveButton(getString(R.string.btn_download)) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASES_URL))
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- FUNCIONES DE SERVICIO, UI y TEMAS ---

    private fun startProxyService() {
        // Limpiar logs en UI visualmente
        txtLogs.text = getString(R.string.logs_default)
        ProxyService.logBuffer.setLength(0) // Limpia el buffer estático

        val intent = Intent(this, ProxyService::class.java)
        intent.action = ProxyService.ACTION_START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateUiState(true)
    }

    private fun stopProxyService() {
        val intent = Intent(this, ProxyService::class.java)
        intent.action = ProxyService.ACTION_STOP
        startService(intent)
        updateUiState(false)
    }

    private fun updateUiState(running: Boolean) {
        if (running) {
            txtStatus.text = getString(R.string.status_online)
            txtStatus.setTextColor(ContextCompat.getColor(this, R.color.status_online))
            btnStartStop.text = getString(R.string.btn_stop)
            btnStartStop.background.setTint(ContextCompat.getColor(this, R.color.btn_stop))
        } else {
            txtStatus.text = getString(R.string.status_offline)
            txtStatus.setTextColor(ContextCompat.getColor(this, R.color.status_offline))
            btnStartStop.text = getString(R.string.btn_start)
            btnStartStop.background.setTint(ContextCompat.getColor(this, R.color.btn_start))
        }
    }

    private fun setupLogReceiver() {
        logReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val msg = intent?.getStringExtra(ProxyService.EXTRA_LOG_MSG)
                if (msg != null) {
                    txtLogs.append("\n$msg")
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            }
        }
    }

    private fun applyThemeFromPrefs() {
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        // Por defecto: false (Modo Claro)
        val isDarkMode = sharedPref.getBoolean("DARK_MODE", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        menu.findItem(R.id.action_dark_mode).isChecked = sharedPref.getBoolean("DARK_MODE", false)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_tutorial -> { showDialog(getString(R.string.menu_tutorial), getString(R.string.tutorial_content)); true }
            R.id.action_update -> { checkForUpdates(); true } // Ejecuta la comprobación
            R.id.action_credits -> { showDialog(getString(R.string.menu_credits), getString(R.string.credits_content)); true }
            R.id.action_youtube -> {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@kizeo0/videos"))) } catch (e: Exception) {}
                true
            }
            R.id.action_dark_mode -> {
                val isChecked = !item.isChecked
                item.isChecked = isChecked
                getSharedPreferences("AppSettings", Context.MODE_PRIVATE).edit().putBoolean("DARK_MODE", isChecked).apply()
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDialog(title: String, content: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
        val msgView = dialog.findViewById<TextView>(android.R.id.message)
        if (msgView != null) {
            Linkify.addLinks(msgView, Linkify.WEB_URLS)
            msgView.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress ?: "ERROR"
                    }
                }
            }
        } catch (ex: Exception) { }
        return "127.0.0.1"
    }
}