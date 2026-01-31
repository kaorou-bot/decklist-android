package com.mtgo.decklistmanager.ui.search

import android.os.Bundle
import android.view.MenuItem
import android.view.LayoutInflater
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ActivitySearchBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 搜索页面 - 显示搜索结果和搜索历史
 */
@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var binding: ActivitySearchBinding

    private lateinit var resultAdapter: SearchResultAdapter
    private lateinit var historyAdapter: SearchHistoryAdapter

    // 当前应用的筛选条件
    private var currentFilters: SearchFilters? = null
    private var cmcOperator: String = "any"

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
            // 显示卡牌详情
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
     */
    private fun showCardDetail(result: SearchResultItem) {
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

        // 构建详细信息文本（处理 \n 换行符）
        val details = buildString {
            appendLine("卡牌名称：${result.displayName ?: result.name}")
            appendLine("英文名称：${result.name}")
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

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(result.displayName ?: result.name)
            .setView(dialogBinding.root)
            .setPositiveButton("关闭", null)
            .show()
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
     * 显示底部表单高级筛选
     */
    private fun showFiltersDialog() {
        // 使用 BottomSheetDialog
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val dialogBinding = com.mtgo.decklistmanager.databinding.BottomSheetAdvancedSearchBinding.inflate(
            LayoutInflater.from(this)
        )

        // 设置 CMC 操作符按钮（互斥选择）
        val cmcButtons = listOf(
            dialogBinding.buttonCmcAny to "any",
            dialogBinding.buttonCmcEqual to "=",
            dialogBinding.buttonCmcGreater to ">",
            dialogBinding.buttonCmcLess to "<"
        )

        // 恢复当前选择的 CMC 操作符
        cmcButtons.forEach { (button, op) ->
            button.isChecked = (op == cmcOperator)
            button.setOnClickListener {
                cmcOperator = op
                cmcButtons.forEach { (b, _) -> b.isChecked = (b == button) }
            }
        }

        // 恢复当前的筛选条件
        currentFilters?.let { filters ->
            // 恢复颜色筛选
            filters.colors.forEach { color ->
                when (color) {
                    "w" -> dialogBinding.chipWhite.isChecked = true
                    "u" -> dialogBinding.chipBlue.isChecked = true
                    "b" -> dialogBinding.chipBlack.isChecked = true
                    "r" -> dialogBinding.chipRed.isChecked = true
                    "g" -> dialogBinding.chipGreen.isChecked = true
                    "c" -> dialogBinding.chipColorless.isChecked = true
                }
            }

            // 恢复颜色标识筛选
            filters.colorIdentity?.forEach { color ->
                when (color) {
                    "w" -> dialogBinding.chipCiWhite.isChecked = true
                    "u" -> dialogBinding.chipCiBlue.isChecked = true
                    "b" -> dialogBinding.chipCiBlack.isChecked = true
                    "r" -> dialogBinding.chipCiRed.isChecked = true
                    "g" -> dialogBinding.chipCiGreen.isChecked = true
                }
            }

            // 恢复 CMC 值
            filters.cmc?.let { cmc ->
                dialogBinding.editTextCmc.setText(cmc.value.toString())
            }

            // 恢复类型筛选
            filters.types?.forEach { type ->
                when (type) {
                    "creature" -> dialogBinding.chipCreature.isChecked = true
                    "instant" -> dialogBinding.chipInstant.isChecked = true
                    "sorcery" -> dialogBinding.chipSorcery.isChecked = true
                    "artifact" -> dialogBinding.chipArtifact.isChecked = true
                    "enchantment" -> dialogBinding.chipEnchantment.isChecked = true
                    "planeswalker" -> dialogBinding.chipPlaneswalker.isChecked = true
                    "land" -> dialogBinding.chipLand.isChecked = true
                }
            }

            // 恢复稀有度筛选
            filters.rarity?.let { rarity ->
                when (rarity) {
                    "common" -> dialogBinding.chipCommon.isChecked = true
                    "uncommon" -> dialogBinding.chipUncommon.isChecked = true
                    "rare" -> dialogBinding.chipRare.isChecked = true
                    "mythic" -> dialogBinding.chipMythic.isChecked = true
                }
            }

            // 恢复系列代码
            filters.set?.let { setCode ->
                dialogBinding.editTextSet.setText(setCode)
            }

            // 恢复伙伴颜色
            filters.partner?.let { partner ->
                when (partner) {
                    "white" -> dialogBinding.chipPartnerWhite.isChecked = true
                    "blue" -> dialogBinding.chipPartnerBlue.isChecked = true
                    "black" -> dialogBinding.chipPartnerBlack.isChecked = true
                    "red" -> dialogBinding.chipPartnerRed.isChecked = true
                    "green" -> dialogBinding.chipPartnerGreen.isChecked = true
                }
            }
        }

        // 关闭按钮
        dialogBinding.buttonClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // 重置按钮
        dialogBinding.buttonReset.setOnClickListener {
            clearAllChips(dialogBinding.chipGroupColors)
            clearAllChips(dialogBinding.chipGroupColorIdentity)
            clearAllChips(dialogBinding.chipGroupTypes)
            clearAllChips(dialogBinding.chipGroupRarity)
            clearAllChips(dialogBinding.chipGroupPartner)
            dialogBinding.editTextQuery.text?.clear()
            dialogBinding.editTextCmc.text?.clear()
            dialogBinding.editTextSet.text?.clear()
            cmcOperator = "="
            cmcButtons.forEach { (b, _) -> b.isChecked = (b == dialogBinding.buttonCmcEqual) }
        }

        // 搜索按钮
        dialogBinding.buttonSearch.setOnClickListener {
            // 更新搜索关键词（如果用户在底部表单中输入了）
            val bottomQuery = dialogBinding.editTextQuery.text?.toString() ?: ""
            if (bottomQuery.isNotBlank()) {
                binding.editTextQuery.setText(bottomQuery)
            }

            // 收集筛选条件
            currentFilters = collectFiltersFromBottomSheet(dialogBinding)
            updateFilterButton()

            // 执行搜索并关闭底部表单
            performSearch()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(dialogBinding.root)
        bottomSheetDialog.show()
    }

    /**
     * 从底部表单收集筛选条件
     */
    private fun collectFiltersFromBottomSheet(
        dialogBinding: com.mtgo.decklistmanager.databinding.BottomSheetAdvancedSearchBinding
    ): SearchFilters {
        // 收集颜色筛选
        val colors = getCheckedChipIds(dialogBinding.chipGroupColors).mapNotNull { chipId ->
            when (chipId) {
                R.id.chipWhite -> "w"
                R.id.chipBlue -> "u"
                R.id.chipBlack -> "b"
                R.id.chipRed -> "r"
                R.id.chipGreen -> "g"
                R.id.chipColorless -> "c"
                else -> null
            }
        }

        // 收集颜色标识筛选
        val colorIdentity = getCheckedChipIds(dialogBinding.chipGroupColorIdentity).mapNotNull { chipId ->
            when (chipId) {
                R.id.chipCiWhite -> "w"
                R.id.chipCiBlue -> "u"
                R.id.chipCiBlack -> "b"
                R.id.chipCiRed -> "r"
                R.id.chipCiGreen -> "g"
                else -> null
            }
        }.takeIf { it.isNotEmpty() }

        // 收集 CMC 筛选（仅当操作符不是 "any" 时）
        val cmc = if (cmcOperator != "any") {
            dialogBinding.editTextCmc.text?.toString()?.toIntOrNull()?.let { value ->
                CmcFilter(operator = cmcOperator, value = value)
            }
        } else null

        // 收集类型筛选
        val types = getCheckedChipIds(dialogBinding.chipGroupTypes).mapNotNull { chipId ->
            when (chipId) {
                R.id.chipCreature -> "creature"
                R.id.chipInstant -> "instant"
                R.id.chipSorcery -> "sorcery"
                R.id.chipArtifact -> "artifact"
                R.id.chipEnchantment -> "enchantment"
                R.id.chipPlaneswalker -> "planeswalker"
                R.id.chipLand -> "land"
                else -> null
            }
        }.takeIf { it.isNotEmpty() }

        // 收集稀有度筛选
        val rarity = getCheckedChipIds(dialogBinding.chipGroupRarity).firstNotNullOfOrNull { chipId ->
            when (chipId) {
                R.id.chipCommon -> "common"
                R.id.chipUncommon -> "uncommon"
                R.id.chipRare -> "rare"
                R.id.chipMythic -> "mythic"
                else -> null
            }
        }

        // 收集系列代码
        val setCode = dialogBinding.editTextSet.text?.toString()?.takeIf { it.isNotBlank() }

        // 收集伙伴颜色
        val partner = getCheckedChipIds(dialogBinding.chipGroupPartner).firstNotNullOfOrNull { chipId ->
            when (chipId) {
                R.id.chipPartnerWhite -> "white"
                R.id.chipPartnerBlue -> "blue"
                R.id.chipPartnerBlack -> "black"
                R.id.chipPartnerRed -> "red"
                R.id.chipPartnerGreen -> "green"
                else -> null
            }
        }

        return SearchFilters(
            colors = colors,
            colorIdentity = colorIdentity,
            cmc = cmc,
            types = types,
            rarity = rarity,
            set = setCode,
            partner = partner
        )
    }

    /**
     * 获取 ChipGroup 中所有选中的 Chip ID
     */
    private fun getCheckedChipIds(chipGroup: ChipGroup): List<Int> {
        val checkedIds = mutableListOf<Int>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                checkedIds.add(chip.id)
            }
        }
        return checkedIds
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
        val hasFilters = currentFilters?.let {
            it.colors.isNotEmpty() ||
            it.colorIdentity != null ||
            it.cmc != null ||
            it.types != null ||
            it.rarity != null ||
            it.set != null ||
            it.partner != null
        } ?: false

        binding.buttonFilters.text = if (hasFilters) "筛选*" else "筛选"
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
