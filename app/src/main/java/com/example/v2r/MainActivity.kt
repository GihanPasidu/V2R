package com.example.v2r

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.*

class MainActivity : AppCompatActivity() {

    private lateinit var v2rayExecutable: File
    private lateinit var configFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize buttons
        val startButton: Button = findViewById(R.id.startButton)
        val importButton: Button = findViewById(R.id.importButton)

        // Button listeners
        importButton.setOnClickListener {
            // Import configuration from clipboard (implement this function)
            importConfigFromClipboard()
        }

        startButton.setOnClickListener {
            val config = readConfigFromFile()
            startVPN(config)
        }

        // Prepare the V2Ray files on first run
        prepareV2RayFiles(this)
    }

    private fun importConfigFromClipboard() {
        try {
            // Implement logic to get the V2Ray config from clipboard
            val config = "V2Ray configuration from clipboard"  // Example, replace with actual clipboard data
            configFile.writeText(config)
            Toast.makeText(this, "Config imported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error importing config: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("ConfigImport", "Error importing config", e)
        }
    }

    private fun readConfigFromFile(): String {
        return try {
            val config = File(cacheDir, "config.json").readText()
            config
        } catch (e: FileNotFoundException) {
            Toast.makeText(this, "Config file not found", Toast.LENGTH_SHORT).show()
            Log.e("ConfigRead", "Config file not found", e)
            ""
        }
    }

    private fun copyAssetFile(context: Context, assetName: String, destinationPath: String) {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open(assetName)
            val outputFile = File(destinationPath)
            outputFile.parentFile?.mkdirs() // Ensure the parent directories exist
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
            inputStream.close()
            outputStream.close()

            // After copying, change the permissions for the v2ray binary
            if (assetName == "v2ray") {
                outputFile.setExecutable(true)
            }
            Log.d("FileCopy", "$assetName copied to $destinationPath")
        } catch (e: IOException) {
            Log.e("FileCopy", "Error copying $assetName: ${e.message}")
        }
    }

    private fun prepareV2RayFiles(context: Context) {
        // Copy V2Ray binary and other necessary files from assets to files directory
        val v2rayBinary = File(context.cacheDir, "v2ray")
        if (!v2rayBinary.exists()) {
            copyAssetFile(context, "v2ray", v2rayBinary.absolutePath)
        }

        // Copy other necessary files (e.g., geoip.dat, geosite.dat, config.json)
        val geoipFile = File(context.cacheDir, "geoip.dat")
        if (!geoipFile.exists()) {
            copyAssetFile(context, "geoip.dat", geoipFile.absolutePath)
        }

        val geositeFile = File(context.cacheDir, "geosite.dat")
        if (!geositeFile.exists()) {
            copyAssetFile(context, "geosite.dat", geositeFile.absolutePath)
        }

        val configFile = File(context.cacheDir, "config.json")
        if (!configFile.exists()) {
            copyAssetFile(context, "config.json", configFile.absolutePath)
        }

        this.v2rayExecutable = v2rayBinary
        this.configFile = configFile
    }

    private fun startVPN(config: String) {
        try {
            // Prepare config file
            configFile.writeText(config)

            // Start the V2Ray process with the config file
            val v2rayProcess = ProcessBuilder(
                v2rayExecutable.absolutePath,  // V2Ray binary path
                "-config", configFile.absolutePath  // Config file path
            )
                .redirectErrorStream(true)
                .start()

            // Log the process output
            val reader = BufferedReader(InputStreamReader(v2rayProcess.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                Log.d("V2Ray", line)
            }

            // Wait for the process to finish
            v2rayProcess.waitFor()
            Log.d("V2Ray", "V2Ray VPN started successfully")
            Toast.makeText(this, "VPN started successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("VPN", "Error starting VPN: ${e.message}")
            Toast.makeText(this, "Error starting VPN: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
