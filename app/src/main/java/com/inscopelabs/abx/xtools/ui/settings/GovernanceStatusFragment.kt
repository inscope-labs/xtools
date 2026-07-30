package com.inscopelabs.abx.xtools.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.kernel.mode.OperatingMode
import com.inscopelabs.abx.xtools.kernel.RuntimeKernel

/**
 * Displays the current governance mode (STANDALONE / GOVERNED).
 * Shows session details when GOVERNED. Stub until Phase 4 wires live AIDL.
 *
 * @see §3.1.1 Step 2.1.4 (settings taxonomy)
 */
class GovernanceStatusFragment : Fragment() {

    private lateinit var modeStatusTextView: TextView
    private lateinit var sessionDetailsTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_governance_status, container, false).apply {
            modeStatusTextView = findViewById(R.id.tv_mode_status)
            sessionDetailsTextView = findViewById(R.id.tv_session_details)

            // Stub: retrieve actual mode from ModeArbiter (Phase 4).
            modeStatusTextView.text = "Current Mode: STANDALONE (stub)"
            sessionDetailsTextView.text = "No active abx-server session."
        }
    }
}
