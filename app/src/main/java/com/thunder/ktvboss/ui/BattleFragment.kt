package com.thunder.ktvboss.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.thunder.ktvboss.audio.AudioInputSampler
import com.thunder.ktvboss.databinding.FragmentBattleBinding
import com.thunder.ktvboss.logic.BattleEngine
import com.thunder.ktvboss.model.BattleResult
import com.thunder.ktvboss.model.BattleSnapshot
import kotlin.math.sin

class BattleFragment : Fragment() {

    interface Listener {
        fun onBattleFinished(result: BattleResult)
    }

    private var listener: Listener? = null
    private var _binding: FragmentBattleBinding? = null
    private val binding get() = _binding!!

    private val battleHandler = Handler(Looper.getMainLooper())
    private val engine = BattleEngine()

    private var battleStartTime = 0L
    private var battleEnded = false
    private var latestVolume = 0
    private var sampler: AudioInputSampler? = null
    private var lastBossHp: Int? = null
    private var lastPlayerHp: Int? = null
    private var lastTotalDamage: Int? = null
    private var lastCombo: Int? = null
    private var ultimateShowing = false

    private val battleLoop = object : Runnable {
        override fun run() {
            if (_binding == null || battleEnded) return

            val elapsed = SystemClock.elapsedRealtime() - battleStartTime
            val inputVolume = if (hasAudioPermission()) latestVolume else createDemoVolume(elapsed)
            val snapshot = engine.update(elapsed, inputVolume)
            render(snapshot, elapsed)
            if (snapshot.isFinished) {
                finishBattle(elapsed)
            } else {
                battleHandler.postDelayed(this, FRAME_DELAY_MS)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBattleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvBattleHint.text = if (hasAudioPermission()) {
            "对着麦克风持续输出，副歌阶段更容易打出连击和大招"
        } else {
            "当前未拿到麦克风权限，已切换为自动演示模式"
        }
        binding.btnEndNow.setOnClickListener {
            finishBattle(SystemClock.elapsedRealtime() - battleStartTime)
        }
        binding.vHitFlash.alpha = 0f
        binding.tvDamagePop.alpha = 0f
        binding.tvDamagePop.visibility = View.INVISIBLE
        binding.tvComboBurst.alpha = 0f
        binding.tvComboBurst.visibility = View.INVISIBLE
        binding.ultimateOverlay.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        startBattle()
    }

    override fun onPause() {
        super.onPause()
        stopBattleLoop()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.battleStage.animate().cancel()
        binding.vHitFlash.animate().cancel()
        binding.tvDamagePop.animate().cancel()
        binding.tvComboBurst.animate().cancel()
        binding.ultimateOverlay.animate().cancel()
        _binding = null
    }

    private fun startBattle() {
        if (battleStartTime != 0L) return
        battleStartTime = SystemClock.elapsedRealtime()
        battleEnded = false
        latestVolume = 0

        if (hasAudioPermission()) {
            sampler = AudioInputSampler(requireContext()) { latestVolume = it }.also { it.start() }
        }
        battleHandler.post(battleLoop)
    }

    private fun stopBattleLoop() {
        sampler?.stop()
        sampler = null
        battleHandler.removeCallbacksAndMessages(null)
    }

    private fun finishBattle(elapsedMs: Long) {
        if (battleEnded) return
        battleEnded = true
        stopBattleLoop()
        listener?.onBattleFinished(engine.buildResult(elapsedMs))
    }

    private fun render(snapshot: BattleSnapshot, elapsedMs: Long) {
        val bossHpDelta = lastBossHp?.let { it - snapshot.bossHp } ?: 0
        val playerHpDelta = lastPlayerHp?.let { it - snapshot.playerHp } ?: 0
        val damageDelta = lastTotalDamage?.let { snapshot.totalDamage - it } ?: 0
        val comboDelta = lastCombo?.let { snapshot.combo - it } ?: 0

        binding.tvBossName.text = snapshot.bossName
        binding.tvSegment.text = snapshot.segmentName
        binding.progressBoss.max = snapshot.bossHpMax
        binding.progressBoss.progress = snapshot.bossHp
        binding.progressPlayer.max = snapshot.playerHpMax
        binding.progressPlayer.progress = snapshot.playerHp
        binding.progressEnergy.progress = snapshot.energy
        binding.tvBossHp.text = "${snapshot.bossHp}/${snapshot.bossHpMax}"
        binding.tvPlayerHp.text = "${snapshot.playerHp}/${snapshot.playerHpMax}"
        binding.tvCombo.text = snapshot.combo.toString()
        binding.tvVolume.text = snapshot.volumeLevel.toString()
        binding.tvDamage.text = snapshot.totalDamage.toString()
        binding.tvMessage.text = snapshot.message
        binding.tvTimer.text = formatTimeLeft(elapsedMs)
        binding.tvUltimate.visibility = if (snapshot.isUltimate) View.VISIBLE else View.INVISIBLE

        if (bossHpDelta > 0 || damageDelta > 0) {
            val critical = snapshot.volumeLevel >= 70 || snapshot.segmentName == "高潮爆发"
            playHitFx(damageDelta.takeIf { it > 0 } ?: bossHpDelta, critical = critical)
        }

        if (playerHpDelta > 0) {
            playHurtFx()
        }

        if (comboDelta > 0 && snapshot.combo >= 8 && snapshot.combo % 8 == 0) {
            playComboBurst(snapshot.combo)
        }

        if (snapshot.isUltimate && !ultimateShowing) {
            playUltimateFx()
        }

        lastBossHp = snapshot.bossHp
        lastPlayerHp = snapshot.playerHp
        lastTotalDamage = snapshot.totalDamage
        lastCombo = snapshot.combo
    }

    private fun playHitFx(damage: Int, critical: Boolean) {
        flash(critical)
        shake(
            amplitudeDp = if (critical) 10f else 6f,
            cycles = if (critical) 8 else 6,
            durationMs = if (critical) 240L else 180L
        )
        showDamage(damage, critical)
        binding.battleStage.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
        )
    }

    private fun playHurtFx() {
        flash(critical = false, alphaPeak = 0.26f)
        shake(amplitudeDp = 12f, cycles = 10, durationMs = 260L)
    }

    private fun playComboBurst(combo: Int) {
        val tv = binding.tvComboBurst
        tv.text = "COMBO x$combo"
        tv.visibility = View.VISIBLE
        tv.alpha = 0f
        tv.scaleX = 0.88f
        tv.scaleY = 0.88f
        tv.animate().cancel()
        tv.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(180L)
            .setInterpolator(OvershootInterpolator(0.9f))
            .withEndAction {
                tv.animate()
                    .alpha(0f)
                    .translationYBy(-18f)
                    .setDuration(360L)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        tv.translationY = 0f
                        tv.visibility = View.INVISIBLE
                    }
                    .start()
            }
            .start()
    }

