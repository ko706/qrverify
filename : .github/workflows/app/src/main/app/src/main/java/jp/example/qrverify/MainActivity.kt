package jp.example.qrverify

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private var nextScanIsMaster = false

    private val PREFS = "qr_prefs"
    private val KEY_MASTER = "master_code"

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScanInternal()
        } else {
            Toast.makeText(this, "カメラ権限が必要です", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnMaster = findViewById<Button>(R.id.btnMaster)
        val btnVerify = findViewById<Button>(R.id.btnVerify)
        tvResult = findViewById(R.id.tvResult)

        btnMaster.setOnClickListener {
            nextScanIsMaster = true
            ensureCameraPermissionThenScan()
        }
        btnVerify.setOnClickListener {
            nextScanIsMaster = false
            ensureCameraPermissionThenScan()
        }
    }

    private fun ensureCameraPermissionThenScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startScanInternal()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanInternal() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt(if (nextScanIsMaster) "マスタ QR を読み取ってください" else "照合用 QR を読み取ってください")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "読み取りがキャンセルされました", Toast.LENGTH_SHORT).show()
            } else {
                handleScanResult(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleScanResult(contents: String) {
        if (contents.length != 27) {
            tvResult.text = "読み取ったデータの長さが27桁ではありません (${contents.length}桁)\n内容：$contents"
            return
        }
        val sub = contents.substring(3, 9)
        if (nextScanIsMaster) {
            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_MASTER, sub).apply()
            tvResult.text = "マスタとして保存しました: $sub"
        } else {
            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val master = prefs.getString(KEY_MASTER, null)
            if (master == null) {
                tvResult.text = "マスタが未設定です。先にマスタ読み取りを行ってください。"
                return
            }
            if (master == sub) {
                tvResult.text = "照合成功（一致）\nマスタ: $master\n今回: $sub"
            } else {
                tvResult.text = "照合失敗（不一致）\nマスタ: $master\n今回: $sub"
            }
        }
    }
}
