package com.mtgo.decklistmanager.exporter

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.text.TextUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.mtgo.decklistmanager.data.repository.DecklistRepository
import com.mtgo.decklistmanager.domain.model.*
import com.mtgo.decklistmanager.util.FormatMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.sqrt

class DeckImageExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DecklistRepository
) {
    private val imageClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS).readTimeout(6, TimeUnit.SECONDS).build()

    data class Result(val file: File, val missingImages: Int)

    suspend fun export(deck: Decklist, cards: List<Card>, progress: suspend (Int, Int) -> Unit): Result = withContext(Dispatchers.IO) {
        val main = DeckImageLayout.entries(cards, CardLocation.MAIN)
        val side = DeckImageLayout.entries(cards, CardLocation.SIDEBOARD)
        require(main.isNotEmpty() || side.isNotEmpty()) { "套牌尚未加载，请稍后重试" }
        val height = DeckImageLayout.height(main.size, side.size)
        // Bound the final bitmap to 16 million pixels (64 MB), including unusually large decks.
        val scale = minOf(1.0, sqrt(16_000_000.0 / (DeckImageLayout.WIDTH.toDouble() * height))).toFloat()
        val output = Bitmap.createBitmap((1600 * scale).toInt(), (height * scale).toInt(), Bitmap.Config.ARGB_8888)
        val file = File(File(context.cacheDir, "deck-images").apply { mkdirs() }, "deck-${UUID.randomUUID()}.png")
        try {
            val canvas = Canvas(output)
            canvas.scale(scale, scale)
            canvas.drawColor(Color.rgb(23, 28, 36))
            val text = TextPaint(Paint.ANTI_ALIAS_FLAG)
            fun line(value: String, x: Float, y: Float, size: Float, width: Float, color: Int = Color.WHITE) {
                text.textSize = size; text.color = color
                canvas.drawText(TextUtils.ellipsize(value.replace('\n', ' '), text, width, TextUtils.TruncateAt.END).toString(), x, y, text)
            }
            line(deck.deckName?.takeIf { it.isNotBlank() } ?: deck.eventName, 32f, 68f, 42f, 1536f)
            line(listOfNotNull(FormatMapper.codeToName(deck.format), deck.playerName, deck.record, deck.date)
                .filter { it.isNotBlank() }.joinToString("  ·  "), 32f, 112f, 24f, 1536f, Color.LTGRAY)
            line(deck.eventName, 32f, 150f, 22f, 1536f, Color.LTGRAY)
            var y = DeckImageLayout.HEADER
            var completed = 0
            var missing = 0
            val total = main.size + side.size
            for ((title, entries) in listOf("主牌" to main, "备牌" to side)) {
                if (entries.isEmpty()) continue
                line("$title · ${entries.sumOf { it.quantity }} 张", 32f, y + 38f, 28f, 1536f)
                y += DeckImageLayout.SECTION
                for ((index, card) in entries.withIndex()) {
                    ensureActive()
                    var target: FutureTarget<Bitmap>? = null
                    var ownedArt: Bitmap? = null
                    var art = try {
                        val info = withTimeoutOrNull(8_000) { repository.getCardInfo(card.cardName) }
                        val url = info?.imageUriNormal ?: info?.frontImageUri ?: info?.imageUriSmall
                        if (url.isNullOrBlank()) null else {
                            target = Glide.with(context).asBitmap().load(url).submit(180, 252)
                            target!!.get(6, TimeUnit.SECONDS)
                        }
                    } catch (e: CancellationException) { target?.let { Glide.with(context).clear(it) }; throw e }
                    catch (e: Exception) { null }
                    if (art == null) {
                        // Named lookup also works when old cached metadata has no set/collector number.
                        ownedArt = try {
                            val request = okhttp3.Request.Builder()
                                .url("https://api.scryfall.com/cards/named?exact=${android.net.Uri.encode(card.cardName)}&format=image&version=normal")
                                .header("User-Agent", "DecklistManager/5.0.1 (https://github.com/kaorou-bot/decklist-android)")
                                .header("Accept", "image/*").build()
                            imageClient.newCall(request).execute().use { response ->
                                if (!response.isSuccessful) null else response.body?.byteStream()?.use {
                                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 2 })
                                }
                            }
                        } catch (e: Exception) { null }
                        art = ownedArt
                    }
                    val x = (32 + index % 8 * 192).toFloat()
                    val top = (y + index / 8 * DeckImageLayout.CELL_HEIGHT).toFloat()
                    try {
                        ensureActive()
                        val rect = RectF(x, top, x + 180, top + 252)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                        paint.color = Color.rgb(49, 57, 70)
                        canvas.drawRoundRect(rect, 10f, 10f, paint)
                        if (art != null) canvas.drawBitmap(art, null, rect, paint)
                        else {
                            missing++
                            text.textSize = 18f; text.color = Color.LTGRAY
                            val name = card.cardNameZh?.takeIf { it.isNotBlank() } ?: card.cardName
                            @Suppress("DEPRECATION")
                            val layout = android.text.StaticLayout(name, text, 148,
                                android.text.Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
                            val checkpoint = canvas.save()
                            canvas.translate(x + 16, top + 45)
                            canvas.clipRect(0, 0, 148, 140)
                            layout.draw(canvas)
                            canvas.restoreToCount(checkpoint)
                            line("卡图暂缺", x + 16, top + 194, 18f, 148f, Color.LTGRAY)
                        }
                        paint.color = Color.rgb(230, 172, 65)
                        canvas.drawRoundRect(RectF(x + 126, top + 210, x + 178, top + 250), 6f, 6f, paint)
                        line("×${card.quantity}", x + 132, top + 239, 23f, 46f, Color.BLACK)
                        val caption = card.cardNameZh?.takeIf { it.isNotBlank() } ?: card.cardName
                        text.color = Color.WHITE
                        text.textSize = 20f
                        @Suppress("DEPRECATION")
                        fun captionLayout() = android.text.StaticLayout(caption, text, 180,
                            android.text.Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
                        var label = captionLayout()
                        while (label.height > 68 && text.textSize > 12f) {
                            text.textSize -= 1f
                            label = captionLayout()
                        }
                        val labelSave = canvas.save()
                        canvas.translate(x, top + 258)
                        canvas.clipRect(0, 0, 180, 70)
                        label.draw(canvas)
                        canvas.restoreToCount(labelSave)
                    } finally { target?.let { Glide.with(context).clear(it) }; ownedArt?.recycle() }
                    progress(++completed, total)
                }
                y += DeckImageLayout.rows(entries.size) * DeckImageLayout.CELL_HEIGHT
            }
            line("MTG 套牌  ·  卡牌图像 © Wizards of the Coast", 32f, height - 20f, 18f, 1536f, Color.LTGRAY)
            ensureActive()
            file.outputStream().use { check(output.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            Result(file, missing)
        } catch (e: Throwable) {
            file.delete()
            throw e
        } finally { output.recycle() }
    }
}
