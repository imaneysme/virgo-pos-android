package com.virgokomputer.pos

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // Standard Serial Port Profile UUID used by virtually all Bluetooth 58mm thermal printers.
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val requestRuntimePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* user can retry the action */ }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Serves the local HTML/JS/CSS through a virtual https:// domain instead of file://.
        // This is required for the in-page camera (getUserMedia, used for barcode scanning)
        // to be treated as a secure context and actually work inside the WebView.
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true      // required: the app stores data with localStorage
        webView.settings.databaseEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val hasCamera = ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasCamera) request.grant(request.resources) else request.deny()
                }
            }
        }

        webView.addJavascriptInterface(PosBridge(), "AndroidPOS")

        requestStartupPermissions()

        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    private fun requestStartupPermissions() {
        val needed = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 31) {
            needed.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestRuntimePermissions.launch(missing.toTypedArray())
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ---- Native barcode scan (ZXing) result handoff back to the web page ----
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            val code = result.contents
            runOnUiThread {
                val js = "window.onBarcodeScanned && window.onBarcodeScanned(" +
                        JSONObject.quote(code ?: "") + ")"
                webView.evaluateJavascript(js, null)
            }
        } else {
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /** Everything exposed to the web page (assets/www/index.html) as `AndroidPOS`. */
    inner class PosBridge {

        @JavascriptInterface
        fun isAvailable(): Boolean = BluetoothAdapter.getDefaultAdapter() != null

        @JavascriptInterface
        fun requestPermission() {
            runOnUiThread { requestStartupPermissions() }
        }

        /** Returns a JSON array of already-paired Bluetooth devices: [{name, address}, ...] */
        @JavascriptInterface
        fun getPairedPrinters(): String {
            val result = JSONArray()
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: return result.toString()
                if (!hasBluetoothConnectPermission()) return result.toString()
                val bonded: Set<BluetoothDevice> = adapter.bondedDevices ?: emptySet()
                for (d in bonded) {
                    val o = JSONObject()
                    o.put("name", d.name ?: "Perangkat tidak dikenal")
                    o.put("address", d.address)
                    result.put(o)
                }
            } catch (e: SecurityException) {
                // permission not granted yet — return empty list, JS will show a hint
            }
            return result.toString()
        }

        /** Connects to a paired printer over classic Bluetooth SPP and writes the receipt text. */
        @JavascriptInterface
        fun printText(address: String, text: String) {
            Thread {
                var socket: BluetoothSocket? = null
                var ok = false
                var message = ""
                try {
                    if (!hasBluetoothConnectPermission()) {
                        throw SecurityException("Izin Bluetooth belum diberikan")
                    }
                    val adapter = BluetoothAdapter.getDefaultAdapter()
                        ?: throw IllegalStateException("Perangkat ini tidak mendukung Bluetooth")
                    val device = adapter.getRemoteDevice(address)
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    adapter.cancelDiscovery()
                    socket.connect()
                    val out: OutputStream = socket.outputStream
                    out.write(byteArrayOf(0x1B, 0x40)) // ESC @  — initialize printer
                    // ISO-8859-1 keeps a 1:1 byte mapping for the ASCII text + control bytes used here.
                    out.write(text.toByteArray(Charsets.ISO_8859_1))
                    out.write(byteArrayOf(0x0A, 0x0A, 0x0A)) // feed a few lines before cut
                    out.write(byteArrayOf(0x1D, 0x56, 0x00)) // GS V 0 — full paper cut, if supported
                    out.flush()
                    ok = true
                } catch (e: Exception) {
                    message = e.message ?: "Gagal terhubung ke printer"
                } finally {
                    try { socket?.close() } catch (e: Exception) { /* ignore */ }
                }
                runOnUiThread {
                    val js = "window.onPrintResult && window.onPrintResult(" + ok +
                            ", " + JSONObject.quote(message) + ")"
                    webView.evaluateJavascript(js, null)
                }
            }.start()
        }

        /** Launches a full-screen native camera scanner (ZXing) as a fallback scan method. */
        @JavascriptInterface
        fun scanBarcode() {
            runOnUiThread {
                val integrator = IntentIntegrator(this@MainActivity)
                integrator.setOrientationLocked(false)
                integrator.setBeepEnabled(true)
                integrator.setPrompt("Arahkan kamera ke barcode")
                integrator.initiateScan()
            }
        }
    }
}
