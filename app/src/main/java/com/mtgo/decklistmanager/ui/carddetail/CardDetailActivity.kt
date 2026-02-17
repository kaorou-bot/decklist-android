package com.mtgo.decklistmanager.ui.carddetail

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ActivityCardDetailBinding
import com.mtgo.decklistmanager.domain.model.CardInfo
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint

/**
 * Card Detail Activity - 卡牌详情页面
 * 支持印刷版本切换（下拉菜单）
 */
@AndroidEntryPoint
class CardDetailActivity : AppCompatActivity() {

    private val viewModel: CardDetailViewModel by viewModels()
    private lateinit var binding: ActivityCardDetailBinding
    private lateinit var legalitiesAdapter: LegalitiesAdapter

    private var currentCardInfo: CardInfo? = null
    private var isShowingFront = true
    private var printings: List<MtgchCardDto> = emptyList()
    private var currentPrintingIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupVersionSelector()
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

    private fun setupVersionSelector() {
        // 设置版本选择按钮点击监听
        binding.btnSelectVersion.setOnClickListener {
            AppLogger.d("CardDetailActivity", "Version selector clicked, printings size: ${printings.size}")
            if (printings.isNotEmpty()) {
                showVersionSelectorDialog()
            }
        }

        // 初始时隐藏按钮
        binding.btnSelectVersion.visibility = View.GONE

        // 隐藏原来的版本切换按钮（如果存在）
        binding.btnChangeVersion.visibility = View.GONE
    }

