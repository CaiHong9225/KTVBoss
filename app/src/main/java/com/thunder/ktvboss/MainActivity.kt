package com.thunder.ktvboss

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.thunder.ktvboss.databinding.ActivityMainBinding
import com.thunder.ktvboss.model.BattleResult
import com.thunder.ktvboss.ui.BattleFragment
import com.thunder.ktvboss.ui.HomeFragment
import com.thunder.ktvboss.ui.ResultFragment
import com.thunder.ktvboss.ui.SplashFragment

class MainActivity : AppCompatActivity(), HomeFragment.Listener, BattleFragment.Listener,
    ResultFragment.Listener, SplashFragment.Listener {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ensureAudioPermission()

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, SplashFragment.newInstance())
            }
        }
    }

    override fun onSplashFinished() {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, HomeFragment.newInstance())
        }
    }

    override fun onStartChallenge() {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, BattleFragment.newInstance())
            addToBackStack(null)
        }
    }

    override fun onOpenRules() {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, HomeFragment.newInstance(showRules = true))
            addToBackStack(null)
        }
    }

    override fun onBattleFinished(result: BattleResult) {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, ResultFragment.newInstance(result))
            addToBackStack(null)
        }
    }

    override fun onPlayAgain() {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, BattleFragment.newInstance())
        }
    }

    override fun onBackHome() {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, HomeFragment.newInstance())
        }
    }

    private fun ensureAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
