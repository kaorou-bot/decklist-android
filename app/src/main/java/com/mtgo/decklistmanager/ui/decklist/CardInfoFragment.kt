package com.mtgo.decklistmanager.ui.decklist

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import com.mtgo.decklistmanager.databinding.DialogCardInfoBinding
import com.mtgo.decklistmanager.domain.model.CardInfo

/**
 * Card Info Dialog - 卡牌信息弹窗
 */
class CardInfoFragment : DialogFragment() {

    private var _binding: DialogCardInfoBinding? = null
    private val binding get() = _binding!!

    private var currentCardInfo: CardInfo? = null
    private var isShowingFront = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCardInfoBinding.inflate(layoutInflater)

        val cardInfo = arguments?.getParcelable(ARG_CARD_INFO, CardInfo::class.java)
        cardInfo?.let {
            currentCardInfo = it
            isShowingFront = true
            displayCardInfo(it)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton("关闭") { _, _ ->
                dismiss()
            }
            .create()
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
        binding.apply {
            // Update flip button visibility
            if (cardInfo.isDualFaced) {
                btnFlipCard.visibility = View.VISIBLE
                btnFlipCard.text = if (isShowingFront) "查看反面" else "查看正面"

                btnFlipCard.setOnClickListener {
                    isShowingFront = !isShowingFront
                    updateCardDisplay()
                }
            } else {
                btnFlipCard.visibility = View.GONE
            }

            updateCardDisplay()
        }
    }

    private fun updateCardDisplay() {
        val cardInfo = currentCardInfo ?: return

        binding.apply {
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
                Glide.with(this@CardInfoFragment)
                    .load(it)
                    .into(ivCardImage)
            }

            // Update button text
            if (cardInfo.isDualFaced) {
                btnFlipCard.text = if (isShowingFront) "查看反面" else "查看正面"
            }

            // Card info - display based on current face
            if (cardInfo.isDualFaced && !isShowingFront) {
                // Show back face info
                tvCardName.text = cardInfo.backFaceName ?: cardInfo.name
                tvManaCost.text = ""  // Back face usually doesn't have mana cost in this model
                tvTypeLine.text = ""   // Would need back face type line
                tvOracleText.text = "" // Would need back face oracle text
                tvPowerToughness.visibility = View.GONE
            } else {
                // Show front face info
                tvCardName.text = cardInfo.name
                tvManaCost.text = cardInfo.manaCost ?: ""
                tvTypeLine.text = cardInfo.typeLine ?: ""
                tvOracleText.text = formatText(cardInfo.oracleText)

                // Power/Toughness
                if (!cardInfo.power.isNullOrEmpty() && !cardInfo.toughness.isNullOrEmpty()) {
                    tvPowerToughness.text = "${cardInfo.power}/${cardInfo.toughness}"
                    tvPowerToughness.visibility = View.VISIBLE
                } else {
                    tvPowerToughness.visibility = View.GONE
                }
            }

            // Set info
            val setInfo = if (!cardInfo.setName.isNullOrEmpty() && !cardInfo.setCode.isNullOrEmpty()) {
                "${cardInfo.setName} (${cardInfo.setCode})"
            } else {
                cardInfo.setName ?: ""
            }
            tvSetInfo.text = setInfo
            tvArtist.text = cardInfo.artist?.let { "Artist: $it" } ?: ""
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
