package com.inscopelabs.abx.xtools.dispatcher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.inscopelabs.abx.server.contractdispatcher.DispatcherContractConstants
import com.inscopelabs.abx.server.contractdispatcher.DispatcherRequest
import com.inscopelabs.abx.server.contractdispatcher.DispatcherResponse
import com.inscopelabs.abx.server.contractdispatcher.IDispatcherExecutor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class DispatcherExecutorService : Service() {

    companion object {
        const val DISPATCHER_NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "dispatcher_notifications_channel"
    }

    private val chatLogger = ChatLogger()

    private val binderStub = object : IDispatcherExecutor.Stub() {
        override fun execute(request: DispatcherRequest): DispatcherResponse {
            // 1. Protocol version check FIRST, before any driver lookup
            if (request.protocolVersion != DispatcherContractConstants.PROTOCOL_VERSION) {
                return DispatcherResponse(
                    false,
                    null,
                    DispatcherContractConstants.ERROR_CODE_PROTOCOL_VERSION_MISMATCH,
                    "Protocol version mismatch",
                    DispatcherContractConstants.PROTOCOL_VERSION
                )
            }

            return runBlocking(Dispatchers.IO) {
                // Show low-priority notification
                showProcessingNotification(request.originComponent)

                // 2. Resolve driver profile
                val profileRepo = ChatDependencies.driverProfileRepository(applicationContext)
                val profile = profileRepo.getProfile(request.originComponent)

                if (profile == null || !profile.enabled) {
                    chatLogger.logCancellation("Access denied: no enabled profile for driver ${request.originComponent}")
                    return@runBlocking DispatcherResponse(
                        false,
                        null,
                        null,
                        "Access denied: no enabled profile for this driver",
                        DispatcherContractConstants.PROTOCOL_VERSION
                    )
                }

                // 3. Resolve isolated ChatManager
                val chatManager = ChatDependencies.chatManagerForDriver(applicationContext, request.originComponent)
                if (chatManager == null) {
                    chatLogger.logCancellation("Access denied: isolated ChatManager resolution failed for ${request.originComponent}")
                    return@runBlocking DispatcherResponse(
                        false,
                        null,
                        null,
                        "Access denied: no enabled profile for this driver",
                        DispatcherContractConstants.PROTOCOL_VERSION
                    )
                }

                // 4. Create a fresh session
                val session = chatManager.createSession(
                    title = "governed:${request.originComponent}",
                    provider = profile.settings.provider,
                    model = profile.settings.model,
                    settingsOverride = profile.settings
                )

                try {
                    // 5. Send prompt
                    chatManager.send(sessionId = session.id, prompt = request.prompt)

                    val accumulatedText = java.lang.StringBuilder()
                    var finalResponse: DispatcherResponse? = null

                    // 6. Collect events until terminal state or timeout
                    try {
                        withTimeout(profile.settings.timeoutMillis) {
                            chatManager.events.collect { event ->
                                val eventSessionId = when (event) {
                                    is ChatEvent.StreamingChunk -> event.sessionId
                                    is ChatEvent.StateChanged -> event.sessionId
                                    is ChatEvent.ErrorOccurred -> event.sessionId
                                    is ChatEvent.MessageAdded -> event.sessionId
                                    is ChatEvent.SessionDeleted -> event.sessionId
                                    is ChatEvent.SessionUpdated -> event.session.id
                                    is ChatEvent.SessionCreated -> event.session.id
                                    is ChatEvent.ConversationCleared -> event.sessionId
                                }

                                if (eventSessionId != session.id) return@collect

                                when (event) {
                                    is ChatEvent.StreamingChunk -> {
                                        accumulatedText.append(event.chunk)
                                    }
                                    is ChatEvent.StateChanged -> {
                                        when (event.state) {
                                            StreamingState.DONE -> {
                                                chatLogger.logResponse(profile.settings.provider, 0L, accumulatedText.length)
                                                finalResponse = DispatcherResponse(
                                                    true,
                                                    accumulatedText.toString(),
                                                    null,
                                                    null,
                                                    DispatcherContractConstants.PROTOCOL_VERSION
                                                )
                                                cancel()
                                            }
                                            StreamingState.ERROR -> {
                                                chatLogger.logError("Governed execution state ERROR for ${request.originComponent}", Exception("Execution state changed to ERROR"))
                                                finalResponse = DispatcherResponse(
                                                    false,
                                                    null,
                                                    null,
                                                    "Provider error: Execution state changed to ERROR",
                                                    DispatcherContractConstants.PROTOCOL_VERSION
                                                )
                                                cancel()
                                            }
                                            StreamingState.CANCELLED -> {
                                                chatLogger.logCancellation(session.id)
                                                finalResponse = DispatcherResponse(
                                                    false,
                                                    null,
                                                    null,
                                                    "Request cancelled",
                                                    DispatcherContractConstants.PROTOCOL_VERSION
                                                )
                                                cancel()
                                            }
                                            else -> { /* Streaming / Connecting states */ }
                                        }
                                    }
                                    is ChatEvent.ErrorOccurred -> {
                                        val msg = event.throwable.message ?: "Unknown provider error"
                                        chatLogger.logProviderError(profile.settings.provider, event.throwable)
                                        finalResponse = DispatcherResponse(
                                            false,
                                            null,
                                            null,
                                            "Provider error: $msg",
                                            DispatcherContractConstants.PROTOCOL_VERSION
                                        )
                                        cancel()
                                    }
                                    else -> {}
                                }
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        chatManager.cancel(session.id)
                        chatLogger.logCancellation("Timeout for session ${session.id}")
                        return@runBlocking DispatcherResponse(
                            false,
                            null,
                            null,
                            "Dispatcher execution timed out after ${profile.settings.timeoutMillis}ms",
                            DispatcherContractConstants.PROTOCOL_VERSION
                        )
                    } catch (e: CancellationException) {
                        if (finalResponse != null) {
                            return@runBlocking finalResponse!!
                        }
                    }

                    return@runBlocking finalResponse ?: DispatcherResponse(
                        false,
                        null,
                        null,
                        "Request ended without final response",
                        DispatcherContractConstants.PROTOCOL_VERSION
                    )
                } finally {
                    // 7. DELETES session afterward (success or failure)
                    chatManager.deleteSession(session.id)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == DispatcherContractConstants.SERVICE_ACTION) {
            return binderStub
        }
        return null
    }

    private fun showProcessingNotification(originComponent: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dispatcher Notifications",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("XTools Dispatcher")
            .setContentText("Processing request from $originComponent")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .build()

        notificationManager.notify(DISPATCHER_NOTIFICATION_ID, notification)
    }
}
