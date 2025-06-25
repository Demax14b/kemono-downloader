package com.example.kemono

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.*
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var urlInput: EditText
    private lateinit var downloadBtn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var statusText: TextView
    private lateinit var web: WebView

    private val downloads = mutableListOf<String>()
    private var completed = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlInput     = findViewById(R.id.urlInput)
        downloadBtn  = findViewById(R.id.downloadBtn)
        progressBar  = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        statusText   = findViewById(R.id.statusText)
        web          = findViewById(R.id.webview)

        web.settings.javaScriptEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                statusText.text = "Extracting links..."
                view.evaluateJavascript("""
                    (function() {
                      return JSON.stringify(Array.from(document.querySelectorAll('a.fileThumb')).map(a => a.href));
                    })();
                """) {
                    val clean = it.replace("\\", "").trim('"')
                    downloads.clear()
                    downloads.addAll(clean.split(",").filter { it.startsWith("http") })
                    startDownload()
                }
            }
        }

        downloadBtn.setOnClickListener {
            reset()
            val url = urlInput.text.toString()
            web.loadUrl(url)
        }
    }

    private fun startDownload() {
        if (downloads.isEmpty()) {
            statusText.text = "No files found!"
            return
        }
        val mgr = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
        val ids = mutableListOf<Long>()
        for ((i, url) in downloads.withIndex()) {
            val uri = Uri.parse(url)
            val filename = "file_$i" + uri.path?.substringAfterLast('/')
            val request = DownloadManager.Request(uri)
                .setTitle(filename)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, filename)
            val id = mgr.enqueue(request)
            ids += id
        }

        progressBar.max = downloads.size
        Thread {
            var finished = 0
            while (finished < downloads.size) {
                finished = ids.count { id ->
                    val q = DownloadManager.Query().setFilterById(id)
                    val cursor = mgr.query(q)
                    cursor.moveToFirst()
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    cursor.close()
                    status == DownloadManager.STATUS_SUCCESSFUL
                }
                completed = finished
                runOnUiThread {
                    progressBar.progress = finished
                    progressText.text = "$finished / ${downloads.size}"
                }
                Thread.sleep(500)
            }
            zipFiles()
        }.start()
    }

    private fun zipFiles() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
        val files = dir.listFiles { _, name -> name.startsWith("file_") } ?: return
        val idPart = urlInput.text.toString().trimEnd('/').substringAfterLast('/')
        val zipFile = File(dir, "KemonoDownloader${idPart}.zip")
        ZipOutputStream(zipFile.outputStream()).use { out ->
            files.forEach { file ->
                out.putNextEntry(ZipEntry(file.name))
                out.write(file.readBytes())
                out.closeEntry()
            }
        }
        runOnUiThread {
            statusText.text = "ZIP saved: ${zipFile.absolutePath}"
        }
    }

    private fun reset() {
        completed = 0
        progressBar.progress = 0
        progressText.text = "0 / 0"
        statusText.text = "Loading post..."
    }
}
