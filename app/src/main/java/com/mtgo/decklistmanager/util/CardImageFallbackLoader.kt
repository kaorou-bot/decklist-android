package com.mtgo.decklistmanager.util

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 卡图加载失败回退加载器
 *
 * 背景: Forge 中文卡查 API 的部分新印刷版本尚未同步卡图，
 * 直接加载 image_url 可能 404。README 要求 App 在加载失败时
 * 按详情 printings 顺序尝试其他版本的 image_url（背面同理 back_image_url）。
 *
 * 策略（惰性回退）:
 * 1. 正常加载主图，不增加额外请求
 * 2. 主图加载失败时，拉取该卡的 printings 列表（带缓存）
 * 3. 按顺序尝试各版本图片
 * 4. 自有服务器全部失败时，正面图回退到 Scryfall
 *    （api.scryfall.com/cards/{set}/{collector}?format=image 直接返回图片）
 * 5. 仍失败才显示错误占位图
 */
@Singleton
class CardImageFallbackLoader @Inject constructor(
    private val mtgchApi: MtgchApi
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** 主线程 Handler：Glide 禁止在 RequestListener 回调内发起新加载，需 post 到下一轮消息循环 */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Scryfall API 要求描述性 User-Agent（HTTP 库默认 UA 会被 400 generic_user_agent 拒绝），
     * 按官方政策附带应用标识与项目地址
     */
    private val scryfallUserAgent =
        "decklist-manager-android/5.0.1 (https://github.com/kaorou-bot/decklist-android)"

    /** Scryfall 专用 OkHttp 客户端（Glide 默认 UA 无法覆盖，故直接抓取） */
    private val scryfallClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    /** cardId -> 候选图片 URL 列表（正面与背面分开缓存） */
    private val frontCache = ConcurrentHashMap<String, List<String>>()
    private val backCache = ConcurrentHashMap<String, List<String>>()

    /**
     * 加载卡图，失败时自动回退到其他印刷版本，最后回退 Scryfall
     *
     * @param imageView 目标视图
     * @param primaryUrl 主图 URL
     * @param cardId 卡牌标识（新 API 24 位 id，存于 MtgchCardDto.oracleId）
     * @param isBack 是否加载背面图
     * @param setCode 系列代码（Scryfall 回退用，可选）
     * @param collectorNumber 收藏编号（Scryfall 回退用，可选）
     * @param placeholderRes 加载中占位图
     * @param errorRes 全部失败后的错误图
     */
    fun load(
        imageView: ImageView,
        primaryUrl: String?,
        cardId: String?,
        isBack: Boolean = false,
        setCode: String? = null,
        collectorNumber: String? = null,
        placeholderRes: Int,
        errorRes: Int
    ) {
        if (primaryUrl.isNullOrEmpty()) {
            // 主图缺失（服务器无该版本图）：若有标识信息则直接进回退链
            // （printings 候选 → Scryfall），否则隐藏视图
            val hasFallbackData = !cardId.isNullOrEmpty() ||
                (!setCode.isNullOrBlank() && !collectorNumber.isNullOrBlank())
            if (!hasFallbackData) {
                imageView.visibility = ImageView.GONE
                return
            }
            imageView.visibility = ImageView.VISIBLE
            loadWithFallbacks(
                imageView, emptyList(), cardId, isBack, 0,
                setCode, collectorNumber, placeholderRes, errorRes
            )
            return
        }
        imageView.visibility = ImageView.VISIBLE

        loadWithFallbacks(
            imageView, listOf(primaryUrl), cardId, isBack, 0,
            setCode, collectorNumber, placeholderRes, errorRes
        )
    }

    /**
     * 从 index 开始依次尝试 urls；主图失败后拉取 printings 候选列表继续；
     * 候选耗尽后回退 Scryfall
     */
    private fun loadWithFallbacks(
        imageView: ImageView,
        urls: List<String>,
        cardId: String?,
        isBack: Boolean,
        index: Int,
        setCode: String?,
        collectorNumber: String?,
        placeholderRes: Int,
        errorRes: Int
    ) {
        if (index >= urls.size) {
            if (urls.size <= 1 && !cardId.isNullOrEmpty()) {
                // 主图失败且无缓存候选：拉取 printings 后重试
                fetchCandidates(cardId, isBack) { candidates ->
                    val remaining = candidates.filter { it !in urls }
                    if (remaining.isEmpty()) {
                        tryScryfall(
                            imageView, isBack, setCode, collectorNumber, placeholderRes, errorRes
                        )
                    } else {
                        loadWithFallbacks(
                            imageView, remaining, cardId, isBack, 0,
                            setCode, collectorNumber, placeholderRes, errorRes
                        )
                    }
                }
            } else {
                // 候选耗尽：回退 Scryfall
                tryScryfall(
                    imageView, isBack, setCode, collectorNumber, placeholderRes, errorRes
                )
            }
            return
        }

        Glide.with(imageView)
            .load(urls[index])
            .placeholder(placeholderRes)
            .error(errorRes)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    AppLogger.w(
                        "CardImageFallback",
                        "Image failed [${index + 1}/${urls.size}]: ${urls[index]}"
                    )
                    // Glide 禁止在 RequestListener 回调内直接发起新加载，
                    // 必须 post 到下一个主线程消息循环
                    mainHandler.post {
                        loadWithFallbacks(
                            imageView, urls, cardId, isBack, index + 1,
                            setCode, collectorNumber, placeholderRes, errorRes
                        )
                    }
                    return true // 接管错误处理，不让 Glide 显示 error 图
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean = false
            })
            .into(imageView)
    }

    /**
     * Scryfall 最终回退：/cards/{set}/{collector}?format=image 直接返回图片字节
     */
    private fun tryScryfall(
        imageView: ImageView,
        isBack: Boolean,
        setCode: String?,
        collectorNumber: String?,
        placeholderRes: Int,
        errorRes: Int
    ) {
        if (setCode.isNullOrBlank() || collectorNumber.isNullOrBlank()) {
            Glide.with(imageView).load(errorRes).into(imageView)
            return
        }

        val face = if (isBack) "&face=back" else ""
        val scryfallUrl =
            "https://api.scryfall.com/cards/${setCode.lowercase()}/$collectorNumber?format=image$face"

        AppLogger.d("CardImageFallback", "Trying Scryfall fallback: $scryfallUrl")
        imageView.setImageResource(placeholderRes)

        // Glide 默认 UA 无法覆盖（会被 Scryfall 400 拒绝），
        // 故用 OkHttp 带自定义 UA 抓取图片字节，再交 Glide 解码显示
        scope.launch(Dispatchers.IO) {
            val bytes = try {
                val request = okhttp3.Request.Builder()
                    .url(scryfallUrl)
                    .header("User-Agent", scryfallUserAgent)
                    .header("Accept", "*/*") // Scryfall 强制要求 Accept 头（OkHttp 默认不发）
                    .build()
                scryfallClient.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        resp.body?.bytes()
                    } else {
                        val snippet = resp.body?.string()?.take(200) ?: ""
                        AppLogger.w(
                            "CardImageFallback",
                            "Scryfall HTTP ${resp.code} for $scryfallUrl body: $snippet"
                        )
                        null
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("CardImageFallback", "Scryfall fetch exception: $scryfallUrl", e)
                null
            }

            withContext(Dispatchers.Main) {
                if (bytes != null && bytes.isNotEmpty()) {
                    AppLogger.d(
                        "CardImageFallback",
                        "Scryfall fallback succeeded: $scryfallUrl (${bytes.size} bytes)"
                    )
                    Glide.with(imageView)
                        .load(bytes)
                        .placeholder(placeholderRes)
                        .error(errorRes)
                        .into(imageView)
                } else {
                    AppLogger.w("CardImageFallback", "Scryfall fallback also failed: $scryfallUrl")
                    Glide.with(imageView).load(errorRes).into(imageView)
                }
            }
        }
    }

    /**
     * 拉取 printings 候选图片列表（带内存缓存）
     */
    private fun fetchCandidates(
        cardId: String,
        isBack: Boolean,
        onResult: (List<String>) -> Unit
    ) {
        val cache = if (isBack) backCache else frontCache
        cache[cardId]?.let { onResult(it); return }

        scope.launch {
            try {
                val response = mtgchApi.getCardPrintings(cardId, limit = 100, offset = 0)
                val printings = if (response.isSuccessful) response.body()?.cards ?: emptyList() else emptyList()

                val candidates = printings.mapNotNull { card ->
                    if (isBack) {
                        card.cardFaces?.getOrNull(1)?.imageUris?.normal
                    } else {
                        card.zhsImage ?: card.imageUris?.normal
                    }
                }.distinct()

                cache[cardId] = candidates
                AppLogger.d("CardImageFallback", "Fetched ${candidates.size} candidates for $cardId (back=$isBack)")
                onResult(candidates)
            } catch (e: Exception) {
                AppLogger.e("CardImageFallback", "Fetch candidates failed: $cardId", e)
                onResult(emptyList())
            }
        }
    }
}
