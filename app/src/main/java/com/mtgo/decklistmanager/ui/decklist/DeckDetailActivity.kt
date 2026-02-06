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
import com.mtgo.decklistmanager.ui.analysis.DeckAnalysisActivity
import com.mtgo.decklistmanager.ui.dialog.ExportFormatDialog
import com.mtgo.decklistmanager.util.ManaSymbolRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeckDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        setupCardLists()
        setupObservers()
        loadData()
    }

    override fun onStart() {
        super.onStart()
        // 重新检查收藏状态（每次进入前台都检查）
        refreshFavoriteStatus()
    }

    override fun onResume() {
        super.onResume()
        // 重新检查收藏状态（因为用户可能在MainActivity中改变了收藏状态）
        refreshFavoriteStatus()
    }

    private fun refreshFavoriteStatus() {
        currentDecklist?.let { decklist ->
            lifecycleScope.launch {
                val newFavoriteState = viewModel.isFavorite(decklist.id)
                isFavorite = newFavoriteState
                updateFavoriteIcon()
            }
        }
    }

    private fun setupButtons() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 收藏按钮
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        // 导出和分享按钮
        binding.btnExportShare.setOnClickListener {
            showExportFormatDialog()
        }

        // 分析按钮
        binding.btnAnalysis.setOnClickListener {
            openDeckAnalysis()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return false // 不再使用菜单
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
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

    private fun openDeckAnalysis() {
        currentDecklist?.let { decklist ->
            val intent = Intent(this, DeckAnalysisActivity::class.java).apply {
                putExtra(DeckAnalysisActivity.EXTRA_DECKLIST_ID, decklist.id)
                putExtra(DeckAnalysisActivity.EXTRA_DECKLIST_NAME, decklist.deckName ?: "Unknown Deck")
            }
            startActivity(intent)
        }
    }

    private fun updateFavoriteIcon() {
        if (isFavorite) {
            binding.btnFavorite.setIconResource(R.drawable.ic_favorite_filled)
        } else {
            binding.btnFavorite.setIconResource(R.drawable.ic_favorite_border)
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

        // Observe export result
        viewModel.exportResult.observe(this) { result ->
            result?.let {
                handleExportResult(it)
                viewModel.clearExportResult()
            }
        }

        // Observe export error
        viewModel.exportError.observe(this) { error ->
            error?.let {
                android.widget.Toast.makeText(
                    this,
                    it,
                    android.widget.Toast.LENGTH_LONG
                ).show()
                viewModel.clearExportError()
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
            val favoriteState = viewModel.isFavorite(decklistId)
            isFavorite = favoriteState
            updateFavoriteIcon()
        }
    }

    private fun updateDecklistInfo(decklist: Decklist) {
        // 设置顶部标题
        binding.tvDeckNameTitle.text = decklist.deckName ?: decklist.eventName ?: "Unknown Deck"

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

    /**
     * 显示导出格式选择对话框
     */
    private fun showExportFormatDialog() {
        val dialog = ExportFormatDialog.newInstance()
        dialog.setExportFormatListener(object : ExportFormatDialog.ExportFormatListener {
            override fun onExportFormatSelected(format: ExportFormatDialog.ExportFormat) {
                currentDecklist?.let { decklist ->
                    when (format) {
                        ExportFormatDialog.ExportFormat.MTGO -> {
                            exportDecklist(decklist, "mtgo")
                        }
                        ExportFormatDialog.ExportFormat.ARENA -> {
                            exportDecklist(decklist, "arena")
                        }
                        ExportFormatDialog.ExportFormat.TEXT -> {
                            exportDecklist(decklist, "text")
                        }
                    }
                }
            }

            override fun onShareMoxfield() {
                currentDecklist?.let { decklist ->
                    shareToMoxfield(decklist)
                }
            }

            override fun onCopyToClipboard() {
                currentDecklist?.let { decklist ->
                    copyToClipboard(decklist)
                }
            }
        })
        dialog.show(supportFragmentManager, ExportFormatDialog.TAG)
    }

    /**
     * 导出套牌
     */
    private fun exportDecklist(decklist: Decklist, format: String) {
        viewModel.exportDecklist(format, includeSideboard = true)
    }

    /**
     * 处理导出结果
     */
    private fun handleExportResult(result: com.mtgo.decklistmanager.exporter.ExportResult) {
        // 使用 FileSaver 保存文件
        lifecycleScope.launch {
            try {
                val fileSaver = com.mtgo.decklistmanager.exporter.FileSaver(this@DeckDetailActivity)
                val savedUri = fileSaver.saveFile(result.fileName, result.content)

                if (savedUri != null) {
                    android.widget.Toast.makeText(
                        this@DeckDetailActivity,
                        "已导出: ${result.fileName}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    // 询问是否分享
                    androidx.appcompat.app.AlertDialog.Builder(this@DeckDetailActivity)
                        .setTitle("导出成功")
                        .setMessage("套牌已保存为 ${result.formatName} 格式\n\n是否分享文件？")
                        .setPositiveButton("分享") { _, _ ->
                            shareFile(savedUri, result.fileName)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                } else {
                    android.widget.Toast.makeText(
                        this@DeckDetailActivity,
                        "保存失败",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    this@DeckDetailActivity,
                    "导出出错: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * 分享文件
     */
    private fun shareFile(uri: android.net.Uri, fileName: String) {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(android.content.Intent.createChooser(shareIntent, "分享套牌"))
    }

    /**
     * 分享到 Moxfield
     */
    private fun shareToMoxfield(decklist: Decklist) {
        lifecycleScope.launch {
            try {
                val allCards = viewModel.getAllCards()

                // 生成 Moxfield 链接
                val moxfieldGenerator = com.mtgo.decklistmanager.exporter.share.MoxfieldShareGenerator()
                val shareLink = moxfieldGenerator.generateShareLink(decklist, allCards)

                // 打开浏览器
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(shareLink)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)

                android.widget.Toast.makeText(
                    this@DeckDetailActivity,
                    "正在打开 Moxfield...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    this@DeckDetailActivity,
                    "分享失败: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 复制到剪贴板
     */
    private fun copyToClipboard(decklist: Decklist) {
        // 使用文本格式导出
        lifecycleScope.launch {
            try {
                val allCards = viewModel.getAllCards()
                val textExporter = com.mtgo.decklistmanager.exporter.format.TextFormatExporter()
                val content = textExporter.export(decklist, allCards, true)

                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Decklist", content)
                clipboard.setPrimaryClip(clip)

                android.widget.Toast.makeText(
                    this@DeckDetailActivity,
                    "已复制到剪贴板",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    this@DeckDetailActivity,
                    "复制失败: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