    private fun playUltimateFx() {
        ultimateShowing = true
        val overlay = binding.ultimateOverlay
        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        overlay.scaleX = 1.02f
        overlay.scaleY = 1.02f

        binding.tvUltimateTitle.apply {
            alpha = 0f
            scaleX = 0.86f
            scaleY = 0.86f
        }
        binding.tvUltimateSub.alpha = 0f

        overlay.animate().cancel()
        overlay.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(160L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.tvUltimateTitle.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220L)
                    .setInterpolator(OvershootInterpolator(0.9f))
                    .start()
                binding.tvUltimateSub.animate()
                    .alpha(1f)
                    .setStartDelay(90L)
                    .setDuration(220L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()

        mainHideUltimate(520L)
    }

    private fun mainHideUltimate(delayMs: Long) {
        battleHandler.postDelayed({
            if (_binding == null) return@postDelayed
            val overlay = binding.ultimateOverlay
            overlay.animate().cancel()
            overlay.animate()
                .alpha(0f)
                .setDuration(220L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    overlay.visibility = View.GONE
                    overlay.alpha = 1f
                    ultimateShowing = false
                }
                .start()
        }, delayMs)
    }

    private fun flash(critical: Boolean, alphaPeak: Float = if (critical) 0.44f else 0.28f) {
        val v = binding.vHitFlash
        v.animate().cancel()
        v.alpha = 0f
        v.animate()
            .alpha(alphaPeak)
            .setDuration(60L)
            .withEndAction {
                v.animate()
                    .alpha(0f)
                    .setDuration(160L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun showDamage(damage: Int, critical: Boolean) {
        val tv = binding.tvDamagePop
        tv.text = if (critical) "暴击 -$damage" else "-$damage"
        tv.visibility = View.VISIBLE
        tv.alpha = 0f
        tv.translationY = 18f
        tv.scaleX = if (critical) 1.08f else 1.02f
        tv.scaleY = if (critical) 1.08f else 1.02f
        tv.animate().cancel()
        tv.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(120L)
            .setInterpolator(OvershootInterpolator(0.8f))
            .withEndAction {
                tv.animate()
                    .alpha(0f)
                    .translationYBy(-24f)
                    .setDuration(260L)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        tv.translationY = 0f
                        tv.visibility = View.INVISIBLE
                    }
                    .start()
            }
            .start()
    }

    private fun shake(amplitudeDp: Float, cycles: Int, durationMs: Long) {
        val stage = binding.battleStage
        stage.animate().cancel()
        val amplitudePx = dp(amplitudeDp)
        val start = SystemClock.elapsedRealtime()

        val runnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                val elapsed = SystemClock.elapsedRealtime() - start
                val t = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                val damping = 1f - t
                val angle = t * cycles * 2f * Math.PI.toFloat()
                stage.translationX = (kotlin.math.sin(angle) * amplitudePx * damping)
                stage.translationY = (kotlin.math.sin(angle * 1.3f) * amplitudePx * 0.6f * damping)
                if (t < 1f) {
                    battleHandler.postDelayed(this, 16L)
                } else {
                    stage.translationX = 0f
                    stage.translationY = 0f
                }
            }
        }
        battleHandler.post(runnable)
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun formatTimeLeft(elapsedMs: Long): String {
        val totalSecondsLeft = ((60_000L - elapsedMs).coerceAtLeast(0L) / 1000L).toInt()
        val minute = totalSecondsLeft / 60
        val second = totalSecondsLeft % 60
        return String.format("%02d:%02d", minute, second)
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createDemoVolume(elapsedMs: Long): Int {
        val wave = ((sin(elapsedMs / 900.0) + 1f) * 32f).toInt()
        val phaseBoost = when {
            elapsedMs in 28_000L..42_000L -> 18
            elapsedMs in 42_000L..52_000L -> 28
            else -> 8
        }
        return (wave + phaseBoost).coerceIn(10, 96)
    }

    companion object {
        private const val FRAME_DELAY_MS = 180L

        fun newInstance(): BattleFragment = BattleFragment()
    }
}
