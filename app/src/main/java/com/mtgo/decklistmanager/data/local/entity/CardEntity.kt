package com.mtgo.decklistmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Card Entity - 卡牌表
 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["decklist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["decklist_id"]),
        Index(value = ["card_name"])
    ]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "decklist_id")
    @SerializedName("decklist_id")
    val decklistId: Long,

    @ColumnInfo(name = "card_name")
    @SerializedName("card_name")
    val cardName: String,

    @ColumnInfo(name = "display_name")
    @SerializedName("display_name")
    val displayName: String? = null,

    val quantity: Int,

    val location: String, // "main" or "sideboard"

    @ColumnInfo(name = "card_order")
    @SerializedName("card_order")
    val cardOrder: Int = 0,

    @ColumnInfo(name = "mana_cost")
    @SerializedName("mana_cost")
    val manaCost: String?,

    val rarity: String?,
    val color: String?,

    @ColumnInfo(name = "card_type")
    @SerializedName("card_type")
    val cardType: String?,

    @ColumnInfo(name = "card_set")
    @SerializedName("card_set")
    val cardSet: String?
)
