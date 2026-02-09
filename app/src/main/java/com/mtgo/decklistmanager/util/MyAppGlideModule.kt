package com.mtgo.decklistmanager.util

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

/**
 * Glide 配置模块 - 优化图片加载性能
 *
 * 优化策略：
 * 1. 使用 RGB_565 格式减少内存占用（比 ARGB_8888 节省 50% 内存）
 * 2. 启用磁盘缓存，避免重复下载
 * 3. 启用图片预加载
 */
@com.bumptech.glide.annotation.GlideModule
class MyAppGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // 设置默认的请求选项
        val defaultOptions = RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565) // 使用 RGB_565 节省内存
            .diskCacheStrategy(DiskCacheStrategy.ALL) // 缓存所有版本的图片
            .encodeQuality(90) // JPEG 编码质量

        builder.setDefaultRequestOptions(defaultOptions)

        AppLogger.d("GlideModule", "Glide配置完成 - RGB_565格式已启用")
    }

    // 禁用清单解析（提升性能）
    override fun isManifestParsingEnabled(): Boolean = false
}
