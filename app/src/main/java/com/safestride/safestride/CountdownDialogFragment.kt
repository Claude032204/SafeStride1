package com.safestride.safestride

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.safestride.safestride.R

class CountdownDialogFragment(private val countdownTimeout: Long, private val onTimeout: (Boolean) -> Unit) : DialogFragment() {

    private lateinit var countdownTextView: TextView
    private lateinit var cancelButton: Button
    private var countdownTimer: CountDownTimer? = null
    private var onDialogDismissedListener: (() -> Unit)? = null

    // Method to set the callback for dialog dismissal
    fun setOnDialogDismissedListener(listener: () -> Unit) {
        onDialogDismissedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_countdown, container, false)

        // Optional: Adjust the window size if needed
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        countdownTextView = view.findViewById(R.id.countdownTextView)
        cancelButton = view.findViewById(R.id.cancelButton)

        // Disable the dialog from being dismissed when the user clicks outside of it
        isCancelable = false  // Prevent the dialog from being dismissed by tapping outside

        // Cancel button dismisses the dialog and triggers the callback to reset the inactivity timer
        cancelButton.setOnClickListener {
            dismiss()
            onDialogDismissedListener?.invoke() // Reset the inactivity timer when the dialog is dismissed
        }

        startCountdown()

        return view
    }

    private fun startCountdown() {
        countdownTimer = object : CountDownTimer(countdownTimeout, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update the countdown text on the UI thread to avoid issues with background threads
                activity?.runOnUiThread {
                    countdownTextView.text = "Timeout in: ${millisUntilFinished / 1000} seconds"
                }
            }

            override fun onFinish() {
                onTimeout(true)  // Trigger timeout action (auto logout) when countdown finishes
                dismiss()  // Dismiss the dialog after finishing the countdown
            }
        }
        countdownTimer?.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()  // Ensure the countdown is canceled when the dialog is dismissed
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set dialog size (e.g., 80% of the screen width)
        val params = view.layoutParams
        params.width = (resources.displayMetrics.widthPixels * 0.8).toInt() // Set width to 80% of screen
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT // Adjust height as needed
        view.layoutParams = params
    }

    // Ensure dialog is only shown when the fragment's state is valid
    override fun onStart() {
        super.onStart()
        // Prevent fragment transactions after the state has been saved to avoid IllegalStateException
        if (isAdded && !requireActivity().isFinishing && !parentFragmentManager.isStateSaved) {
            // Safe to show the dialog
        }
    }
}
