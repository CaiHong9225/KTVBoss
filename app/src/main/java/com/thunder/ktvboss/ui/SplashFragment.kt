package com.thunder.ktvboss.ui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import com.thunder.ktvboss.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    interface Listener {
        fun onSplashFinished()
    }

    private var listener: Listener? = null
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val mainHandler = Handler(Looper.getMainLooper())
    private var finished = false
    private var scanAnimator: ValueAnimator? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener { finishNow() }

        binding.coreRing.apply {
            rotation = 0f
            animate()
                .rotationBy(360f)
                .setDuration(1400L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        binding.corePulse.apply {
            scaleX = 0.92f
            scaleY = 0.92f
            alpha = 0.65f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(900L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        binding.tvTitle.apply {
            alpha = 0f
            translationY = 14f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(520L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        binding.tvSubtitle.apply {
            alpha = 0f
            translationY = 18f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(150L)
                .setDuration(520L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        mainHandler.postDelayed({ finishNow() }, 1600L)
        startScanLine()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopScanLine()
        mainHandler.removeCallbacksAndMessages(null)
        _binding = null
    }

    private fun finishNow() {
        if (finished) return
        finished = true
        listener?.onSplashFinished()
    }

    private fun startScanLine() {
        val scan = binding.scanLine
        scan.post {
            val parentHeight = (scan.parent as? View)?.height ?: return@post
            scan.translationY = (-scan.height).toFloat()
            scanAnimator = ValueAnimator.ofFloat(
                (-scan.height).toFloat(),
                (parentHeight + scan.height).toFloat()
            ).apply {
                duration = 1200L
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    scan.translationY = animator.animatedValue as Float
                }
                start()
            }
        }
    }

    private fun stopScanLine() {
        scanAnimator?.cancel()
        scanAnimator = null
    }

    companion object {
        fun newInstance(): SplashFragment = SplashFragment()
    }
}

