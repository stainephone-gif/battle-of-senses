package com.battleofsenses

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class AdminActivity : AppCompatActivity() {

    private lateinit var passwordSection: LinearLayout
    private lateinit var adminContent: LinearLayout
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var categoryRadioGroup: RadioGroup
    private lateinit var artworksRadio: RadioButton
    private lateinit var interviewsRadio: RadioButton
    private lateinit var fileSpinner: Spinner
    private lateinit var uploadButton: Button
    private lateinit var resetButton: Button
    private lateinit var statusText: TextView
    private lateinit var backButton: Button

    private lateinit var fileManager: FileManager
    private var selectedCategory = "artworks"
    private var selectedFile = ""

    companion object {
        private const val ADMIN_PASSWORD = "exhibition2025"  // Change this to your desired password
        private const val PICK_FILE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        fileManager = FileManager(this)

        // Initialize views
        passwordSection = findViewById(R.id.passwordSection)
        adminContent = findViewById(R.id.adminContent)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        categoryRadioGroup = findViewById(R.id.categoryRadioGroup)
        artworksRadio = findViewById(R.id.artworksRadio)
        interviewsRadio = findViewById(R.id.interviewsRadio)
        fileSpinner = findViewById(R.id.fileSpinner)
        uploadButton = findViewById(R.id.uploadButton)
        resetButton = findViewById(R.id.resetButton)
        statusText = findViewById(R.id.statusText)
        backButton = findViewById(R.id.backButton)

        setupListeners()
        updateFileList()
    }

    private fun setupListeners() {
        loginButton.setOnClickListener {
            val password = passwordInput.text.toString()
            if (password == ADMIN_PASSWORD) {
                passwordSection.visibility = View.GONE
                adminContent.visibility = View.VISIBLE
                Toast.makeText(this, "Доступ разрешён", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
                passwordInput.text.clear()
            }
        }

        categoryRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedCategory = when (checkedId) {
                R.id.artworksRadio -> "artworks"
                R.id.interviewsRadio -> "interviews"
                else -> "artworks"
            }
            updateFileList()
        }

        fileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFile = fileSpinner.selectedItem.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        uploadButton.setOnClickListener {
            if (selectedFile.isEmpty()) {
                Toast.makeText(this, "Выберите файл для замены", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = if (selectedCategory == "artworks") "image/*" else "audio/*"
            startActivityForResult(intent, PICK_FILE_REQUEST)
        }

        resetButton.setOnClickListener {
            if (selectedFile.isEmpty()) {
                Toast.makeText(this, "Выберите файл для сброса", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = fileManager.resetToPlaceholder(selectedCategory, selectedFile)
            if (success) {
                statusText.text = "Файл $selectedFile сброшен к placeholder"
                statusText.setTextColor(getColor(android.R.color.holo_green_light))
                Toast.makeText(this, "Успешно сброшено", Toast.LENGTH_SHORT).show()
            } else {
                statusText.text = "Ошибка при сбросе файла"
                statusText.setTextColor(getColor(android.R.color.holo_red_light))
                Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun updateFileList() {
        val fileList = when (selectedCategory) {
            "artworks" -> FileManager.ARTWORK_FILES
            "interviews" -> FileManager.INTERVIEW_FILES
            else -> emptyList()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fileList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fileSpinner.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                handleFileUpload(uri)
            }
        }
    }

    private fun handleFileUpload(uri: Uri) {
        try {
            // Create temporary file from URI
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, "temp_upload")

            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Replace file
            val success = fileManager.replaceFile(selectedCategory, selectedFile, tempFile)

            if (success) {
                statusText.text = "Файл $selectedFile успешно загружен"
                statusText.setTextColor(getColor(android.R.color.holo_green_light))
                Toast.makeText(this, "Успешно загружено", Toast.LENGTH_SHORT).show()
            } else {
                statusText.text = "Ошибка при загрузке файла"
                statusText.setTextColor(getColor(android.R.color.holo_red_light))
                Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }

            // Clean up
            tempFile.delete()

        } catch (e: Exception) {
            e.printStackTrace()
            statusText.text = "Ошибка: ${e.message}"
            statusText.setTextColor(getColor(android.R.color.holo_red_light))
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
