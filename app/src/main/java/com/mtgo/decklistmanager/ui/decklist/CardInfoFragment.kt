package com.mtgo.decklistmanager.ui.decklist

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mtgo.decklistmanager.databinding.DialogCardInfoBinding
import com.mtgo.decklistmanager.domain.model.CardInfo

/**
 * Card Info Dialog - 卡牌信息弹窗
 */
class CardInfoFragment : DialogFragment() {

    private var _binding: DialogCardInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCardInfoBinding.inflate(layoutInflater)

        val cardInfo = arguments?.getParcelable<CardInfo>(ARG_CARD_INFO)
        cardInfo?.let {
            displayCardInfo(it)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Close") { _, _ ->
                dismiss()
            }
            .create()
    }

    private fun displayCardInfo(cardInfo: CardInfo) {
        binding.apply {
            // Load image
            cardInfo.imageUriNormal?.let { imageUrl ->
                Glide.with(this@CardInfoFragment)
                    .load(imageUrl)
                    .into(ivCardImage)
            }

            // Card info
            tvCardName.text = cardInfo.name
            tvManaCost.text = cardInfo.manaCost ?: ""
            tvTypeLine.text = cardInfo.typeLine ?: ""
            tvOracleText.text = cardInfo.oracleText ?: ""

            // Power/Toughness
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
                cardInfo.setName ?: ""
            }
            tvSetInfo.text = setInfo
            tvArtist.text = cardInfo.artist?.let { "Artist: $it" } ?: ""

            // Price
            tvPrice.text = cardInfo.priceUsd?.let { "Price: $$it USD" } ?: "Price: N/A"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CARD_INFO = "card_info"

        fun newInstance(cardInfo: CardInfo): CardInfoFragment {
            return CardInfoFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CARD_INFO, cardInfo)
                }
            }
        }
    }
}
