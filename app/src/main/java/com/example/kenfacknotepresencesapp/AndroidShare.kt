
package com.example.myapp

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

class AndroidShare(
    private val activity: Activity
) {

    @JavascriptInterface
    fun shareJSON(json: String, filename: String) {
        activity.runOnUiThread {
            try {
                // Prevent directory traversal and invalid filenames.
                val safeName = filename
                    .substringAfterLast('/')
                    .substringAfterLast('\\')
                    .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    .ifBlank { "data.json" }
                    .let {
                        if (it.endsWith(".json", ignoreCase = true)) it
                        else "$it.json"
                    }

                // Validate that the input is valid JSON.
                JSONObject(json)

                val directory = File(
                    activity.cacheDir,
                    "shared_json"
                ).apply {
                    mkdirs()
                }

                val file = File(directory, safeName)
                file.writeText(json, Charsets.UTF_8)

                val uri = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.fileprovider",
                    file
                )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(
                        activity.contentResolver,
                        safeName,
                        uri
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                activity.startActivity(
                    Intent.createChooser(sendIntent, "Share JSON file")
                )

            } catch (e: Exception) {
                Toast.makeText(
                    activity,
                    "Unable to share JSON: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
