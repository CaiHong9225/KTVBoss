package com.thunder.ktvboss.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.thunder.ktvboss.databinding.FragmentResultBinding
import com.thunder.ktvboss.model.BattleResult

class ResultFragment : Fragment() {

    interface Listener {
        fun onPlayAgain()
        fun onBackHome()
    }

    private var listener: Listener? = null
    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val result = arguments?.getSerializable(ARG_RESULT) as? BattleResult ?: return

        binding.tvResultTitle.text = if (result.victory) "挑战成功" else "挑战结束"
        binding.tvResultSubtitle.text = result.playerTitle
        binding.tvSummary.text = result.summary
        binding.tvAiComment.text = result.aiComment
        binding.tvStats.text = "最大连击 ${result.maxCombo}  |  总伤害 ${result.totalDamage}  |  必杀 ${result.ultimateCount} 次"
        binding.tvBattleTime.text = "战斗时长 ${result.durationSeconds} 秒  |  命中 ${result.hitCount} 次"

        binding.btnPlayAgain.setOnClickListener { listener?.onPlayAgain() }
        binding.btnBackHome.setOnClickListener { listener?.onBackHome() }
        binding.btnPlayAgain.requestFocus()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_RESULT = "arg_result"

        fun newInstance(result: BattleResult): ResultFragment {
            return ResultFragment().apply {
                arguments = bundleOf(ARG_RESULT to result)
            }
        }
    }
}
