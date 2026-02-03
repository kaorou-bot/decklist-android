package com.mtgo.decklistmanager.ui.search

import android.os.Bundle
import android.view.MenuItem
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ActivitySearchBinding
import com.mtgo.decklistmanager.databinding.BottomSheetAdvancedSearchBinding
import com.mtgo.decklistmanager.ui.search.model.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 搜索页面 - 显示搜索结果和搜索历史
 * 完全复制 MTGCH 的高级搜索功能
 */
@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var binding: ActivitySearchBinding

    private lateinit var resultAdapter: SearchResultAdapter
    private lateinit var historyAdapter: SearchHistoryAdapter

    // 当前应用的筛选条件
    private var currentFilters: SearchFilters = SearchFilters.empty()

    // 当前显示的卡牌详情对话框（防止重复打开）
    private var currentDetailDialog: androidx.appcompat.app.AlertDialog? = null

    // 下拉菜单选项
    private val operatorOptions = listOf("任意", "=", ">", "<", ">=", "<=")
    private val formatOptions = Format.values().map { it.displayName }
    private val legalityOptions = LegalityMode.values().map {
        when (it) {
            LegalityMode.LEGAL -> "合法"
            LegalityMode.BANNED -> "禁用"
            LegalityMode.RESTRICTED -> "限用"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.search)

        setupRecyclerViews()
        observeViewModel()
        setupListeners()
    }

    private fun setupRecyclerViews() {
        resultAdapter = SearchResultAdapter { result ->
            showCardDetail(result)
        }
        binding.recyclerViewResults.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = resultAdapter
        }

        historyAdapter = SearchHistoryAdapter(
            onItemClick = { query ->
                viewModel.searchFromHistory(query)
            },
            onDeleteClick = { id ->
                viewModel.deleteSearchHistory(id)
            }
        )
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = historyAdapter
        }
    }

    /**
     * 显示卡牌详情（包含卡图）
     * 限制只能同时显示一个对话框
     * 如果卡牌原本是英文名（没有中文名），则不显示中文字段
     */
    private fun showCardDetail(result: SearchResultItem) {
        // 如果已有对话框打开，先关闭
        currentDetailDialog?.dismiss()

        val dialogBinding = com.mtgo.decklistmanager.databinding.DialogCardDetailBinding.inflate(
            LayoutInflater.from(this)
        )

        // 加载卡牌图片
        if (!result.imageUrl.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(result.imageUrl)
                .placeholder(com.google.android.material.R.drawable.mtrl_ic_cancel)
                .error(com.google.android.material.R.drawable.mtrl_ic_error)
                .into(dialogBinding.imageViewCard)
        } else {
            dialogBinding.imageViewCard.visibility = android.view.View.GONE
        }

        // 判断是否是中文卡牌（有中文名显示）
        val hasChineseName = !result.displayName.isNullOrEmpty() && result.displayName != result.name

        // 构建详细信息文本
        val details = buildString {
            // 卡牌名称：如果有中文名则显示中文名，否则只显示英文名
            if (hasChineseName) {
                appendLine("卡牌名称：${result.displayName}")
                appendLine("英文名称：${result.name}")
            } else {
                appendLine("卡牌名称：${result.name}")
            }
            appendLine()
            appendLine("类型：${result.typeLine ?: result.type ?: ""}")
            appendLine("法术力：${result.manaCost ?: "N/A"}")

            // 处理规则文本中的换行符
            result.oracleText?.let {
                val text = it.replace("\\n", "\n")
                appendLine("规则文本：\n$text")
            }

            result.power?.let { power ->
                result.toughness?.let { toughness ->
                    appendLine("攻防：$power/$toughness")
                }
            }
            result.loyalty?.let { appendLine("忠诚：$it") }
            appendLine()
            appendLine("系列：${result.setName ?: "N/A"}")
            result.collectorNumber?.let { appendLine("编号：$it") }
            result.artist?.let { appendLine("画家：$it") }
            appendLine()
            appendLine("稀有度：${result.rarity ?: "N/A"}")
            result.colorIdentity?.let { appendLine("颜色标识：${it.joinToString()}") }
        }

        dialogBinding.textViewCardDetails.text = details

        // 对话框标题：优先显示中文名
        val title = if (hasChineseName) result.displayName else result.name

        // 创建并保存对话框引用
        currentDetailDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton("关闭", null)
            .setOnDismissListener {
                currentDetailDialog = null
            }
            .create()

        currentDetailDialog?.show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.searchResults.collect { results ->
                        resultAdapter.submitList(results)
                        binding.textViewEmpty.visibility = if (results.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                    }
                }
                launch {
                    viewModel.searchHistory.collect { history ->
                        historyAdapter.submitList(history)
                    }
                }
                launch {
                    viewModel.isSearching.collect { searching ->
                        binding.progressBar.visibility = if (searching) android.view.View.VISIBLE else android.view.View.GONE
                    }
                }
                launch {
                    viewModel.showHistory.collect { show ->
                        binding.layoutHistory.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            android.widget.Toast.makeText(this@SearchActivity, it, android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        // 回车键搜索
        binding.editTextQuery.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }

        // 搜索按钮
        binding.buttonSearch.setOnClickListener {
            performSearch()
        }

        // 清空历史按钮
        binding.buttonClearHistory.setOnClickListener {
            viewModel.clearAllHistory()
        }

        // 筛选按钮
        binding.buttonFilters.setOnClickListener {
            showFiltersDialog()
        }
    }

    /**
     * 执行搜索（允许空文本）
     */
    private fun performSearch() {
        val query = binding.editTextQuery.text?.toString() ?: ""
        viewModel.search(query, filters = currentFilters)
    }

    /**
     * 显示底部表单高级筛选（完全复制 MTGCH）
     * 防止下滑关闭，用户必须点击关闭按钮
     */
    private fun showFiltersDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val dialogBinding = BottomSheetAdvancedSearchBinding.inflate(
            LayoutInflater.from(this)
        )

        setupDropdownMenus(dialogBinding)
        restoreFilters(dialogBinding)
        setupDialogListeners(dialogBinding, bottomSheetDialog)

        bottomSheetDialog.setContentView(dialogBinding.root)

        // 防止下滑关闭，必须点击关闭按钮
        bottomSheetDialog.setOnDismissListener {
            // 可以在这里添加清理逻辑
        }

        // 设置对话框行为，禁用下滑关闭
        val bottomSheetBehavior = bottomSheetDialog.behavior
        bottomSheetBehavior.isDraggable = false
        bottomSheetBehavior.peekHeight = android.view.ViewGroup.LayoutParams.MATCH_PARENT

        bottomSheetDialog.show()
    }

    /**
     * 设置下拉菜单
     */
    private fun setupDropdownMenus(dialogBinding: BottomSheetAdvancedSearchBinding) {
        // 法术力值操作符
        (dialogBinding.autoCompleteMvOperator as? android.widget.AutoCompleteTextView)?.let {
            it.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, operatorOptions))
            it.setText(operatorOptions[0], false)
        }

        // 力量操作符
        (dialogBinding.autoCompletePowerOperator as? android.widget.AutoCompleteTextView)?.let {
            it.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, operatorOptions))
            it.setText(operatorOptions[0], false)
        }

        // 防御力操作符
        (dialogBinding.autoCompleteToughnessOperator as? android.widget.AutoCompleteTextView)?.let {
            it.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, operatorOptions))
            it.setText(operatorOptions[0], false)
        }

        // 赛制
        (dialogBinding.autoCompleteFormat as? android.widget.AutoCompleteTextView)?.let {
            it.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, formatOptions))
        }

        // 可用性
        (dialogBinding.autoCompleteLegality as? android.widget.AutoCompleteTextView)?.let {
            it.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, legalityOptions))
        }

        // 标识色开关监听
        dialogBinding.switchSearchColorIdentity.setOnCheckedChangeListener { _, isChecked ->
            // 启用/禁用标识色 Chip
            listOf(
                dialogBinding.chipCiWhite,
                dialogBinding.chipCiBlue,
                dialogBinding.chipCiBlack,
                dialogBinding.chipCiRed,
                dialogBinding.chipCiGreen
            ).forEach { chip ->
                chip.isEnabled = isChecked
            }
        }
    }

    /**
     * 恢复当前筛选条件
     */
    private fun restoreFilters(dialogBinding: BottomSheetAdvancedSearchBinding) {
        // 恢复名称
        dialogBinding.editTextName.setText(currentFilters.name ?: "")

        // 恢复规则概述
        dialogBinding.editTextOracleText.setText(currentFilters.oracleText ?: "")

        // 恢复类别
        dialogBinding.editTextType.setText(currentFilters.type ?: "")

        // 恢复颜色筛选
        currentFilters.colors.forEach { color ->
            when (color.lowercase()) {
                "w" -> dialogBinding.chipWhite.isChecked = true
                "u" -> dialogBinding.chipBlue.isChecked = true
                "b" -> dialogBinding.chipBlack.isChecked = true
                "r" -> dialogBinding.chipRed.isChecked = true
                "g" -> dialogBinding.chipGreen.isChecked = true
                "c" -> dialogBinding.chipColorless.isChecked = true
            }
        }

        // 恢复颜色模式
        when (currentFilters.colorMode) {
            ColorMatchMode.EXACT -> dialogBinding.radioColorExact.isChecked = true
            ColorMatchMode.AT_MOST -> dialogBinding.radioColorAtMost.isChecked = true
            ColorMatchMode.AT_LEAST -> dialogBinding.radioColorAtLeast.isChecked = true
        }

        // 恢复标识色
        dialogBinding.switchSearchColorIdentity.isChecked = currentFilters.searchColorIdentity
        currentFilters.colorIdentity?.forEach { color ->
            when (color.lowercase()) {
                "w" -> dialogBinding.chipCiWhite.isChecked = true
                "u" -> dialogBinding.chipCiBlue.isChecked = true
                "b" -> dialogBinding.chipCiBlack.isChecked = true
                "r" -> dialogBinding.chipCiRed.isChecked = true
                "g" -> dialogBinding.chipCiGreen.isChecked = true
            }
        }

        // 恢复法术力值
        currentFilters.manaValue?.let { mv ->
            val opIndex = when (mv.operator) {
                CompareOperator.EQUAL -> 1
                CompareOperator.GREATER -> 2
                CompareOperator.LESS -> 3
                CompareOperator.GREATER_EQUAL -> 4
                CompareOperator.LESS_EQUAL -> 5
                CompareOperator.ANY -> 0
            }
            (dialogBinding.autoCompleteMvOperator as? android.widget.AutoCompleteTextView)?.setText(operatorOptions[opIndex], false)
            dialogBinding.editTextMvValue.setText(mv.value.toString())
        }

        // 恢复力量
        currentFilters.power?.let { p ->
            val opIndex = when (p.operator) {
                CompareOperator.EQUAL -> 1
                CompareOperator.GREATER -> 2
                CompareOperator.LESS -> 3
                CompareOperator.GREATER_EQUAL -> 4
                CompareOperator.LESS_EQUAL -> 5
                CompareOperator.ANY -> 0
            }
            (dialogBinding.autoCompletePowerOperator as? android.widget.AutoCompleteTextView)?.setText(operatorOptions[opIndex], false)
            dialogBinding.editTextPowerValue.setText(p.value.toString())
        }

        // 恢复防御力
        currentFilters.toughness?.let { t ->
            val opIndex = when (t.operator) {
                CompareOperator.EQUAL -> 1
                CompareOperator.GREATER -> 2
                CompareOperator.LESS -> 3
                CompareOperator.GREATER_EQUAL -> 4
                CompareOperator.LESS_EQUAL -> 5
                CompareOperator.ANY -> 0
            }
            (dialogBinding.autoCompleteToughnessOperator as? android.widget.AutoCompleteTextView)?.setText(operatorOptions[opIndex], false)
            dialogBinding.editTextToughnessValue.setText(t.value.toString())
        }

        // 恢复赛制
        currentFilters.format?.let { format ->
            (dialogBinding.autoCompleteFormat as? android.widget.AutoCompleteTextView)?.setText(format.displayName, false)
        }

        // 恢复可用性
        currentFilters.legality?.let { legality ->
            val text = when (legality) {
                LegalityMode.LEGAL -> "合法"
                LegalityMode.BANNED -> "禁用"
                LegalityMode.RESTRICTED -> "限用"
            }
            (dialogBinding.autoCompleteLegality as? android.widget.AutoCompleteTextView)?.setText(text, false)
        }

        // 恢复系列
        dialogBinding.editTextSet.setText(currentFilters.setCode ?: "")

        // 恢复稀有度
        currentFilters.rarities.forEach { rarity ->
            when (rarity.lowercase()) {
                "common" -> dialogBinding.chipCommon.isChecked = true
                "uncommon" -> dialogBinding.chipUncommon.isChecked = true
                "rare" -> dialogBinding.chipRare.isChecked = true
                "mythic" -> dialogBinding.chipMythic.isChecked = true
                "special" -> dialogBinding.chipSpecial.isChecked = true
            }
        }

        // 恢复背景叙述
        dialogBinding.editTextFlavorText.setText(currentFilters.flavorText ?: "")

        // 恢复画师
        dialogBinding.editTextArtist.setText(currentFilters.artist ?: "")

        // 恢复游戏平台
        when (currentFilters.game) {
            GamePlatform.PAPER -> dialogBinding.radioGamePaper.isChecked = true
            GamePlatform.MTGO -> dialogBinding.radioGameMtgo.isChecked = true
            GamePlatform.ARENA -> dialogBinding.radioGameArena.isChecked = true
            null -> { /* 不选择 */ }
        }

        // 恢复额外卡牌
        dialogBinding.checkBoxIncludeExtras.isChecked = currentFilters.includeExtras
    }

    /**
     * 设置对话框按钮监听
     */
    private fun setupDialogListeners(
        dialogBinding: BottomSheetAdvancedSearchBinding,
        dialog: BottomSheetDialog
    ) {
        // 关闭按钮
        dialogBinding.buttonClose.setOnClickListener {
            dialog.dismiss()
        }

        // 重置按钮
        dialogBinding.buttonReset.setOnClickListener {
            resetAllFilters(dialogBinding)
        }

        // 清除法术力值按钮
        dialogBinding.buttonClearMv.setOnClickListener {
            (dialogBinding.autoCompleteMvOperator as? android.widget.AutoCompleteTextView)?.setText(operatorOptions[0], false)
            dialogBinding.editTextMvValue.text?.clear()
        }

        // 清除力量/防御力按钮
        dialogBinding.buttonClearPowerToughness.setOnClickListener {
            (dialogBinding.autoCompletePowerOperator as? android.widget.AutoCompleteTextView)?.setText(operatorOptions[0], false)
            dialogBinding.editTextPowerValue.text?.clear()
            (dialogBinding.autoCompleteToughnessOperator as? android.widget.AutoCompleteTextView)?.setText(operatorOptions[0], false)
            dialogBinding.editTextToughnessValue.text?.clear()
        }

        // 搜索按钮
        dialogBinding.buttonSearch.setOnClickListener {
            currentFilters = collectFilters(dialogBinding)
            updateFilterButton()
            performSearch()
            dialog.dismiss()
        }
    }

    /**
     * 重置所有筛选条件
     */
    private fun resetAllFilters(dialogBinding: BottomSheetAdvancedSearchBinding) {
        dialogBinding.editTextName.text?.clear()
        dialogBinding.editTextOracleText.text?.clear()
        dialogBinding.editTextType.text?.clear()

        clearAllChips(dialogBinding.chipGroupColors)
        clearAllChips(dialogBinding.chipGroupColorIdentity)
        clearAllChips(dialogBinding.chipGroupRarity)

        dialogBinding.radioColorExact.isChecked = true
        dialogBinding.switchSearchColorIdentity.isChecked = false

        (dialogBinding.autoCompleteMvOperator as? android.widget.AutoCompleteTextView)?.setText(operatorOptions[0], false)
        dialogBinding.editTextMvValue.text?.clear()

        (dialogBinding.autoCompletePowerOperator as? android.widget.AutoCompleteTextView)?.setText(operatorOptions[0], false)
        dialogBinding.editTextPowerValue.text?.clear()

        (dialogBinding.autoCompleteToughnessOperator as? android.widget.AutoCompleteTextView)?.setText(operatorOptions[0], false)
        dialogBinding.editTextToughnessValue.text?.clear()

        (dialogBinding.autoCompleteFormat as? android.widget.AutoCompleteTextView)?.text?.clear()
        (dialogBinding.autoCompleteLegality as? android.widget.AutoCompleteTextView)?.text?.clear()

        dialogBinding.editTextSet.text?.clear()
        dialogBinding.editTextFlavorText.text?.clear()
        dialogBinding.editTextArtist.text?.clear()

        dialogBinding.radioGroupGame.clearCheck()
        dialogBinding.checkBoxIncludeExtras.isChecked = false
    }

    /**
     * 从底部表单收集筛选条件
     */
    private fun collectFilters(dialogBinding: BottomSheetAdvancedSearchBinding): SearchFilters {
        // 收集颜色
        val colors = getCheckedChips(dialogBinding.chipGroupColors)

        // 收集颜色模式
        val colorMode = when (dialogBinding.radioGroupColorMode.checkedRadioButtonId) {
            R.id.radioColorExact -> ColorMatchMode.EXACT
            R.id.radioColorAtMost -> ColorMatchMode.AT_MOST
            R.id.radioColorAtLeast -> ColorMatchMode.AT_LEAST
            else -> ColorMatchMode.AT_LEAST
        }

        // 收集标识色
        val colorIdentity = if (dialogBinding.switchSearchColorIdentity.isChecked) {
            getCheckedChips(dialogBinding.chipGroupColorIdentity).takeIf { it.isNotEmpty() }
        } else null

        // 收集法术力值
        val mvText = dialogBinding.editTextMvValue.text?.toString()
        val mvOperator = (dialogBinding.autoCompleteMvOperator as? android.widget.AutoCompleteTextView)?.text?.toString() ?: "任意"
        val manaValue = if (mvText?.isNotBlank() == true && mvOperator != "任意") {
            val operator = when (mvOperator) {
                "=" -> CompareOperator.EQUAL
                ">" -> CompareOperator.GREATER
                "<" -> CompareOperator.LESS
                ">=" -> CompareOperator.GREATER_EQUAL
                "<=" -> CompareOperator.LESS_EQUAL
                else -> CompareOperator.ANY
            }
            NumericFilter(operator, mvText.toIntOrNull() ?: 0)
        } else null

        // 收集力量
        val powerText = dialogBinding.editTextPowerValue.text?.toString()
        val powerOperator = (dialogBinding.autoCompletePowerOperator as? android.widget.AutoCompleteTextView)?.text?.toString() ?: "任意"
        val power = if (powerText?.isNotBlank() == true && powerOperator != "任意") {
            val operator = when (powerOperator) {
                "=" -> CompareOperator.EQUAL
                ">" -> CompareOperator.GREATER
                "<" -> CompareOperator.LESS
                ">=" -> CompareOperator.GREATER_EQUAL
                "<=" -> CompareOperator.LESS_EQUAL
                else -> CompareOperator.ANY
            }
            NumericFilter(operator, powerText.toIntOrNull() ?: 0)
        } else null

        // 收集防御力
        val toughnessText = dialogBinding.editTextToughnessValue.text?.toString()
        val toughnessOperator = (dialogBinding.autoCompleteToughnessOperator as? android.widget.AutoCompleteTextView)?.text?.toString() ?: "任意"
        val toughness = if (toughnessText?.isNotBlank() == true && toughnessOperator != "任意") {
            val operator = when (toughnessOperator) {
                "=" -> CompareOperator.EQUAL
                ">" -> CompareOperator.GREATER
                "<" -> CompareOperator.LESS
                ">=" -> CompareOperator.GREATER_EQUAL
                "<=" -> CompareOperator.LESS_EQUAL
                else -> CompareOperator.ANY
            }
            NumericFilter(operator, toughnessText.toIntOrNull() ?: 0)
        } else null

        // 收集赛制
        val formatText = (dialogBinding.autoCompleteFormat as? android.widget.AutoCompleteTextView)?.text?.toString()
        val format = formatText?.let { Format.values().find { f -> f.displayName == it } }

        // 收集可用性
        val legalityText = (dialogBinding.autoCompleteLegality as? android.widget.AutoCompleteTextView)?.text?.toString()
        val legality = legalityText?.let {
            when (it) {
                "合法" -> LegalityMode.LEGAL
                "禁用" -> LegalityMode.BANNED
                "限用" -> LegalityMode.RESTRICTED
                else -> null
            }
        }

        // 收集稀有度
        val rarities = getCheckedChips(dialogBinding.chipGroupRarity)

        // 收集游戏平台
        val game = when (dialogBinding.radioGroupGame.checkedRadioButtonId) {
            R.id.radioGamePaper -> GamePlatform.PAPER
            R.id.radioGameMtgo -> GamePlatform.MTGO
            R.id.radioGameArena -> GamePlatform.ARENA
            else -> null
        }

        return SearchFilters(
            name = dialogBinding.editTextName.text?.toString()?.takeIf { it.isNotBlank() },
            oracleText = dialogBinding.editTextOracleText.text?.toString()?.takeIf { it.isNotBlank() },
            type = dialogBinding.editTextType.text?.toString()?.takeIf { it.isNotBlank() },
            colors = colors,
            colorMode = colorMode,
            colorIdentity = colorIdentity,
            searchColorIdentity = dialogBinding.switchSearchColorIdentity.isChecked,
            manaValue = manaValue,
            power = power,
            toughness = toughness,
            format = format,
            legality = legality,
            setCode = dialogBinding.editTextSet.text?.toString()?.takeIf { it.isNotBlank() },
            rarities = rarities,
            flavorText = dialogBinding.editTextFlavorText.text?.toString()?.takeIf { it.isNotBlank() },
            artist = dialogBinding.editTextArtist.text?.toString()?.takeIf { it.isNotBlank() },
            game = game,
            includeExtras = dialogBinding.checkBoxIncludeExtras.isChecked
        )
    }

    /**
     * 获取 ChipGroup 中所有选中的 Chip 的值
     */
    private fun getCheckedChips(chipGroup: ChipGroup): List<String> {
        val checkedValues = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                checkedValues.add(chip.text?.toString()?.lowercase() ?: "")
            }
        }
        return checkedValues
    }

    /**
     * 清除 ChipGroup 中所有 Chip 的选中状态
     */
    private fun clearAllChips(chipGroup: ChipGroup) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.isChecked = false
        }
    }

    /**
     * 更新筛选按钮显示
     */
    private fun updateFilterButton() {
        binding.buttonFilters.text = if (currentFilters.hasActiveFilters) "筛选*" else "筛选"
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
}
