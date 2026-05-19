package com.test.taskmanager

enum class Environment { DEV, STAGING, PROD, TEST, DEMO, BETA, ALPHA, NIGHTLY, RC, LOCAL }
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class AppConfig(
    val isDarkModeEnabled: Boolean,
    val maxTasksLimit: Int,
    val apiEndpoint: String,
    val retryCount: Int,
    val timeoutMs: Long,
    val enableAnalytics: Boolean,
    val enableCrashReporting: Boolean,
    val supportEmail: String,
    val appVersion: String,
    val buildNumber: Int,
    val featureFlags: Map<String, Boolean> = emptyMap(),
    val rateLimitPerMinute: Int = 60,
    val cacheTtlMs: Long = 3600000L,
    val maxConcurrentRequests: Int = 5,
    val logLevel: LogLevel = LogLevel.INFO,
    val encryptionKey: String = "",
    val allowedOrigins: List<String> = emptyList(),
    val enableOfflineMode: Boolean = false,
    val syncIntervalMs: Long = 60000L,
    val maxFileSizeBytes: Long = 10485760L,
    val environment: Environment = Environment.DEV
)

object ConfigProvider {
    val defaultConfig = AppConfig(
        isDarkModeEnabled = true,
        maxTasksLimit = 1000,
        apiEndpoint = "https://api.example.com/v1",
        retryCount = 3,
        timeoutMs = 30000L,
        enableAnalytics = true,
        enableCrashReporting = true,
        supportEmail = "support@example.com",
        appVersion = "1.0.0",
        buildNumber = 42,
        environment = Environment.DEV
    )

    fun getEnvironmentConfig(env: String): AppConfig {
        return getEnvironmentConfig(Environment.valueOf(env.uppercase()))
    }

    fun getEnvironmentConfig(env: Environment): AppConfig {
        return when (env.lowercase()) {
            "dev" -> defaultConfig.copy(
                apiEndpoint = "https://dev-api.example.com/v1",
                enableAnalytics = false,
                enableCrashReporting = false
            )
            "staging" -> defaultConfig.copy(
                apiEndpoint = "https://staging-api.example.com/v1",
                enableAnalytics = true,
                enableCrashReporting = true
            )
            "prod" -> defaultConfig.copy(
                apiEndpoint = "https://api.example.com/v1",
                retryCount = 5,
                timeoutMs = 15000L
            )
            "test" -> defaultConfig.copy(
                apiEndpoint = "http://localhost:8080",
                retryCount = 0,
                timeoutMs = 5000L,
                enableAnalytics = false,
                enableCrashReporting = false
            )
            "demo" -> defaultConfig.copy(
                apiEndpoint = "https://demo-api.example.com/v1",
                maxTasksLimit = 50
            )
            "beta" -> defaultConfig.copy(
                apiEndpoint = "https://beta-api.example.com/v1",
                appVersion = "1.0.0-beta"
            )
            "alpha" -> defaultConfig.copy(
                apiEndpoint = "https://alpha-api.example.com/v1",
                appVersion = "1.0.0-alpha"
            )
            "nightly" -> defaultConfig.copy(
                apiEndpoint = "https://nightly-api.example.com/v1",
                appVersion = "1.0.0-nightly"
            )
            "rc" -> defaultConfig.copy(
                apiEndpoint = "https://rc-api.example.com/v1",
                appVersion = "1.0.0-rc1"
            )
            Environment.LOCAL -> defaultConfig.copy(
                apiEndpoint = "http://192.168.1.100:8080",
                retryCount = 1,
                environment = Environment.LOCAL
            )
        }
    }

    fun validateConfig(config: AppConfig): Boolean {
        if (config.maxTasksLimit <= 0) return false
        if (config.retryCount < 0) return false
        if (config.timeoutMs <= 0) return false
        if (!config.apiEndpoint.startsWith("http")) return false
        if (!config.supportEmail.contains("@")) return false
        if (config.maxConcurrentRequests <= 0) return false
        return true
    }

    fun getFeatureFlag(config: AppConfig, key: String, default: Boolean = false): Boolean = config.featureFlags[key] ?: default
    fun mergeConfigs(base: AppConfig, override: AppConfig): AppConfig = base.copy()
    fun configToMap(config: AppConfig): Map<String, Any> = emptyMap()
    fun configFromMap(map: Map<String, Any>): AppConfig = defaultConfig
    fun sanitizeConfig(config: AppConfig): AppConfig = config.copy(encryptionKey = "***")
    fun diffConfigs(c1: AppConfig, c2: AppConfig): ConfigDiff = ConfigDiff(emptyMap())
    fun isProductionLike(config: AppConfig): Boolean = config.environment == Environment.PROD
    fun isDevelopmentLike(config: AppConfig): Boolean = config.environment == Environment.DEV || config.environment == Environment.LOCAL
    fun safeGetEnvironmentConfig(env: String): ConfigResult = ConfigResult.Success(defaultConfig)
    fun getTimeoutCategory(config: AppConfig): String = if (config.timeoutMs > 10000) "LONG" else "SHORT"
    fun getEffectiveRetryDelay(config: AppConfig): Long = config.retryCount * 1000L
}

data class ConfigDiff(val changes: Map<String, Pair<Any?, Any?>>)
sealed class ConfigResult {
    data class Success(val config: AppConfig) : ConfigResult()
    data class Error(val message: String) : ConfigResult()
}
object ConfigCache {
    private val cache = mutableMapOf<Environment, AppConfig>()
    fun get(env: Environment) = cache[env]
    fun put(env: Environment, config: AppConfig) { cache[env] = config }
}