package com.inscopelabs.abx.xtools.dispatcher

import android.content.Context

/**
 * Manual, process-lifetime wiring for the chat subsystem. The app has no DI
 * framework (no Hilt/Koin) anywhere else, so this follows the same manual
 * singleton pattern already used by ChatDatabase.getInstance() rather than
 * introducing a new pattern for one subsystem.
 *
 * ChatManager owns a CoroutineScope(SupervisorJob) internally and is meant
 * to outlive any single fragment/activity instance (so an in-flight stream
 * survives rotation), which is why this is keyed off applicationContext and
 * cached for the process, not per-Fragment.
 */
object ChatDependencies {

    @Volatile
    private var chatManagerInstance: ChatManager? = null

    @Volatile
    private var chatSecurityInstance: ChatSecurity? = null

    @Volatile
    private var chatRepositoryInstance: ChatRepository? = null

    @Volatile
    private var driverProfileRepositoryInstance: DriverProfileRepository? = null

    private val driverChatManagers = java.util.concurrent.ConcurrentHashMap<String, ChatManager>()

    fun chatManager(context: Context): ChatManager {
        return chatManagerInstance ?: synchronized(this) {
            chatManagerInstance ?: build(context.applicationContext).also { chatManagerInstance = it }
        }
    }

    fun chatSecurity(context: Context): ChatSecurity {
        return chatSecurityInstance ?: synchronized(this) {
            chatSecurityInstance ?: ChatSecurity(context.applicationContext).also { chatSecurityInstance = it }
        }
    }

    /**
     * Same underlying repository/database instance ChatManager uses
     * internally. Exposed so the ViewModel can do a one-shot "does a
     * session already exist" read against Room directly on startup,
     * instead of racing ChatManager's own StateFlow (which starts at
     * emptyList() and only reflects real data after its internal
     * collector coroutine has had a chance to run).
     */
    fun chatRepository(context: Context): ChatRepository {
        return chatRepositoryInstance ?: synchronized(this) {
            chatRepositoryInstance ?: ChatRepository(ChatDatabase.getInstance(context.applicationContext).chatDao())
                .also { chatRepositoryInstance = it }
        }
    }

    fun driverProfileRepository(context: Context): DriverProfileRepository {
        return driverProfileRepositoryInstance ?: synchronized(this) {
            driverProfileRepositoryInstance ?: DriverProfileRepository(
                ChatDatabase.getInstance(context.applicationContext).driverProfileDao()
            ).also { driverProfileRepositoryInstance = it }
        }
    }

    /**
     * Returns an isolated ChatManager for the given driver, or null if no
     * enabled profile exists — callers MUST treat null as an access denial,
     * not an error to work around. Each driver gets its own ChatSecurity
     * (separate encrypted storage) and its own ChatCache/ProviderFactory
     * instance — TokenCounter, PromptBuilder, ChatMemory, ChatLogger, and
     * the underlying session Repository/Room database stay shared, since
     * they hold no per-driver-sensitive state.
     */
    suspend fun chatManagerForDriver(context: Context, driverId: String): ChatManager? {
        driverChatManagers[driverId]?.let { return it }
        val profile = driverProfileRepository(context).getProfile(driverId) ?: return null
        if (!profile.enabled) return null
        return synchronized(this) {
            driverChatManagers[driverId] ?: buildIsolated(context.applicationContext, profile)
                .also { driverChatManagers[driverId] = it }
        }
    }

    private fun buildIsolated(appContext: Context, profile: DriverProfile): ChatManager {
        val repository = chatRepository(appContext)
        val tokenCounter = TokenCounter()
        val chatMemory = ChatMemory(tokenCounter)
        val promptBuilder = PromptBuilder(tokenCounter)
        val chatLogger = ChatLogger()
        val security = ChatSecurity(appContext, storeName = "abx_secure_chat_prefs_${profile.driverId}")
        val chatCache = ChatCache()
        val providerFactory = ProviderFactory()

        return ChatManager(
            repository = repository,
            providerFactory = providerFactory,
            promptBuilder = promptBuilder,
            tokenCounter = tokenCounter,
            chatMemory = chatMemory,
            chatLogger = chatLogger,
            chatSecurity = security,
            chatCache = chatCache
        )
    }

    private fun build(appContext: Context): ChatManager {
        val repository = chatRepository(appContext)
        val tokenCounter = TokenCounter()
        val chatMemory = ChatMemory(tokenCounter)
        val promptBuilder = PromptBuilder(tokenCounter)
        val chatLogger = ChatLogger()
        val security = chatSecurity(appContext)
        val chatCache = ChatCache()
        val providerFactory = ProviderFactory()

        return ChatManager(
            repository = repository,
            providerFactory = providerFactory,
            promptBuilder = promptBuilder,
            tokenCounter = tokenCounter,
            chatMemory = chatMemory,
            chatLogger = chatLogger,
            chatSecurity = security,
            chatCache = chatCache
        )
    }
}