    private fun showVersionSelectorDialog() {
        val items = printings.mapIndexed { index, card ->
            val setName = card.setName ?: card.setCode ?: "Unknown"
            val collectorNumber = card.collectorNumber ?: "?"
            val rarity = getRaritySymbol(card.rarity)
            "$setName — #$collectorNumber $rarity"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("选择印刷版本")
            .setItems(items) { _, which ->
                if (which != currentPrintingIndex && which in printings.indices) {
                    switchToPrinting(which)
                }
            }
            .show()
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

        // 观察印刷版本数据
        viewModel.printings.observe(this) { printingList ->
            printingList?.let {
                printings = it
                updateVersionSelectorItems()
            }
        }

        viewModel.currentPrintingIndex.observe(this) { index ->
            val oldIndex = currentPrintingIndex
            currentPrintingIndex = index

            // 如果索引变化，更新当前版本显示
            if (oldIndex != index && index in printings.indices) {
                val card = printings[index]
                val setName = card.setName ?: card.setCode ?: "Unknown"
                val collectorNumber = card.collectorNumber ?: "?"
                val rarity = getRaritySymbol(card.rarity)
                binding.tvCurrentVersion.text = "$setName — #$collectorNumber $rarity"
            }
        }
    }

    private fun loadData() {
        viewModel.loadCardDetail()
    }

    /**
     * 更新版本下拉菜单的选项
     */
    private fun updateVersionSelectorItems() {
        // 如果有多个版本，显示选择按钮
        binding.btnSelectVersion.visibility = if (printings.size > 1) View.VISIBLE else View.GONE

        // 更新当前版本显示
        if (currentPrintingIndex in printings.indices) {
            val card = printings[currentPrintingIndex]
            val setName = card.setName ?: card.setCode ?: "Unknown"
            val collectorNumber = card.collectorNumber ?: "?"
            val rarity = getRaritySymbol(card.rarity)
            binding.tvCurrentVersion.text = "$setName — #$collectorNumber $rarity"
        }
    }

    /**
     * 切换到指定印刷版本
     */
    private fun switchToPrinting(index: Int) {
        currentPrintingIndex = index

        // 更新卡牌信息
        val newCard = printings[index]
        val newCardInfo = com.mtgo.decklistmanager.util.CardDetailHelper.buildCardInfo(
            mtgchCard = newCard,
            cardInfoId = newCard.idString ?: newCard.oracleId ?: ""
        )

        currentCardInfo = newCardInfo
        isShowingFront = true
        updateCardDisplay()
    }

    /**
     * 获取稀有度符号
     */
    private fun getRaritySymbol(rarity: String?): String {
        return when (rarity?.lowercase()) {
            "common" -> "C"
            "uncommon" -> "U"
            "rare" -> "R"
            "mythic" -> "M"
            else -> ""
        }
    }

    /**
     * 将文本中的换行符转换为实际的换行
     */
    private fun formatText(text: String?): CharSequence {
        if (text == null) return ""

        return text
            .replace("\\n", "\n")
            .replace("\\r\\n", "\n")
            .replace("\\r", "\n")
            .replace("\n", "\n")
    }

    private fun displayCardInfo(cardInfo: CardInfo) {
        currentCardInfo = cardInfo

        AppLogger.d("CardDetailActivity", "isDualFaced: ${cardInfo.isDualFaced}")
        AppLogger.d("CardDetailActivity", "backImageUri: ${cardInfo.backImageUri}")

        isShowingFront = true
        updateCardDisplay()
    }

    private fun updateCardDisplay() {
        val cardInfo = currentCardInfo ?: return

        binding.apply {
            // Update flip button visibility and text
            if (cardInfo.isDualFaced) {
                btnFlipCard.visibility = View.VISIBLE
                btnFlipCard.text = "查看其他部分"

                btnFlipCard.setOnClickListener {
                    isShowingFront = !isShowingFront
                    updateCardDisplay()
                }
            } else {
                btnFlipCard.visibility = View.GONE
            }

            // Load image
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

            // Card info
            if (cardInfo.isDualFaced) {
                if (isShowingFront) {
                    tvCardName.text = cardInfo.frontFaceName ?: cardInfo.name
                    tvManaCost.text = cardInfo.manaCost ?: ""
                    tvTypeLine.text = cardInfo.typeLine ?: ""
                    tvOracleText.text = formatText(cardInfo.oracleText)

                    if (!cardInfo.power.isNullOrEmpty() && !cardInfo.toughness.isNullOrEmpty()) {
                        tvPowerToughness.text = "${cardInfo.power}/${cardInfo.toughness}"
                        tvPowerToughness.visibility = View.VISIBLE
                    } else {
                        tvPowerToughness.visibility = View.GONE
                    }

                    llBackFace.visibility = View.GONE
                } else {
                    tvCardName.text = cardInfo.backFaceName ?: ""
                    tvManaCost.text = cardInfo.backFaceManaCost ?: ""
                    tvTypeLine.text = cardInfo.backFaceTypeLine ?: ""
                    tvOracleText.text = formatText(cardInfo.backFaceOracleText)
                    tvPowerToughness.visibility = View.GONE
                    llBackFace.visibility = View.GONE
                }
            } else {
                tvCardName.text = cardInfo.name
                tvManaCost.text = cardInfo.manaCost ?: ""
                tvTypeLine.text = cardInfo.typeLine ?: ""
                tvOracleText.text = formatText(cardInfo.oracleText)

                if (!cardInfo.power.isNullOrEmpty() && !cardInfo.toughness.isNullOrEmpty()) {
                    tvPowerToughness.text = "${cardInfo.power}/${cardInfo.toughness}"
                    tvPowerToughness.visibility = View.VISIBLE
                } else {
                    tvPowerToughness.visibility = View.GONE
                }

                llBackFace.visibility = View.GONE
            }

            // Artist info
            tvArtist.text = cardInfo.artist?.let { "Artist: $it" } ?: ""

            // 如果印刷版本为空或只有一个，显示默认系列信息
            if (printings.size <= 1) {
                val setInfo = if (!cardInfo.setName.isNullOrEmpty() && !cardInfo.setCode.isNullOrEmpty()) {
                    "${cardInfo.setName} (${cardInfo.setCode})"
                } else {
                    cardInfo.setName ?: cardInfo.setCode ?: ""
                }
                cardInfo.cardNumber?.let { number ->
                    binding.tvCurrentVersion.text = "$setInfo — #$number"
                } ?: run {
                    binding.tvCurrentVersion.text = setInfo
                }
            }

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
