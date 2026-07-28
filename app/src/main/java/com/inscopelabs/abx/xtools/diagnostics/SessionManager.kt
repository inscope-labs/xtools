package com.inscopelabs.abx.xtools.diagnostics

import java.util.UUID

object SessionManager {
    val sessionId: String = UUID.randomUUID().toString().take(8).uppercase()
}
