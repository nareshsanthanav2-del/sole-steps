package app.sole.tracker.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import app.sole.tracker.R
import app.sole.tracker.data.PrefsManager
import app.sole.tracker.data.StepDatabaseHelper
import app.sole.tracker.databinding.BottomSheetSettingsBinding
import app.sole.tracker.sensor.StepForegroundService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDate

class SettingsBottomSheet(
    private val onSettingsSaved: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: PrefsManager
    private var selectedImperial: Boolean = false
    private var pendingAvatarUri: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            pendingAvatarUri = uri.toString()
            renderAvatarPreview()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext())

        pendingAvatarUri = prefs.avatarUri
        selectedImperial = prefs.isImperial

        binding.etSettingsName.setText(prefs.userName)
        binding.etSettingsStepGoal.setText(prefs.dailyStepGoal.toString())
        binding.switchBodyMetrics.isChecked = prefs.useBodyMetrics
        binding.layoutBodyInputs.visibility = if (prefs.useBodyMetrics) View.VISIBLE else View.GONE
        binding.etHeight.setText(prefs.heightCm.toString())
        binding.etWeight.setText(prefs.weightKg.toString())
        binding.switchBackgroundService.isChecked = prefs.backgroundServiceEnabled
        binding.switchAiEnabled.isChecked = prefs.aiEnabled
        binding.etGoogleApiKey.setText(prefs.googleApiKey)

        renderAvatarPreview()
        updateUnitPills()

        binding.btnChangeAvatar.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        binding.btnResetAvatar.setOnClickListener {
            pendingAvatarUri = null
            renderAvatarPreview()
        }

        binding.btnUnitMetric.setOnClickListener {
            selectedImperial = false
            updateUnitPills()
        }

        binding.btnUnitImperial.setOnClickListener {
            selectedImperial = true
            updateUnitPills()
        }

        binding.switchBodyMetrics.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutBodyInputs.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.btnSaveSettings.setOnClickListener {
            val name = binding.etSettingsName.text.toString().trim().ifEmpty { "Noah" }
            val newGoal = binding.etSettingsStepGoal.text.toString().toIntOrNull() ?: prefs.dailyStepGoal
            val useBody = binding.switchBodyMetrics.isChecked
            val h = binding.etHeight.text.toString().toFloatOrNull() ?: prefs.heightCm
            val w = binding.etWeight.text.toString().toFloatOrNull() ?: prefs.weightKg
            val bgService = binding.switchBackgroundService.isChecked
            val aiOn = binding.switchAiEnabled.isChecked
            val apiKey = binding.etGoogleApiKey.text.toString().trim()

            prefs.userName = name
            prefs.avatarUri = pendingAvatarUri
            prefs.dailyStepGoal = newGoal
            prefs.isImperial = selectedImperial
            prefs.useBodyMetrics = useBody
            prefs.heightCm = h
            prefs.weightKg = w
            prefs.backgroundServiceEnabled = bgService
            prefs.aiEnabled = aiOn
            prefs.googleApiKey = apiKey

            // Update only today's goal; past days keep their original goal status
            val db = StepDatabaseHelper(requireContext())
            db.updateTodayGoal(LocalDate.now().toString(), prefs.dailyStepGoal)

            // Start or stop optional background service
            StepForegroundService.syncServiceState(requireContext(), bgService)

            onSettingsSaved()
            dismiss()
        }

        // Dummy Buy Me a Coffee button — intentionally no-op for now
        binding.btnBuyMeACoffee.setOnClickListener { }
    }

    private fun renderAvatarPreview() {
        val uriStr = pendingAvatarUri
        if (!uriStr.isNullOrBlank()) {
            binding.ivSettingsAvatar.visibility = View.VISIBLE
            binding.tvSettingsInitial.visibility = View.GONE
            binding.ivSettingsAvatar.setImageURI(Uri.parse(uriStr))
        } else {
            binding.ivSettingsAvatar.visibility = View.GONE
            binding.tvSettingsInitial.visibility = View.VISIBLE
            val initial = binding.etSettingsName.text.toString().trim().firstOrNull()?.uppercase()
                ?: prefs.userName.firstOrNull()?.uppercase() ?: "N"
            binding.tvSettingsInitial.text = initial
        }
    }

    private fun updateUnitPills() {
        val ctx = requireContext()
        if (!selectedImperial) {
            binding.btnUnitMetric.setBackgroundResource(R.drawable.bg_nav_pill_active)
            binding.btnUnitMetric.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            binding.btnUnitImperial.background = null
            binding.btnUnitImperial.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
        } else {
            binding.btnUnitImperial.setBackgroundResource(R.drawable.bg_nav_pill_active)
            binding.btnUnitImperial.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            binding.btnUnitMetric.background = null
            binding.btnUnitMetric.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
