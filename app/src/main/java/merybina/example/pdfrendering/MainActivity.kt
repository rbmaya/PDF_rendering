package merybina.example.pdfrendering

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import merybina.example.pdfrendering.databinding.MainActivityBinding
import merybina.example.pdfrendering.ui.PdfRendererAdapter
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewBinding by lazy {
        MainActivityBinding.inflate(layoutInflater)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {

        }

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let(::loadAndDisplayPdf)
        }

    private val mainHandler: Handler by lazy {
        Handler(this.mainLooper)
    }

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private val pagesAdapter: PdfRendererAdapter by lazy {
        PdfRendererAdapter(renderPage = { renderPage(it) },)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(viewBinding.root)

        requestPermissions()
        setupRecyclerview()

        viewBinding.openPdfButton.setOnClickListener {
            filePickerLauncher.launch("application/pdf")
        }
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }


    private fun setupRecyclerview() = with(viewBinding) {
        recyclerView.run {
            adapter = pagesAdapter
            itemAnimator = null
        }
    }

    private fun loadAndDisplayPdf(uri: Uri) {
        fileDescriptor?.close()
        pdfRenderer?.close()

        Thread {
            try {
                val file = createTempFileFromUri(uri)
                fileDescriptor =
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)

                val pageCount = pdfRenderer?.pageCount ?: 0
                Log.d(this::class.simpleName, "Page count: $pageCount")
                val pageStubs = mutableMapOf<Int, Bitmap?>()
                for (i in 0 until pageCount) {
                    pageStubs.put(i, null)
                }
                mainHandler.post {
                    pagesAdapter.pages = pageStubs
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun renderPage(index: Int): Bitmap? {
        Log.d(this::class.simpleName, "Render page: $index")
        return try {
            val page = pdfRenderer?.openPage(index) ?: return null
            renderPage(page)
                .also {
                    page.close()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File(cacheDir, "temp_file")
        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input?.copyTo(output)
            }
        }
        return tempFile
    }

    private fun renderPage(page: PdfRenderer.Page): Bitmap {
        val density = resources.displayMetrics.densityDpi
        val pageWidth = page.width
        val pageHeight = page.height
        val width = (density / 72f * pageWidth).toInt()
        val height = (density / 72f * pageHeight).toInt()

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fileDescriptor = null
        pdfRenderer = null
    }

}
