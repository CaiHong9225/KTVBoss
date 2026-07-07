package com.thunder.ktvboss.boss

import com.thunder.ktvboss.R
import kotlin.random.Random

object BossSession {

    private var bossArt: BossArt? = null

    fun current(): BossArt {
        val cached = bossArt
        if (cached != null) return cached

        val candidates = listOf(
            BossArt(name = "噪音吞噬者·01", localResId = R.drawable.boss1, imageUrl = null),
            BossArt(name = "噪音吞噬者·02", localResId = R.drawable.boss2, imageUrl = null),
            BossArt(name = "噪音吞噬者·03", localResId = R.drawable.boss3, imageUrl = null),
            BossArt(name = "噪音吞噬者·04", localResId = R.drawable.boss4, imageUrl = null),
            BossArt(name = "噪音吞噬者·05", localResId = R.drawable.boss5, imageUrl = null),
            BossArt(name = "噪音吞噬者·06", localResId = R.drawable.boss6, imageUrl = null)
        )
        val chosen = candidates[Random.nextInt(candidates.size)]
        return chosen.also { bossArt = it }
    }
}
