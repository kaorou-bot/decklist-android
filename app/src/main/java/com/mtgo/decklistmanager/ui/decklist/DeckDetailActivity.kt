package com.mtgo.decklistmanager.ui.decklist

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ActivityDeckDetailBinding
import com.mtgo.decklistmanager.domain.model.Card
import com.mtgo.decklistmanager.domain.model.CardLocation
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.util.ManaSymbolRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Deck Detail Activity - 牌组详情页面
 */
@AndroidEntryPoint
class DeckDetailActivity : AppCompatActivity() {

    private val viewModel: DeckDetailViewModel by viewModels()
    private lateinit var binding: ActivityDeckDetailBinding

    private lateinit var llMainDeck: LinearLayout
    private lateinit var llSideboard: LinearLayout
    private lateinit var tvMainboardCount: MaterialTextView
    private lateinit var tvSideboardCount: MaterialTextView

    private var currentDecklist: Decklist? = null
    private var allCards: List<Card> = emptyList()
    private var isFavorite = false
    private var favoriteMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeckDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCardLists()
        setupObservers()
        loadData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_deck_detail, menu)
        favoriteMenuItem = menu.findItem(R.id.action_favorite)
        updateFavoriteIcon()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_favorite -> {
                toggleFavorite()
                true
            }
            R.id.action_export_text -> {
                showExportDialog("text")
                true
            }
            R.id.action_export_json -> {
                showExportDialog("json")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleFavorite() {
        currentDecklist?.let { decklist ->
            lifecycleScope.launch {
                val newState = viewModel.toggleFavorite(decklist.id)
                isFavorite = newState
                updateFavoriteIcon()

                android.widget.Toast.makeText(
                    this@DeckDetailActivity,
                    if (newState) "Added to favorites" else "Removed from favorites",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateFavoriteIcon() {
        favoriteMenuItem?.apply {
            if (isFavorite) {
                setIcon(android.R.drawable.star_big_on)
                setTitle("Remove from Favorites")
            } else {
                setIcon(android.R.drawable.star_big_off)
                setTitle("Add to Favorites")
            }
        }
    }

    private fun setupCardLists() {
        llMainDeck = binding.llMainDeck
        llSideboard = binding.llSideboard
        tvMainboardCount = binding.tvMainboardCount
        tvSideboardCount = binding.tvSideboardCount

        // Setup toggle buttons
        binding.btnToggleMainboard.setOnClickListener {
            toggleSection(llMainDeck, binding.btnToggleMainboard)
        }

        binding.btnToggleSideboard.setOnClickListener {
            toggleSection(llSideboard, binding.btnToggleSideboard)
        }
    }

    /**
     * 切换区域的展开/收起状态
     */
    private fun toggleSection(linearLayout: LinearLayout, button: com.google.android.material.button.MaterialButton) {
        if (linearLayout.visibility == View.VISIBLE) {
            linearLayout.visibility = View.GONE
            // 旋转图标表示收起状态（旋转180度）
            button.rotation = 180f
        } else {
            linearLayout.visibility = View.VISIBLE
            // 恢复图标原始方向
            button.rotation = 0f
        }
    }

    /**
     * 创建卡牌视图
     */
    private fun createCardView(card: Card): View {
        // 创建卡牌项布局
        val cardView = layoutInflater.inflate(R.layout.item_card, llMainDeck, false)

        // 设置卡牌数据
        cardView.findViewById<MaterialTextView>(R.id.tvQuantity).text = card.quantity.toString()
        val btnCardName = cardView.findViewById<MaterialButton>(R.id.btnCardName)
        // 优先显示中文名，如果没有则显示英文名
        btnCardName.text = card.cardNameZh ?: card.cardName
        btnCardName.setOnClickListener {
            showCardInfo(card.cardName)
        }
        val tvManaCost = cardView.findViewById<MaterialTextView>(R.id.tvManaCost)
        tvManaCost.text = ManaSymbolRenderer.renderManaCost(card.manaCost, this)

        return cardView
    }

    /**
     * 填充卡牌列表
     */
    private fun populateCardList(linearLayout: LinearLayout, cards: List<Card>) {
        // 清空现有视图
        linearLayout.removeAllViews()

        // 添加卡牌视图
        cards.forEach { card ->
            val cardView = createCardView(card)
            linearLayout.addView(cardView)
        }
    }

    /**
     * 更新卡牌数量显示
     */
    private fun updateCardCount(textView: MaterialTextView, cards: List<Card>, sectionName: String) {
        // 计算总卡牌数量（每张卡的数量 × 数量）
        val totalCards = cards.sumOf { it.quantity }
        textView.text = "$sectionName ($totalCards)"
    }

    private fun setupObservers() {
        // Observe decklist
        viewModel.decklist.observe(this) { decklist ->
            decklist?.let {
                currentDecklist = it
                updateDecklistInfo(it)
            }
        }

        // Observe main deck
        viewModel.mainDeck.observe(this) { cards ->
            populateCardList(llMainDeck, cards)
            updateCardCount(tvMainboardCount, cards, "Mainboard")
        }

        // Observe sideboard
        viewModel.sideboard.observe(this) { cards ->
            populateCardList(llSideboard, cards)
            updateCardCount(tvSideboardCount, cards, "Sideboard")
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe card info loading state
        viewModel.isCardInfoLoading.observe(this) { isLoading ->
            if (isLoading) {
                // Show loading toast
                android.widget.Toast.makeText(
                    this,
                    "Loading card info...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Observe card info error
        viewModel.cardInfoError.observe(this) { errorMessage ->
            errorMessage?.let {
                android.widget.Toast.makeText(
                    this,
                    it,
                    android.widget.Toast.LENGTH_LONG
                ).show()
                viewModel.clearCardInfoError()
            }
        }

        // Observe card info for popup
        viewModel.cardInfo.observe(this) { cardInfo ->
            cardInfo?.let {
                showCardInfoDialog(it)
                viewModel.clearCardInfo()
            }
        }
    }

    private fun loadData() {
        // Get decklist ID from intent
        val decklistId = intent.getLongExtra("decklistId", -1)
        if (decklistId == -1L) {
            finish()
            return
        }

        viewModel.loadDecklistDetail()

        // Check if this decklist is favorited
        lifecycleScope.launch {
            isFavorite = viewModel.isFavorite(decklistId)
            updateFavoriteIcon()
        }
    }

    private fun updateDecklistInfo(decklist: Decklist) {
        binding.apply {
            tvEventName.text = decklist.eventName
            tvFormat.text = "Format: ${decklist.format}"
            tvDate.text = "Date: ${decklist.date}"
            tvPlayer.text = decklist.playerName?.let { "Player: $it" } ?: "Player: N/A"
            tvRecord.text = decklist.record ?: "N/A"
        }
    }

    private fun showCardInfo(cardName: String) {
        viewModel.loadCardInfo(cardName)
    }

    private fun showCardInfoDialog(cardInfo: com.mtgo.decklistmanager.domain.model.CardInfo) {
        // Show card info popup
        CardInfoFragment.newInstance(cardInfo).show(
            supportFragmentManager,
            "card_info"
        )
    }

    private fun showExportDialog(format: String) {
        val decklist = currentDecklist
        if (decklist == null) {
            android.widget.Toast.makeText(this, "No decklist data", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // Collect all cards
        allCards = (viewModel.mainDeck.value ?: emptyList()) + (viewModel.sideboard.value ?: emptyList())

        val formatName = if (format == "text") "Text" else "JSON"

        AlertDialog.Builder(this)
            .setTitle("Export as $formatName")
            .setMessage("Export '${decklist.eventName}' to $formatName format?")
            .setPositiveButton("Export") { _, _ ->
                performExport(format, decklist, allCards)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performExport(format: String, decklist: Decklist, cards: List<Card>) {
        val content = if (format == "text") {
            buildTextContent(decklist, cards)
        } else {
            buildJsonContent(decklist, cards)
        }

        // Copy to clipboard
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Decklist", content)
        clipboardManager.setPrimaryClip(clipData)

        android.widget.Toast.makeText(
            this,
            "已复制到剪贴板",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun buildTextContent(decklist: Decklist, cards: List<Card>): String {
        return buildString {
            appendLine("===========================================")
            appendLine(decklist.eventName)
            appendLine("===========================================")
            appendLine()
            appendLine("Format: ${decklist.format}")
            appendLine("Date: ${decklist.date}")
            appendLine("Player: ${decklist.playerName ?: "N/A"}")
            appendLine("Record: ${decklist.record ?: "N/A"}")
            appendLine()

            val mainDeck = cards.filter { it.location == CardLocation.MAIN }
            val sideboard = cards.filter { it.location == CardLocation.SIDEBOARD }

            if (mainDeck.isNotEmpty()) {
                appendLine("Main Deck (${mainDeck.size}):")
                appendLine("-------------------------------------------")
                mainDeck.sortedBy { it.cardOrder }.forEach { card ->
                    appendLine("${card.quantity}x ${card.cardName}")
                    if (!card.manaCost.isNullOrEmpty()) {
                        appendLine("   ${card.manaCost}")
                    }
                }
                appendLine()
            }

            if (sideboard.isNotEmpty()) {
                appendLine("Sideboard (${sideboard.size}):")
                appendLine("-------------------------------------------")
                sideboard.sortedBy { it.cardOrder }.forEach { card ->
                    appendLine("${card.quantity}x ${card.cardName}")
                    if (!card.manaCost.isNullOrEmpty()) {
                        appendLine("   ${card.manaCost}")
                    }
                }
            }

            appendLine()
            appendLine("===========================================")
            appendLine("Generated by MTGO Decklist Manager")
            appendLine("===========================================")
        }
    }

    private fun buildJsonContent(decklist: Decklist, cards: List<Card>): String {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        val exportData = mapOf(
            "decklist" to mapOf(
                "eventName" to decklist.eventName,
                "eventType" to decklist.eventType,
                "format" to decklist.format,
                "date" to decklist.date,
                "url" to decklist.url,
                "playerName" to decklist.playerName,
                "playerId" to decklist.playerId,
                "record" to decklist.record,
                "createdAt" to decklist.createdAt
            ),
            "mainDeck" to cards.filter { it.location == CardLocation.MAIN }
                .sortedBy { it.cardOrder }
                .map { cardToMap(it) },
            "sideboard" to cards.filter { it.location == CardLocation.SIDEBOARD }
                .sortedBy { it.cardOrder }
                .map { cardToMap(it) },
            "exportedAt" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
        )

        return gson.toJson(exportData)
    }

    private fun cardToMap(card: Card): Map<String, Any?> {
        return mapOf(
            "cardName" to card.cardName,
            "quantity" to card.quantity,
            "manaCost" to card.manaCost,
            "rarity" to card.rarity,
            "color" to card.color,
            "cardType" to card.cardType,
            "cardSet" to card.cardSet
        )
    }
}
