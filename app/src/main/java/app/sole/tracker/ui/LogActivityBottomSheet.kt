package app.sole.tracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import app.sole.tracker.R
import app.sole.tracker.ai.GeminiActivityEstimator
import app.sole.tracker.data.PrefsManager
import app.sole.tracker.data.StepDatabaseHelper
import app.sole.tracker.databinding.BottomSheetLogActivityBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.concurrent.thread

class LogActivityBottomSheet(
    private val onActivityAdded: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLogActivityBinding? = null
    private val binding get() = _binding!!

    private var selectedMinutes: Int = 30
    private var isOtherSelected: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLogActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chips = listOf(
            binding.chip15 to 15,
            binding.chip30 to 30,
            binding.chip45 to 45,
            binding.chip60 to 60
        )

        chips.forEach { (chip, mins) ->
            chip.setOnClickListener {
                isOtherSelected = false
                selectedMinutes = mins
                binding.etCustomMinutes.visibility = View.GONE
                updateChipStyles(chip)
            }
        }

        binding.chipOther.setOnClickListener {
            isOtherSelected = true
            binding.etCustomMinutes.visibility = View.VISIBLE
            binding.etCustomMinutes.requestFocus()
            updateChipStyles(binding.chipOther)
        }

        binding.btnSaveActivity.setOnClickListener {
            val activityName = binding.etActivityName.text.toString().trim()
            if (activityName.isEmpty()) {
                Toast.makeText(requireContext(), "Enter an activity first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalMinutes = if (isOtherSelected) {
                binding.etCustomMinutes.text.toString().toIntOrNull() ?: 0
            } else {
                selectedMinutes
            }

            if (finalMinutes <= 0) {
                Toast.makeText(requireContext(), "Enter a valid duration in minutes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSaveActivity.isEnabled = false
            binding.btnSaveActivity.text = "Estimating..."

            val prefs = PrefsManager(requireContext())
            thread {
                val result = GeminiActivityEstimator.estimate(activityName, finalMinutes, prefs)
                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.btnSaveActivity.isEnabled = true
                    binding.btnSaveActivity.text = "Save activity"

                    result.onSuccess { est ->
                        val fmt = NumberFormat.getNumberInstance(Locale.US)
                        val distText = if (prefs.isImperial) {
                            String.format(Locale.US, "%.1f mi", est.distanceKm * 0.621371)
                        } else {
                            String.format(Locale.US, "%.1f km", est.distanceKm)
                        }

                        val summaryMessage = buildString {
                            append("Based on \"")
                            append(activityName)
                            append("\" (")
                            append(finalMinutes)
                            append(" min), add these metrics to your day?\n\n")
                            append("• +${fmt.format(est.steps)} steps\n")
                            append("• +$distText\n")
                            append("• +${fmt.format(est.kcal)} Kcal\n")
                            append("• +${est.activeMinutes} min active")
                        }

                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Add to today?")
                            .setMessage(summaryMessage)
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton("Add to Today") { _, _ ->
                                val db = StepDatabaseHelper(requireContext())
                                db.addActivityMetrics(
                                    dateIso = LocalDate.now().toString(),
                                    currentGoal = prefs.dailyStepGoal,
                                    addSteps = est.steps,
                                    addDistKm = est.distanceKm,
                                    addKcal = est.kcal,
                                    addActiveMin = est.activeMinutes
                                )
                                onActivityAdded()
                                dismiss()
                            }
                            .show()
                    }.onFailure { err ->
                        Toast.makeText(
                            requireContext(),
                            err.message ?: "Could not estimate activity",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateChipStyles(activeChip: TextView) {
        val ctx = requireContext()
        val all = listOf(binding.chip15, binding.chip30, binding.chip45, binding.chip60, binding.chipOther)
        for (chip in all) {
            if (chip == activeChip) {
                chip.setBackgroundResource(R.drawable.bg_white_pill)
                chip.setTextColor(ContextCompat.getColor(ctx, R.color.oled_black))
            } else if (chip == binding.chipOther) {
                chip.setBackgroundResource(R.drawable.bg_chip_outline)
                chip.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
