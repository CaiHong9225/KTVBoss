package com.thunder.ktvboss.boss

import com.thunder.ktvboss.R
import java.net.URLEncoder

object BossSession {

    private var bossArt: BossArt? = null

    fun current(): BossArt {
        val cached = bossArt
        if (cached != null) return cached

        val name = "噪音吞噬者"
        val localResId = R.drawable.boss_avatar_cyber

        val enableRemote = false
        if (!enableRemote) {
            return BossArt(name = name, localResId = localResId, imageUrl = null).also { bossArt = it }
        }

        val promptRaw =
            "Cyberpunk sound devourer boss portrait, neon karaoke room, glowing mask, sharp silhouette, dramatic rim light, high contrast, ultra detailed, digital art, centered composition"
        val prompt = URLEncoder.encode(promptRaw, "UTF-8")
        val url =
            "https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=$prompt&image_size=square_hd"

        return BossArt(name = name, localResId = localResId, imageUrl = url).also { bossArt = it }
    }
}
