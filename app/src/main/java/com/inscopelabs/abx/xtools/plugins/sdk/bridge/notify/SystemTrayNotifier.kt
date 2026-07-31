package com.inscopelabs.abx.xtools.plugins.sdk.bridge.notify

import android.content.Context
import android.widget.Toast

class SystemTrayNotifier(private val context: Context) : Notifier {
    override fun show(title: String, body: String) {
        Toast.makeText(context, "$title: $body", Toast.LENGTH_SHORT).show()
    }
}
