package com.thunder.ktvboss.logic

import com.thunder.ktvboss.model.BattleResult
import com.thunder.ktvboss.model.BattleSnapshot
import kotlin.math.max
import kotlin.math.min

class BattleEngine(
    private val bossName: String = "噪音吞噬者",
    private val durationMs: Long = 60_000L
) {

    private val bossHpMax = 1000
    private val playerHpMax = 100

    private var bossHp = bossHpMax
    private var playerHp = playerHpMax
    private var energy = 0
    private var combo = 0
    private var maxCombo = 0
    private var totalDamage = 0
    private var hitCount = 0
    private var ultimateCount = 0
    private var silentTicks = 0
    private var lastUltimateTick = -100
    private var tickCount = 0
    private var lastMessage = "Boss 登场，准备开唱"

    fun update(elapsedMs: Long, volumeLevel: Int): BattleSnapshot {
        tickCount += 1

        val segmentName = when {
            elapsedMs < 12_000L -> "开场蓄力"
            elapsedMs < 28_000L -> "主歌推进"
            elapsedMs < 42_000L -> "副歌连击"
            elapsedMs < 52_000L -> "高潮爆发"
            else -> "收尾冲刺"
        }

        val attackThreshold = when (segmentName) {
            "高潮爆发" -> 35
            "副歌连击" -> 28
            else -> 24
        }

        var message = lastMessage
        var isUltimate = false

        if (volumeLevel >= attackThreshold) {
            silentTicks = 0
            val baseDamage = when {
                volumeLevel >= 80 -> 22
                volumeLevel >= 60 -> 16
                else -> 10
            }
            val comboBonus = min(combo, 15) * 2
            val segmentBonus = when (segmentName) {
                "副歌连击" -> 8
                "高潮爆发" -> 15
                else -> 3
            }
            var damage = baseDamage + comboBonus + segmentBonus
            combo += 1
            maxCombo = max(maxCombo, combo)
            hitCount += 1
            energy = min(100, energy + 8 + volumeLevel / 12)

            if (segmentName == "高潮爆发" && energy >= 70 && tickCount - lastUltimateTick >= 10) {
                damage += 120
                energy = 15
                ultimateCount += 1
                lastUltimateTick = tickCount
                isUltimate = true
                message = "必杀技触发，Boss 护盾被撕开"
            } else {
                message = if (combo >= 8) "连击继续，Boss 已被压制" else "命中成功，持续输出"
            }

            bossHp = max(0, bossHp - damage)
            totalDamage += damage
        } else {
            combo = 0
            silentTicks += 1
            if (silentTicks >= 4 && silentTicks % 3 == 1) {
                playerHp = max(0, playerHp - 6)
                message = "Boss 反击，别让声场掉下来"
            } else {
                message = "正在蓄力，抬高声音继续压制"
            }
        }

        val finished = bossHp <= 0 || playerHp <= 0 || elapsedMs >= durationMs
        lastMessage = message

        return BattleSnapshot(
            bossName = bossName,
            segmentName = segmentName,
            bossHp = bossHp,
            bossHpMax = bossHpMax,
            playerHp = playerHp,
            playerHpMax = playerHpMax,
            energy = energy,
            combo = combo,
            maxCombo = maxCombo,
            volumeLevel = volumeLevel,
            totalDamage = totalDamage,
            message = message,
            isUltimate = isUltimate,
            isFinished = finished
        )
    }

    fun buildResult(elapsedMs: Long): BattleResult {
        val victory = bossHp <= 0 || (bossHp <= bossHpMax * 0.2f && playerHp > 0)
        return ResultNarrator.createResult(
            victory = victory,
            bossName = bossName,
            maxCombo = maxCombo,
            totalDamage = totalDamage,
            ultimateCount = ultimateCount,
            hitCount = hitCount,
            durationSeconds = (elapsedMs / 1000L).toInt()
        )
    }
}
