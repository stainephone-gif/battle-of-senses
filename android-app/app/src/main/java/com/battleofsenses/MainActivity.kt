package com.battleofsenses

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var audioManager: AudioManager
    private lateinit var fileManager: FileManager
    private lateinit var gestureDetector: GestureDetectorCompat
    private var isHeadphonesConnected = false
    private var tapCount = 0
    private val tapHandler = Handler(Looper.getMainLooper())

    private val audioCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            super.onAudioDevicesAdded(addedDevices)

            // Проверяем, добавлены ли наушники
            val hasHeadphones = addedDevices.any { device ->
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }

            if (hasHeadphones && !isHeadphonesConnected) {
                isHeadphonesConnected = true
                notifyWebViewHeadphonesConnected()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            super.onAudioDevicesRemoved(removedDevices)

            // Проверяем, удалены ли наушники
            val removedHeadphones = removedDevices.any { device ->
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }

            if (removedHeadphones) {
                // Проверяем, остались ли еще какие-то наушники подключенными
                val stillHasHeadphones = checkCurrentHeadphonesState()
                if (!stillHasHeadphones && isHeadphonesConnected) {
                    isHeadphonesConnected = false
                    notifyWebViewHeadphonesDisconnected()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Включаем полноэкранный режим (immersive mode)
        setupImmersiveMode()

        // Инициализация FileManager
        fileManager = FileManager(this)
        initializeFilesAsync()

        // Инициализация AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Инициализация WebView
        webView = findViewById(R.id.webView)
        setupWebView()

        // Setup gesture detector for admin access (triple tap)
        setupGestureDetector()

        // Регистрация audio callback
        audioManager.registerAudioDeviceCallback(audioCallback, Handler(Looper.getMainLooper()))

        // Проверка текущего состояния наушников при запуске
        isHeadphonesConnected = checkCurrentHeadphonesState()

        // Загрузка веб-приложения
        webView.loadUrl("file:///android_asset/index.html")

        // Уведомляем веб-приложение о начальном состоянии наушников
        if (isHeadphonesConnected) {
            // Небольшая задержка для гарантии загрузки страницы
            Handler(Looper.getMainLooper()).postDelayed({
                notifyWebViewHeadphonesConnected()
            }, 500)
        }
    }

    /**
     * Асинхронная инициализация файлов
     */
    private fun initializeFilesAsync() {
        Thread {
            val success = fileManager.initializeFiles()
            runOnUiThread {
                if (!success) {
                    Toast.makeText(this, "Ошибка инициализации файлов", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /**
     * Настройка детектора жестов для входа в админку (тройное нажатие)
     */
    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                tapCount++

                if (tapCount == 1) {
                    // Reset count after 2 seconds
                    tapHandler.postDelayed({
                        tapCount = 0
                    }, 2000)
                }

                if (tapCount == 3) {
                    // Triple tap detected - open admin
                    tapCount = 0
                    tapHandler.removeCallbacksAndMessages(null)
                    openAdminPanel()
                    return true
                }

                return false
            }
        })
    }

    /**
     * Открыть админ-панель
     */
    private fun openAdminPanel() {
        val intent = Intent(this, AdminActivity::class.java)
        startActivity(intent)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun setupWebView() {
        webView.apply {
            // Включение JavaScript
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true

            // Настройки для корректного отображения
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false

            // Кэширование
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            // Note: setAppCacheEnabled deprecated and removed in newer Android versions

            // Поддержка мультимедиа
            settings.mediaPlaybackRequiresUserGesture = false

            // WebViewClient для навигации внутри приложения
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    // Все ссылки открываем внутри WebView
                    return false
                }
            }

            // WebChromeClient для поддержки JS console и alerts
            webChromeClient = WebChromeClient()

            // Добавление JavaScript интерфейса
            addJavascriptInterface(WebAppInterface(this@MainActivity), "Android")
        }
    }

    private fun setupImmersiveMode() {
        // Скрываем системные панели для полноэкранного режима
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Обработка потери фокуса (возврат в immersive mode)
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    /**
     * Проверяет текущее состояние аудио устройств
     * @return true если наушники подключены
     */
    private fun checkCurrentHeadphonesState(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { device ->
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            device.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
    }

    /**
     * Уведомляет WebView о подключении наушников
     */
    private fun notifyWebViewHeadphonesConnected() {
        runOnUiThread {
            webView.evaluateJavascript(
                "if (typeof window.onNativeHeadphonesConnected === 'function') { window.onNativeHeadphonesConnected(); }",
                null
            )
        }
    }

    /**
     * Уведомляет WebView об отключении наушников
     */
    private fun notifyWebViewHeadphonesDisconnected() {
        runOnUiThread {
            webView.evaluateJavascript(
                "if (typeof window.onNativeHeadphonesDisconnected === 'function') { window.onNativeHeadphonesDisconnected(); }",
                null
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Восстанавливаем immersive mode при возврате в приложение
        setupImmersiveMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Отписываемся от audio callback
        audioManager.unregisterAudioDeviceCallback(audioCallback)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
