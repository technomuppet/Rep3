package com.replog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records when a user chose to train despite the recovery system recommending a
 * rest day ("Train Anyway"). Kept locally and fed back into future recovery
 * intelligence (e.g. learning a user's true tolerance for training on lower
 * recovery scores).
 */
@Entity(tableName = "rest_day_overrides")
data class RestDayOverride(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    /** Recovery score at the moment of override, if known (0-100). */
    val recoveryScore: Int? = null,
    /** The reason text the coach showed for recommending rest. */
    val recommendationReason: String? = null
)
