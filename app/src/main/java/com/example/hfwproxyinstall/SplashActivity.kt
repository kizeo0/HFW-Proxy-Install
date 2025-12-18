package com.example.hfwproxyinstall

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Evitar que el splash se ponga encima de la Main Activity al volver del Home
        if (!isTaskRoot) {
            finish()
            return
        }

        // 2. Si el servicio de Proxy ya está corriendo, saltamos la espera
        if (ProxyService.serviceRunning) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()

        // Espera normal de 3 segundos si es la primera vez
        Handler(Looper.getMainLooper()).postDelayed({
            goToMain()
        }, 3000)
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}