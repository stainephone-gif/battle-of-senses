package com.battleofsenses

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast

/**
 * JavaScript интерфейс для взаимодействия веб-приложения с нативными функциями Android
 * Доступен в JavaScript как window.Android
 */
class WebAppInterface(private val context: Context) {

    companion object {
        private const val TAG = "WebAppInterface"
    }

    /**
     * Проверяет, запущено ли приложение в нативном окружении Android
     * JavaScript: Android.isNativeApp()
     * @return true если запущено в нативном приложении
     */
    @JavascriptInterface
    fun isNativeApp(): Boolean {
        return true
    }

    /**
     * Получить версию Android
     * JavaScript: Android.getAndroidVersion()
     * @return версия Android (например "13")
     */
    @JavascriptInterface
    fun getAndroidVersion(): String {
        return Build.VERSION.SDK_INT.toString()
    }

    /**
     * Получить модель устройства
     * JavaScript: Android.getDeviceModel()
     * @return модель устройства
     */
    @JavascriptInterface
    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    /**
     * Показать Toast сообщение
     * JavaScript: Android.showToast("Текст сообщения")
     * @param message текст для отображения
     */
    @JavascriptInterface
    fun showToast(message: String) {
        (context as? MainActivity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Показать длинное Toast сообщение
     * JavaScript: Android.showLongToast("Текст сообщения")
     * @param message текст для отображения
     */
    @JavascriptInterface
    fun showLongToast(message: String) {
        (context as? MainActivity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Вибрация устройства
     * JavaScript: Android.vibrate(500) // 500 миллисекунд
     * @param milliseconds длительность вибрации в миллисекундах
     */
    @JavascriptInterface
    fun vibrate(milliseconds: Long) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration error: ${e.message}")
        }
    }

    /**
     * Вибрация с паттерном
     * JavaScript: Android.vibratePattern("100,200,100,500")
     * @param pattern строка с паттерном вибрации (миллисекунды через запятую)
     */
    @JavascriptInterface
    fun vibratePattern(pattern: String) {
        try {
            val patternArray = pattern.split(",").map { it.trim().toLong() }.toLongArray()

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(patternArray, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(patternArray, -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration pattern error: ${e.message}")
        }
    }

    /**
     * Логирование в Android LogCat (для отладки)
     * JavaScript: Android.log("Debug message")
     * @param message сообщение для логирования
     */
    @JavascriptInterface
    fun log(message: String) {
        Log.d(TAG, "JS: $message")
    }

    /**
     * Логирование ошибки в Android LogCat
     * JavaScript: Android.logError("Error message")
     * @param message сообщение об ошибке
     */
    @JavascriptInterface
    fun logError(message: String) {
        Log.e(TAG, "JS Error: $message")
    }

    /**
     * Сохранить данные в SharedPreferences (постоянное хранилище)
     * JavaScript: Android.saveData("key", "value")
     * @param key ключ
     * @param value значение (строка)
     */
    @JavascriptInterface
    fun saveData(key: String, value: String) {
        try {
            val prefs = context.getSharedPreferences("BattleOfSensesPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString(key, value).apply()
            Log.d(TAG, "Data saved: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Save data error: ${e.message}")
        }
    }

    /**
     * Загрузить данные из SharedPreferences
     * JavaScript: Android.loadData("key")
     * @param key ключ
     * @return значение или пустая строка если не найдено
     */
    @JavascriptInterface
    fun loadData(key: String): String {
        return try {
            val prefs = context.getSharedPreferences("BattleOfSensesPrefs", Context.MODE_PRIVATE)
            prefs.getString(key, "") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Load data error: ${e.message}")
            ""
        }
    }

    /**
     * Удалить данные из SharedPreferences
     * JavaScript: Android.removeData("key")
     * @param key ключ для удаления
     */
    @JavascriptInterface
    fun removeData(key: String) {
        try {
            val prefs = context.getSharedPreferences("BattleOfSensesPrefs", Context.MODE_PRIVATE)
            prefs.edit().remove(key).apply()
            Log.d(TAG, "Data removed: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Remove data error: ${e.message}")
        }
    }

    /**
     * Очистить все данные из SharedPreferences
     * JavaScript: Android.clearAllData()
     */
    @JavascriptInterface
    fun clearAllData() {
        try {
            val prefs = context.getSharedPreferences("BattleOfSensesPrefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d(TAG, "All data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Clear data error: ${e.message}")
        }
    }

    /**
     * Выход из приложения
     * JavaScript: Android.exitApp()
     */
    @JavascriptInterface
    fun exitApp() {
        (context as? MainActivity)?.finish()
    }
}
