package com.mtgo.decklistmanager.ui.decklist

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*
import java.io.File

class DeckImagePreviewActivity : AppCompatActivity() {
    private lateinit var file: File
    private val saveImage = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = contentResolver.openOutputStream(uri) ?: error("无法打开保存位置")
                    stream.use { output -> file.inputStream().use { it.copyTo(output) } }
                }
                Toast.makeText(this@DeckImagePreviewActivity, "图片已保存", Toast.LENGTH_SHORT).show()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Toast.makeText(this@DeckImagePreviewActivity, "保存失败，请重试", Toast.LENGTH_LONG).show() }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("path")
        if (path == null) { finish(); return }
        file = File(path)
        if (!file.isFile) { Toast.makeText(this, "图片已失效，请重新导出", Toast.LENGTH_LONG).show(); finish(); return }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val actions = LinearLayout(this)
        fun button(label: String, action: () -> Unit) {
            actions.addView(MaterialButton(this).apply { text = label; setOnClickListener { action() } }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        button("返回") { finish() }
        button("保存 PNG") { saveImage.launch("套牌-${System.currentTimeMillis()}.png") }
        button("分享图片") {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(contentResolver, "套牌图片", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "分享套牌图片"))
        }
        root.addView(actions)
        val missing = intent.getIntExtra("missing", 0)
        root.addView(TextView(this).apply {
            text = if (missing > 0) "有 $missing 张卡图未能加载，已保留牌名与数量。可返回后重新导出。" else "双指缩放或双击放大，拖动查看卡牌。"
            setPadding(16, 8, 16, 8)
        })
        val preview = ZoomableDeckImageView(this).apply { contentDescription = "套牌图片预览，可缩放和拖动" }
        root.addView(LinearLayout(this).apply {
            fun control(label: String, action: () -> Unit) {
                addView(MaterialButton(this@DeckImagePreviewActivity).apply {
                    text = label; setOnClickListener { action() }
                }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            control("放大") { preview.zoomBy(1.5f) }
            control("缩小") { preview.zoomBy(1 / 1.5f) }
            control("全图") { preview.resetZoom() }
        })
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        Glide.with(this).load(file).override(1600, 6000).fitCenter().into(preview)
    }
}
