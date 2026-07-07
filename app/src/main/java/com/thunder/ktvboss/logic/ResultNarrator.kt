package com.thunder.ktvboss.logic

import com.thunder.ktvboss.model.BattleResult

object ResultNarrator {

    fun createResult(
        victory: Boolean,
        bossName: String,
        maxCombo: Int,
        totalDamage: Int,
        ultimateCount: Int,
        hitCount: Int,
        durationSeconds: Int
    ): BattleResult {
        val title = when {
            victory && maxCombo >= 18 -> "副歌终结者"
            victory && ultimateCount >= 2 -> "高能爆发体"
            victory -> "热血攻擂王"
            maxCombo >= 10 -> "倔强追击者"
            else -> "不服输的挑战者"
        }

        val summary = when {
            victory -> "你用持续输出压爆了 $bossName 的血线，把副歌唱成了处刑现场。"
            totalDamage >= 700 -> "虽然没有彻底击倒 $bossName，但整场压制感已经出来了。"
            else -> "这局更像试音热身，下一次只要把高潮段顶住就能翻盘。"
        }

        val comment = buildString {
            append("AI评委：")
            append(
                when {
                    victory && ultimateCount >= 2 ->
                        "你今天的声音像开了大招，高潮段一到，Boss 基本没有还手空间。"
                    victory ->
                        "你的输出节奏很稳，尤其是连续命中阶段，已经有包厢主控台的压场感。"
                    maxCombo >= 10 ->
                        "你的中段状态不错，但副歌爆发还差最后一口气。补强持续输出就能拿下。"
                    else ->
                        "气势已经到位了，下一版建议把开唱前 10 秒当热身，把声场先撑起来。"
                }
            )
        }

        return BattleResult(
            victory = victory,
            bossName = bossName,
            playerTitle = title,
            summary = summary,
            aiComment = comment,
            maxCombo = maxCombo,
            totalDamage = totalDamage,
            ultimateCount = ultimateCount,
            hitCount = hitCount,
            durationSeconds = durationSeconds
        )
    }
}
