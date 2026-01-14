package com.mtgo.decklistmanager.ui.carddetail

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ActivityCardDetailBinding
import com.mtgo.decklistmanager.domain.model.CardInfo
import dagger.hilt.android.AndroidEntryPoint

/**
 * Card Detail Activity - 卡牌详情页面
 */
@AndroidEntryPoint
class CardDetailActivity : AppCompatActivity() {

    private val viewModel: CardDetailViewModel by viewModels()
    private lateinit var binding: ActivityCardDetailBinding
    private lateinit var legalitiesAdapter: LegalitiesAdapter

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

    private fun displayCardInfo(cardInfo: CardInfo) {
        binding.apply {
            // Load image
            cardInfo.imageUriNormal?.let { imageUrl ->
                Glide.with(this@CardDetailActivity)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(ivCardImage)
            }

            // Card info
            tvCardName.text = cardInfo.name
            tvManaCost.text = cardInfo.manaCost ?: ""
            tvTypeLine.text = cardInfo.typeLine ?: ""
            tvOracleText.text = cardInfo.oracleText ?: ""

            // Power/Toughness for creatures
            if (!cardInfo.power.isNullOrEmpty() && !cardInfo.toughness.isNullOrEmpty()) {
                tvPowerToughness.text = "${cardInfo.power}/${cardInfo.toughness}"
                tvPowerToughness.visibility = View.VISIBLE
            } else {
                tvPowerToughness.visibility = View.GONE
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

            // Price
            cardInfo.priceUsd?.let { price ->
                tvPrice.text = "Price: $$price USD"
            } ?: run {
                tvPrice.text = "Price: N/A"
            }
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
