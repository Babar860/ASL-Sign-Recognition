package com.example.signassistap

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.signassistap.utils.FileUploadUtils
import com.example.signassistap.viewmodel.VideoViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.signassistap.utils.UserSession

class CameraActivity : AppCompatActivity() {

    private lateinit var videoPickerLauncher: ActivityResultLauncher<String>
    private var selectedVideoUri: Uri? = null

    private val videoViewModel = VideoViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnPick = findViewById<Button>(R.id.btnPickVideo)
        val btnUpload = findViewById<Button>(R.id.btnUpload)
        val txtSelected = findViewById<TextView>(R.id.txtSelected)
        val txtResult = findViewById<TextView>(R.id.txtResult)
        val edtUserId = findViewById<EditText>(R.id.edtUserId)
        val edtDuration = findViewById<EditText>(R.id.edtDuration)

        // ✅ init picker
        videoPickerLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                selectedVideoUri = uri
                if (uri != null) {
                    txtSelected.text = "Selected: $uri"
                    btnUpload.isEnabled = true
                    Toast.makeText(this, "Video selected", Toast.LENGTH_SHORT).show()
                } else {
                    txtSelected.text = "No video selected"
                    btnUpload.isEnabled = false
                }
            }

        // ✅ pick click
        btnPick.setOnClickListener {
            videoPickerLauncher.launch("video/*")
        }

        // ✅ observe result
        lifecycleScope.launch {
            videoViewModel.uploadResult.collectLatest { res ->
                if (res != null) txtResult.text = res
                Toast.makeText(this@CameraActivity, res, Toast.LENGTH_LONG).show()
            }
        }
        lifecycleScope.launch {
            videoViewModel.isUploading.collectLatest { uploading ->
                btnUpload.isEnabled = (selectedVideoUri != null) && !uploading
                btnUpload.text = if (uploading) "Uploading..." else "Upload"
            }
        }

        // ✅ upload click
        btnUpload.setOnClickListener {
            val uri = selectedVideoUri
            if (uri == null) {
                Toast.makeText(this, "Please select a video first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = UserSession.getUserId(this)?.trim()
            if (userId.isNullOrEmpty()) {
                Toast.makeText(this, "User not logged in (UserId missing in session)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val durationSeconds = edtDuration.text.toString().trim().toIntOrNull() ?: 0

            try {
                // ✅ NEW: upload start indicator
                Toast.makeText(this, "Uploading started...", Toast.LENGTH_SHORT).show()
                txtResult.text = "Uploading & analyzing video..."
                val filePart = FileUploadUtils.uriToVideoPart(this, uri)

                videoViewModel.uploadVideo(
                    filePart = filePart,
                    userId = userId,
                    durationSeconds = durationSeconds
                )
            } catch (e: Exception) {
                Toast.makeText(this, "File error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}