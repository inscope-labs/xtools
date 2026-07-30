package com.inscopelabs.abx.xtools.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.inscopelabs.abx.xtools.R

/**
 * Security & privacy settings: clear audit logs, reset permissions,
 * view privacy policy, clear app data.
 *
 * @see §7.6.1
 */
class SecurityPrivacyFragment : Fragment() {

    private lateinit var clearAuditLogsButton: Button
    private lateinit var resetPermissionsButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_security_privacy, container, false).apply {
            clearAuditLogsButton = findViewById(R.id.btn_clear_audit_logs)
            resetPermissionsButton = findViewById(R.id.btn_reset_permissions)

            clearAuditLogsButton.setOnClickListener {
                // TODO: Phase 6 – clear AuditLog store.
            }

            resetPermissionsButton.setOnClickListener {
                // TODO: Phase 4 – reset all permission grants.
            }
        }
    }
}
