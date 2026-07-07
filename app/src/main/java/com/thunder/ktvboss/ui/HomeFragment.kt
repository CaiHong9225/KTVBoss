package com.thunder.ktvboss.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.thunder.ktvboss.boss.BossSession
import com.thunder.ktvboss.databinding.FragmentHomeBinding
import com.thunder.ktvboss.net.RemoteImageLoader

class HomeFragment : Fragment() {

    interface Listener {
        fun onStartChallenge()
        fun onOpenRules()
    }

    private var listener: Listener? = null
    private var _binding: FragmentHomeBinding? = null
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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val showRules = arguments?.getBoolean(ARG_SHOW_RULES) ?: false
        binding.rulesContent.visibility = if (showRules) View.VISIBLE else View.GONE

        val boss = BossSession.current()
        binding.tvBossTitle.text = "Boss：${boss.name}"
        RemoteImageLoader.load(boss.imageUrl, binding.ivBossAvatar)

        binding.btnStart.setOnClickListener { listener?.onStartChallenge() }
        binding.btnRules.setOnClickListener { listener?.onOpenRules() }
        binding.btnStart.requestFocus()
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
        private const val ARG_SHOW_RULES = "show_rules"

        fun newInstance(showRules: Boolean = false): HomeFragment {
            return HomeFragment().apply {
                arguments = bundleOf(ARG_SHOW_RULES to showRules)
            }
        }
    }
}
