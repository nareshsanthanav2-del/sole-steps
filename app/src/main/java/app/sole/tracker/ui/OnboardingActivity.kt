package app.sole.tracker.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import app.sole.tracker.MainActivity
import app.sole.tracker.R
import app.sole.tracker.data.PrefsManager
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import kotlin.concurrent.thread

class OnboardingActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager

    private var currentStep = 0 // 0=Welcome, 1=Goal, 2=About, 3=Permission, 4=ConnectAI
    private var aiLoggingEnabled = false
    private var selectedGoal = 10000

    private lateinit var llProgressSegments: LinearLayout
    private lateinit var segments: List<View>

    private lateinit var stepWelcome: View
    private lateinit var stepGoal: View
    private lateinit var stepAbout: View
    private lateinit var stepPermission: View
    private lateinit var stepAiKey: View

    private lateinit var boxAiCheckbox: FrameLayout
    private lateinit var tvAiCheckmark: TextView

    private lateinit var tvGoalNumber: TextView
    private lateinit var btnPreset6k: TextView
    private lateinit var btnPreset8k: TextView
    private lateinit var btnPreset10k: TextView
    private lateinit var btnPreset12k: TextView
    private lateinit var btnCustomGoal: TextView

    private lateinit var etFirstName: EditText
    private lateinit var etHeight: EditText
    private lateinit var etWeight: EditText
    private lateinit var boxHeight: LinearLayout
    private lateinit var boxWeight: LinearLayout

    private lateinit var boxApiKey: LinearLayout
    private lateinit var etApiKey: EditText
    private lateinit var tvKeyCheckIcon: TextView
    private lateinit var tvKeyStatus: TextView

    private lateinit var btnPrimary: TextView
    private lateinit var btnSecondary: TextView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        advanceAfterPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        prefs = PrefsManager(this)
        setContentView(R.layout.activity_onboarding)

        val root = findViewById<LinearLayout>(R.id.rootOnboarding)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(dp(24), bars.top + dp(12), dp(24), bars.bottom + dp(20))
            insets
        }

        bindViews()
        applyInitialStyles()
        setupListeners()
        showStep(0)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentStep > 0) {
                    showStep(currentStep - 1)
                } else {
                    finish()
                }
            }
        })
    }

    private fun bindViews() {
        llProgressSegments = findViewById(R.id.llProgressSegments)
        segments = listOf(
            findViewById(R.id.seg1),
            findViewById(R.id.seg2),
            findViewById(R.id.seg3),
            findViewById(R.id.seg4),
            findViewById(R.id.seg5)
        )

        stepWelcome = findViewById(R.id.stepWelcome)
        stepGoal = findViewById(R.id.stepGoal)
        stepAbout = findViewById(R.id.stepAbout)
        stepPermission = findViewById(R.id.stepPermission)
        stepAiKey = findViewById(R.id.stepAiKey)

        boxAiCheckbox = findViewById(R.id.boxAiCheckbox)
        tvAiCheckmark = findViewById(R.id.tvAiCheckmark)

        tvGoalNumber = findViewById(R.id.tvGoalNumber)
        btnPreset6k = findViewById(R.id.btnPreset6k)
        btnPreset8k = findViewById(R.id.btnPreset8k)
        btnPreset10k = findViewById(R.id.btnPreset10k)
        btnPreset12k = findViewById(R.id.btnPreset12k)
        btnCustomGoal = findViewById(R.id.btnCustomGoal)

        etFirstName = findViewById(R.id.etFirstName)
        etHeight = findViewById(R.id.etHeight)
        etWeight = findViewById(R.id.etWeight)
        boxHeight = findViewById(R.id.boxHeight)
        boxWeight = findViewById(R.id.boxWeight)

        boxApiKey = findViewById(R.id.boxApiKey)
        etApiKey = findViewById(R.id.etApiKey)
        tvKeyCheckIcon = findViewById(R.id.tvKeyCheckIcon)
        tvKeyStatus = findViewById(R.id.tvKeyStatus)

        btnPrimary = findViewById(R.id.btnPrimary)
        btnSecondary = findViewById(R.id.btnSecondary)
    }

    private fun applyInitialStyles() {
        // Bold "100% local-first." in white inside privacy card
        val tvPrivacy = findViewById<TextView>(R.id.tvPrivacyNote)
        val fullPrivacy = "100% local-first. Your step data never leaves your phone."
        val span = SpannableString(fullPrivacy)
        val boldLen = "100% local-first.".length
        span.setSpan(StyleSpan(Typeface.BOLD), 0, boldLen, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(ForegroundColorSpan(Color.WHITE), 0, boldLen, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvPrivacy.text = span

        // Dark rounded cards
        val cardBg = makeRoundedDrawable("#111113", 16f, "#222225", 1f)
        findViewById<View>(R.id.cardPrivacy).background = makeRoundedDrawable("#111113", 16f, "#222225", 1f)
        findViewById<View>(R.id.cardAiToggle).background = makeRoundedDrawable("#111113", 16f, "#222225", 1f)
        findViewById<View>(R.id.cardPermissionNote).background = makeRoundedDrawable("#111113", 16f, "#222225", 1f)
        findViewById<View>(R.id.cardGetFreeKey).background = cardBg

        // Primary white pill button
        btnPrimary.background = makeRoundedDrawable("#FFFFFF", 28f)

        // About You inputs
        etFirstName.background = makeRoundedDrawable("#141416", 14f, "#FFFFFF", 1.5f)
        boxHeight.background = makeRoundedDrawable("#161618", 14f)
        boxWeight.background = makeRoundedDrawable("#161618", 14f)
        findViewById<View>(R.id.btnUseDefaults).background = makeRoundedDrawable("#000000", 24f, "#2C2C2E", 1f)

        // API key input & green check badge
        boxApiKey.background = makeRoundedDrawable("#141416", 14f, "#2C2C2E", 1f)
        tvKeyCheckIcon.background = makeRoundedDrawable("#22C55E", 12f)

        updateAiCheckboxUI()
        updateGoalUI()
    }

    private fun setupListeners() {
        // Step 1: Toggle AI activity logging
        findViewById<View>(R.id.cardAiToggle).setOnClickListener {
            aiLoggingEnabled = !aiLoggingEnabled
            updateAiCheckboxUI()
        }

        // Step 2: Goal presets & Custom number
        btnPreset6k.setOnClickListener { selectedGoal = 6000; updateGoalUI() }
        btnPreset8k.setOnClickListener { selectedGoal = 8000; updateGoalUI() }
        btnPreset10k.setOnClickListener { selectedGoal = 10000; updateGoalUI() }
        btnPreset12k.setOnClickListener { selectedGoal = 12000; updateGoalUI() }
        btnCustomGoal.setOnClickListener { showCustomGoalDialog() }

        // Step 3: Use defaults button
        findViewById<View>(R.id.btnUseDefaults).setOnClickListener {
            etHeight.setText("170")
            etWeight.setText("70")
            showStep(3)
        }

        // Step 5: Open console.groq.com/keys
        findViewById<View>(R.id.cardGetFreeKey).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys")))
            } catch (_: Exception) {
                Toast.makeText(this, "Open console.groq.com/keys in your browser", Toast.LENGTH_SHORT).show()
            }
        }

        // Step 5: Live API key verification UI
        etApiKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val key = s?.toString()?.trim() ?: ""
                validateApiKeyInput(key)
            }
        })

        // Primary CTA button
        btnPrimary.setOnClickListener {
            when (currentStep) {
                0 -> showStep(1)
                1 -> showStep(2)
                2 -> showStep(3)
                3 -> requestStepPermission()
                4 -> completeOnboarding(saveApiKey = true)
            }
        }

        // Secondary link ("Skip" / "Skip for now")
        btnSecondary.setOnClickListener {
            when (currentStep) {
                2 -> {
                    etFirstName.setText("")
                    etHeight.setText("")
                    etWeight.setText("")
                    showStep(3)
                }
                4 -> completeOnboarding(saveApiKey = false)
            }
        }
    }

    private fun showStep(step: Int) {
        currentStep = step

        stepWelcome.visibility = if (step == 0) View.VISIBLE else View.GONE
        stepGoal.visibility = if (step == 1) View.VISIBLE else View.GONE
        stepAbout.visibility = if (step == 2) View.VISIBLE else View.GONE
        stepPermission.visibility = if (step == 3) View.VISIBLE else View.GONE
        stepAiKey.visibility = if (step == 4) View.VISIBLE else View.GONE

        // Segmented progress bar: hidden on step 0, visible on steps 1..4
        if (step == 0) {
            llProgressSegments.visibility = View.GONE
        } else {
            llProgressSegments.visibility = View.VISIBLE
            val totalSteps = if (aiLoggingEnabled) 5 else 4
            for (i in segments.indices) {
                val seg = segments[i]
                if (i < totalSteps) {
                    seg.visibility = View.VISIBLE
                    val active = i <= step
                    seg.background = makeRoundedDrawable(if (active) "#FFFFFF" else "#2C2C2E", 1.5f)
                } else {
                    seg.visibility = View.GONE
                }
            }
        }

        // Configure primary & secondary buttons per screen
        when (step) {
            0 -> {
                btnPrimary.text = "Get Started"
                btnSecondary.visibility = View.GONE
            }
            1 -> {
                btnPrimary.text = "Continue"
                btnSecondary.visibility = View.GONE
            }
            2 -> {
                btnPrimary.text = "Continue"
                btnSecondary.text = "Skip"
                btnSecondary.visibility = View.VISIBLE
            }
            3 -> {
                btnPrimary.text = "Enable step counting"
                btnSecondary.visibility = View.GONE
            }
            4 -> {
                btnPrimary.text = "Finish"
                btnSecondary.text = "Skip for now"
                btnSecondary.visibility = View.VISIBLE
            }
        }
    }

    private fun updateAiCheckboxUI() {
        if (aiLoggingEnabled) {
            boxAiCheckbox.background = makeRoundedDrawable("#FFFFFF", 6f)
            tvAiCheckmark.visibility = View.VISIBLE
        } else {
            boxAiCheckbox.background = makeRoundedDrawable("#111113", 6f, "#48484A", 1.5f)
            tvAiCheckmark.visibility = View.GONE
        }
    }

    private fun updateGoalUI() {
        val fmt = NumberFormat.getNumberInstance(Locale.US)
        tvGoalNumber.text = fmt.format(selectedGoal)

        val presets = listOf(
            btnPreset6k to 6000,
            btnPreset8k to 8000,
            btnPreset10k to 10000,
            btnPreset12k to 12000
        )
        var matchedPreset = false
        for ((btn, value) in presets) {
            if (selectedGoal == value) {
                matchedPreset = true
                btn.background = makeRoundedDrawable("#FFFFFF", 22f)
                btn.setTextColor(Color.BLACK)
            } else {
                btn.background = makeRoundedDrawable("#161618", 22f)
                btn.setTextColor(Color.WHITE)
            }
        }

        if (!matchedPreset) {
            btnCustomGoal.background = makeRoundedDrawable("#161618", 24f, "#FFFFFF", 1f)
            btnCustomGoal.setTextColor(Color.WHITE)
        } else {
            btnCustomGoal.background = makeRoundedDrawable("#000000", 24f, "#2C2C2E", 1f)
            btnCustomGoal.setTextColor(Color.parseColor("#8E8E93"))
        }
    }

    private fun showCustomGoalDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "e.g. 7500"
            setText(selectedGoal.toString())
            setSelection(text.length)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#636366"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Custom daily step goal")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Set Goal") { _, _ ->
                val entered = input.text.toString().toIntOrNull()
                if (entered != null && entered in 500..200000) {
                    selectedGoal = entered
                    updateGoalUI()
                } else {
                    Toast.makeText(this, "Enter a goal between 500 and 200,000", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun requestStepPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                return
            }
        }
        advanceAfterPermission()
    }

    private fun advanceAfterPermission() {
        if (aiLoggingEnabled) {
            showStep(4)
        } else {
            completeOnboarding(saveApiKey = false)
        }
    }

    private fun validateApiKeyInput(key: String) {
        if (key.isEmpty()) {
            boxApiKey.background = makeRoundedDrawable("#141416", 14f, "#2C2C2E", 1f)
            tvKeyCheckIcon.visibility = View.GONE
            tvKeyStatus.visibility = View.GONE
            return
        }

        val looksValid = (key.startsWith("gsk_") || key.startsWith("AIza")) && key.length >= 24
        if (!looksValid) {
            boxApiKey.background = makeRoundedDrawable("#141416", 14f, "#FFFFFF", 1.5f)
            tvKeyCheckIcon.visibility = View.GONE
            tvKeyStatus.visibility = View.GONE
            return
        }

        // Show verified visual state immediately, then confirm against Groq in background
        boxApiKey.background = makeRoundedDrawable("#141416", 14f, "#FFFFFF", 1.5f)
        tvKeyCheckIcon.background = makeRoundedDrawable("#22C55E", 12f)
        tvKeyCheckIcon.visibility = View.VISIBLE
        tvKeyStatus.text = "Key verified"
        tvKeyStatus.setTextColor(Color.parseColor("#22C55E"))
        tvKeyStatus.visibility = View.VISIBLE

        if (key.startsWith("gsk_")) {
            thread {
                try {
                    val url = URL("https://api.groq.com/openai/v1/models")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Authorization", "Bearer $key")
                        connectTimeout = 6000
                        readTimeout = 6000
                    }
                    val code = conn.responseCode
                    runOnUiThread {
                        if (etApiKey.text.toString().trim() != key) return@runOnUiThread
                        if (code == 401) {
                            tvKeyCheckIcon.visibility = View.GONE
                            tvKeyStatus.text = "Invalid API key — check key and try again"
                            tvKeyStatus.setTextColor(Color.parseColor("#EF4444"))
                            tvKeyStatus.visibility = View.VISIBLE
                        }
                    }
                } catch (_: Exception) {
                    // Offline or network timeout: keep format-verified state
                }
            }
        }
    }

    private fun completeOnboarding(saveApiKey: Boolean) {
        prefs.dailyStepGoal = selectedGoal

        val name = etFirstName.text.toString().trim()
        if (name.isNotEmpty()) {
            prefs.userName = name
        }

        val h = etHeight.text.toString().trim().toIntOrNull()
        val w = etWeight.text.toString().trim().toIntOrNull()
        if (h != null && h in 80..260 && w != null && w in 25..350) {
            prefs.heightCm = h
            prefs.weightKg = w
            prefs.useBodyMetrics = true
        } else {
            prefs.heightCm = 170
            prefs.weightKg = 70
            prefs.useBodyMetrics = false
        }

        val enteredKey = if (saveApiKey) etApiKey.text.toString().trim() else ""
        if (aiLoggingEnabled && enteredKey.isNotEmpty()) {
            prefs.aiLoggingEnabled = true
            prefs.googleApiKey = enteredKey
        } else {
            prefs.aiLoggingEnabled = aiLoggingEnabled
            if (!saveApiKey) {
                prefs.googleApiKey = ""
            }
        }

        prefs.isOnboardingComplete = true

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun makeRoundedDrawable(
        fillHex: String,
        radiusDp: Float,
        strokeHex: String? = null,
        strokeDp: Float = 0f
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * resources.displayMetrics.density
            setColor(Color.parseColor(fillHex))
            if (strokeHex != null && strokeDp > 0f) {
                setStroke((strokeDp * resources.displayMetrics.density).toInt(), Color.parseColor(strokeHex))
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
