package com.thunder.ktvboss.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
