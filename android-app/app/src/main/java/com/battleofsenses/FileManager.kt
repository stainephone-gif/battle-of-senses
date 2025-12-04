package com.battleofsenses

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileManager(private val context: Context) {

    companion object {
        private const val TAG = "FileManager"
        private const val ARTWORKS_DIR = "artworks"
        private const val INTERVIEWS_DIR = "interviews"

        // Placeholder file names
        val ARTWORK_FILES = listOf(
            "01MoodMirror.jpg", "02Gadgets.jpg", "03SetHumans.jpg", "04Connection.jpg",
            "05Identification.jpg", "06Touching.jpg", "07CAMP.jpg", "08TuesdaySong.jpg",
            "09LightOfUs.jpg", "10Diary.jpg", "11SoundscapeConsole.jpg", "12Bioconstruction.jpg",
            "13Iddily.jpg", "14Inverse.jpg", "16ReadFace.jpg", "17Omniharmony.jpg",
            "18Terribletales.jpg", "20GrimasaSynth.jpg", "21LoveAgreement.jpg", "23Biohub.jpg"
        )

        val INTERVIEW_FILES = listOf(
            "interview-1.mp3", "interview-2.mp3", "interview-3.mp3", "interview-4.mp3",
            "interview-5.mp3", "interview-6.mp3", "interview-7.mp3", "interview-8.mp3",
            "interview-9.mp3", "interview-10.mp3", "interview-11.mp3", "interview-12.mp3",
            "interview-13.mp3", "interview-14.mp3", "interview-16.mp3", "interview-17.mp3",
            "interview-18.mp3", "interview-20.mp3", "interview-21.mp3", "interview-23.mp3"
        )
    }

    /**
     * Initialize files: copy placeholders from assets to internal storage if not exists
     */
    fun initializeFiles(): Boolean {
        return try {
            copyAssetsToInternalStorage(ARTWORKS_DIR, ARTWORK_FILES)
            copyAssetsToInternalStorage(INTERVIEWS_DIR, INTERVIEW_FILES)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing files", e)
            false
        }
    }

    /**
     * Copy files from assets to internal storage
     */
    private fun copyAssetsToInternalStorage(folderName: String, fileNames: List<String>) {
        val dir = File(context.filesDir, folderName)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        for (fileName in fileNames) {
            val destFile = File(dir, fileName)

            // Only copy if file doesn't exist (don't overwrite user uploads)
            if (!destFile.exists()) {
                copyAssetFile("$folderName/$fileName", destFile)
            }
        }
    }

    /**
     * Copy single asset file to destination
     */
    private fun copyAssetFile(assetPath: String, destFile: File) {
        try {
            context.assets.open(assetPath).use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "Copied asset: $assetPath to ${destFile.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Error copying asset: $assetPath", e)
        }
    }

    /**
     * Get file from internal storage
     */
    fun getFile(folderName: String, fileName: String): File {
        val dir = File(context.filesDir, folderName)
        return File(dir, fileName)
    }

    /**
     * Replace file with new content (for admin uploads)
     */
    fun replaceFile(folderName: String, fileName: String, sourceFile: File): Boolean {
        return try {
            val destFile = getFile(folderName, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            Log.d(TAG, "Replaced file: ${destFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing file: $fileName", e)
            false
        }
    }

    /**
     * Reset file to placeholder (copy from assets)
     */
    fun resetToPlaceholder(folderName: String, fileName: String): Boolean {
        return try {
            val destFile = getFile(folderName, fileName)
            if (destFile.exists()) {
                destFile.delete()
            }
            copyAssetFile("$folderName/$fileName", destFile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting file: $fileName", e)
            false
        }
    }

    /**
     * Check if file exists in internal storage
     */
    fun fileExists(folderName: String, fileName: String): Boolean {
        return getFile(folderName, fileName).exists()
    }

    /**
     * Get file path for WebView (file:// protocol)
     */
    fun getFilePathForWebView(folderName: String, fileName: String): String {
        return "file://${getFile(folderName, fileName).absolutePath}"
    }
}
