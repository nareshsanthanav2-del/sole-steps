package app.sole.tracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import app.sole.tracker.data.PrefsManager
import app.sole.tracker.databinding.ActivityOnboardingBinding

/**
 * Simple, modular first-launch onboarding screen.
 * Easily replaceable with full custom multi-screen onboarding later.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: PrefsManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        finishOnboarding()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGetStarted.setOnClickListener {
            val enteredName = binding.etOnboardName.text.toString().trim()
            if (enteredName.isNotEmpty()) {
                prefs.userName = enteredName
            }
            val goal = binding.etOnboardGoal.text.toString().toIntOrNull() ?: 10_000
            prefs.dailyStepGoal = goal

            val perms = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (perms.isNotEmpty()) {
                permissionLauncher.launch(perms.toTypedArray())
            } else {
                finishOnboarding()
            }
        }
    }

    private fun finishOnboarding() {
        prefs.isOnboarded = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
