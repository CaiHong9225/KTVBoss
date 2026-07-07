package com.thunder.ktvboss.model

import java.io.Serializable

data class BattleResult(
    val victory: Boolean,
    val bossName: String,
    val playerTitle: String,
    val summary: String,
    val aiComment: String,
    val maxCombo: Int,
    val totalDamage: Int,
    val ultimateCount: Int,
    val hitCount: Int,
    val durationSeconds: Int
) : Serializable

data class BattleSnapshot(
    val bossName: String,
    val segmentName: String,
    val bossHp: Int,
    val bossHpMax: Int,
    val playerHp: Int,
    val playerHpMax: Int,
    val energy: Int,
    val combo: Int,
    val maxCombo: Int,
    val volumeLevel: Int,
    val totalDamage: Int,
    val message: String,
    val isUltimate: Boolean,
    val isFinished: Boolean
)
