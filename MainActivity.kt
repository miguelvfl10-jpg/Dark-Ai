package com.darkedit.ai

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import java.io.File

class MainActivity : ComponentActivity() {
    private var selectedVideo: Uri? = null
    private lateinit var status: TextView
    private lateinit var preview: VideoView
    private lateinit var exportButton: Button

    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedVideo = uri
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            preview.setVideoURI(uri)
            preview.start()
            status.text = "Vídeo carregado. Escolha uma ferramenta."
            exportButton.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 16, 20)
        window.navigationBarColor = Color.rgb(16, 16, 20)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 22)
            setBackgroundColor(Color.rgb(16, 16, 20))
        }
        fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val title = text("DARK EDIT AI", 27f, Color.rgb(255, 55, 65), true)
        root.addView(title, LinearLayout.LayoutParams(-1, 58))
        root.addView(text("EDITE SEUS VÍDEOS COM IA", 12f, Color.LTGRAY), LinearLayout.LayoutParams(-1, 30))

        preview = VideoView(this).apply { setBackgroundColor(Color.rgb(32, 32, 38)) }
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 16; bottomMargin = 16 })

        val choose = makeButton("＋  IMPORTAR VÍDEO") { pickVideo.launch(arrayOf("video/*")) }
        root.addView(choose, buttonParams())
        val reverse = makeButton("↶  REVERSO AUTOMÁTICO") {
            Toast.makeText(this, "O motor de reverso está previsto para a próxima versão.", Toast.LENGTH_LONG).show()
        }
        root.addView(reverse, buttonParams())
        val effects = makeButton("✦  EFEITOS E EDIÇÃO AUTOMÁTICA") {
            Toast.makeText(this, "Os efeitos automáticos estão em desenvolvimento.", Toast.LENGTH_LONG).show()
        }
        root.addView(effects, buttonParams())
        val export = makeButton("⇧  EXPORTAR VÍDEO") { exportVideo() }
        export.isEnabled = false
        export.alpha = 0.5f
        exportButton = export
        root.addView(export, buttonParams())
        status = text("Importe um vídeo para começar.", 13f, Color.LTGRAY)
        root.addView(status, LinearLayout.LayoutParams(-1, 46))
        root.addView(text("Exportação inicial: resolução de origem • 4K depende do aparelho e do vídeo", 10f, Color.GRAY), LinearLayout.LayoutParams(-1, 38))
        setContentView(root)
    }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(190, 35, 45))
        setOnClickListener { action() }
    }

    private fun buttonParams() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 54).apply {
        bottomMargin = 10
    }

    private fun exportVideo() {
        val uri = selectedVideo ?: return
        val output = File(getExternalFilesDir(null), "dark_edit_${System.currentTimeMillis()}.mp4")
        status.text = "Preparando exportação..."
        exportButton.isEnabled = false
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                runOnUiThread {
                    status.text = "Exportado: ${output.name}"
                    Toast.makeText(this@MainActivity, "Vídeo salvo na pasta do app.", Toast.LENGTH_LONG).show()
                    exportButton.isEnabled = true
                }
            }
            override fun onError(composition: androidx.media3.transformer.Composition, exportResult: ExportResult, exportException: ExportException) {
                runOnUiThread {
                    status.text = "Não foi possível exportar. Tente outro vídeo."
                    exportButton.isEnabled = true
                    Toast.makeText(this@MainActivity, exportException.message ?: "Erro na exportação", Toast.LENGTH_LONG).show()
                }
            }
        }
        try {
            val transformer = Transformer.Builder(this).addListener(listener).build()
            val item = EditedMediaItem.Builder(MediaItem.fromUri(uri)).build()
            transformer.start(item, output.absolutePath)
        } catch (e: Exception) {
            status.text = "Erro ao iniciar exportação."
            exportButton.isEnabled = true
        }
    }
}
