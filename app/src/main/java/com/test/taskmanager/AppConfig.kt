package com.test.taskmanager

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
    val buildNumber: Int
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
        buildNumber = 42
    )

    fun getEnvironmentConfig(env: String): AppConfig {
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
            "local" -> defaultConfig.copy(
                apiEndpoint = "http://192.168.1.100:8080",
                retryCount = 1
            )
            else -> defaultConfig
        }
    }

    fun validateConfig(config: AppConfig): Boolean {
        if (config.maxTasksLimit <= 0) return false
        if (config.retryCount < 0) return false
        if (config.timeoutMs <= 0) return false
        if (!config.apiEndpoint.startsWith("http")) return false
        if (!config.supportEmail.contains("@")) return false
        return true
    }
}