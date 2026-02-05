package com.mtgo.decklistmanager.ui.carddetail

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ActivityCardDetailBinding
import com.mtgo.decklistmanager.domain.model.CardInfo
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint

/**
 * Card Detail Activity - 卡牌详情页面
 */
@AndroidEntryPoint
class CardDetailActivity : AppCompatActivity() {

    private val viewModel: CardDetailViewModel by viewModels()
    private lateinit var binding: ActivityCardDetailBinding
    private lateinit var legalitiesAdapter: LegalitiesAdapter

    private var currentCardInfo: CardInfo? = null
    private var isShowingFront = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        loadData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        legalitiesAdapter = LegalitiesAdapter()

        binding.rvLegalities.apply {
            layoutManager = LinearLayoutManager(this@CardDetailActivity)
            adapter = legalitiesAdapter
        }
    }

    private fun setupObservers() {
        viewModel.cardInfo.observe(this) { cardInfo ->
            cardInfo?.let {
                displayCardInfo(it)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun loadData() {
        viewModel.loadCardDetail()
    }

    /**
     * 将文本中的换行符转换为实际的换行
     * 处理多种可能的换行符格式
     */
    private fun formatText(text: String?): CharSequence {
        if (text == null) return ""

        // 处理多种换行符格式
        return text
            .replace("\\n", "\n")      // 字面量 \n 转换为换行符
            .replace("\\r\\n", "\n")   // Windows 风格
            .replace("\\r", "\n")      // 旧 Mac 风格
            .replace("\n", "\n")       // 确保实际的换行符保留
    }

    private fun displayCardInfo(cardInfo: CardInfo) {
        currentCardInfo = cardInfo

        // 添加日志
        AppLogger.d("CardDetailActivity", "isDualFaced: ${cardInfo.isDualFaced}")
        AppLogger.d("CardDetailActivity", "backImageUri: ${cardInfo.backImageUri}")
        AppLogger.d("CardDetailActivity", "frontFaceName: ${cardInfo.frontFaceName}")
        AppLogger.d("CardDetailActivity", "backFaceName: ${cardInfo.backFaceName}")

        isShowingFront = true  // Reset to front when loading new card
        updateCardDisplay()
    }

    private fun updateCardDisplay() {
        val cardInfo = currentCardInfo ?: return

        binding.apply {
            // Update flip button visibility and text
            if (cardInfo.isDualFaced) {
                btnFlipCard.visibility = View.VISIBLE
                btnFlipCard.text = "查看其他部分"

                // Set flip button click listener
                btnFlipCard.setOnClickListener {
                    isShowingFront = !isShowingFront
                    updateCardDisplay()
                }
            } else {
                btnFlipCard.visibility = View.GONE
            }

            // Load image - 根据当前显示的面选择图片
            val imageUrl = if (cardInfo.isDualFaced) {
                if (isShowingFront) {
                    cardInfo.frontImageUri ?: cardInfo.imageUriNormal
                } else {
                    cardInfo.backImageUri
                }
            } else {
                cardInfo.imageUriNormal
            }

            imageUrl?.let {
                Glide.with(this@CardDetailActivity)
                    .load(it)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(ivCardImage)
            }

            // Card info - 根据当前显示的面选择信息
            if (cardInfo.isDualFaced) {
                if (isShowingFront) {
                    // 显示正面信息
                    tvCardName.text = cardInfo.frontFaceName ?: cardInfo.name
                    tvManaCost.text = cardInfo.manaCost ?: ""
                    tvTypeLine.text = cardInfo.typeLine ?: ""
                    tvOracleText.text = formatText(cardInfo.oracleText)

                    // Power/Toughness for creatures
                    if (!cardInfo.power.isNullOrEmpty() && !cardInfo.toughness.isNullOrEmpty()) {
                        tvPowerToughness.text = "${cardInfo.power}/${cardInfo.toughness}"
                        tvPowerToughness.visibility = View.VISIBLE
                    } else {
                        tvPowerToughness.visibility = View.GONE
                    }

                    // 隐藏反面信息区域
                    llBackFace.visibility = View.GONE
                } else {
                    // 显示反面信息
                    tvCardName.text = cardInfo.backFaceName ?: ""
                    tvManaCost.text = cardInfo.backFaceManaCost ?: ""
                    tvTypeLine.text = cardInfo.backFaceTypeLine ?: ""
                    tvOracleText.text = formatText(cardInfo.backFaceOracleText)

                    // 反面通常没有 Power/Toughness
                    tvPowerToughness.visibility = View.GONE

                    // 隐藏反面信息区域（因为我们已经在主区域显示反面信息了）
                    llBackFace.visibility = View.GONE
                }
            } else {
                // 普通卡牌显示
                tvCardName.text = cardInfo.name
                tvManaCost.text = cardInfo.manaCost ?: ""
                tvTypeLine.text = cardInfo.typeLine ?: ""
                tvOracleText.text = formatText(cardInfo.oracleText)

                // Power/Toughness for creatures
                if (!cardInfo.power.isNullOrEmpty() && !cardInfo.toughness.isNullOrEmpty()) {
                    tvPowerToughness.text = "${cardInfo.power}/${cardInfo.toughness}"
                    tvPowerToughness.visibility = View.VISIBLE
                } else {
                    tvPowerToughness.visibility = View.GONE
                }

                // 隐藏反面信息区域
                llBackFace.visibility = View.GONE
            }

            // Set info
            val setInfo = if (!cardInfo.setName.isNullOrEmpty() && !cardInfo.setCode.isNullOrEmpty()) {
                "${cardInfo.setName} (${cardInfo.setCode})"
            } else {
                cardInfo.setName ?: cardInfo.setCode ?: ""
            }
            tvSetInfo.text = setInfo

            cardInfo.cardNumber?.let { number ->
                tvSetInfo.text = "${tvSetInfo.text} — #$number"
            }

            tvArtist.text = cardInfo.artist?.let { "Artist: $it" } ?: ""

            // Legalities
            val legalitiesList = buildLegalitiesList(cardInfo)
            legalitiesAdapter.submitList(legalitiesList)
        }
    }

    private fun buildLegalitiesList(cardInfo: CardInfo): List<LegalityItem> {
        val formats = listOf(
            "Standard" to cardInfo.legalStandard,
            "Modern" to cardInfo.legalModern,
            "Pioneer" to cardInfo.legalPioneer,
            "Legacy" to cardInfo.legalLegacy,
            "Vintage" to cardInfo.legalVintage,
            "Commander" to cardInfo.legalCommander,
            "Pauper" to cardInfo.legalPauper
        )

        return formats.mapNotNull { (format, legality) ->
            if (!legality.isNullOrEmpty()) {
                LegalityItem(format, legality)
            } else {
                null
            }
        }
    }

    data class LegalityItem(
        val format: String,
        val legality: String
    )
}
