package com.test2.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис уведомлений. Хранит in-memory очередь, поддерживает приоритеты,
 * фильтрацию по типу и broadcast через SharedFlow.
 */
@Singleton
class NotificationService @Inject constructor() {

    data class Notification(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val body: String,
        val type: NotificationType,
        val priority: Priority = Priority.NORMAL,
        val timestamp: Long = System.currentTimeMillis(),
        val userId: String? = null,
        val read: Boolean = false,
        val metadata: Map<String, String> = emptyMap()
    )

    enum class NotificationType {
        INFO, WARNING, ERROR, SUCCESS, REMINDER, MARKETING, SYSTEM
    }

    enum class Priority(val weight: Int) {
        LOW(1), NORMAL(5), HIGH(8), URGENT(10);
        companion object {
            fun fromWeight(w: Int): Priority = entries.minByOrNull { kotlin.math.abs(it.weight - w) } ?: NORMAL
        }
    }

    private val mutex = Mutex()
    private val notifications = mutableListOf<Notification>()

    private val _stream = MutableSharedFlow<Notification>(replay = 0, extraBufferCapacity = 100)
    val stream: Flow<Notification> = _stream.asSharedFlow()

    suspend fun send(
        title: String,
        body: String,
        type: NotificationType = NotificationType.INFO,
        priority: Priority = Priority.NORMAL,
        userId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Notification {
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(body.isNotBlank()) { "Body cannot be blank" }

        val notification = Notification(
            title = title.trim(),
            body = body.trim(),
            type = type,
            priority = priority,
            userId = userId,
            metadata = metadata
        )

        mutex.withLock {
            notifications.add(notification)
            if (notifications.size > MAX_STORAGE) {
                val toDrop = notifications.size - MAX_STORAGE
                repeat(toDrop) { notifications.removeAt(0) }
            }
        }

        _stream.tryEmit(notification)
        return notification
    }

    suspend fun sendInfo(title: String, body: String, userId: String? = null): Notification =
        send(title, body, NotificationType.INFO, Priority.NORMAL, userId)

    suspend fun sendWarning(title: String, body: String, userId: String? = null): Notification =
        send(title, body, NotificationType.WARNING, Priority.HIGH, userId)

    suspend fun sendError(title: String, body: String, userId: String? = null): Notification =
        send(title, body, NotificationType.ERROR, Priority.URGENT, userId)

    suspend fun sendSuccess(title: String, body: String, userId: String? = null): Notification =
        send(title, body, NotificationType.SUCCESS, Priority.NORMAL, userId)

    suspend fun markAsRead(id: String): Boolean = mutex.withLock {
        val index = notifications.indexOfFirst { it.id == id }
        if (index < 0) return false
        val current = notifications[index]
        if (current.read) return false
        notifications[index] = current.copy(read = true)
        true
    }

    suspend fun markAllAsRead(userId: String? = null): Int = mutex.withLock {
        var count = 0
        for (i in notifications.indices) {
            val n = notifications[i]
            val matchesUser = userId == null || n.userId == userId
            if (matchesUser && !n.read) {
                notifications[i] = n.copy(read = true)
                count++
            }
        }
        count
    }

    suspend fun delete(id: String): Boolean = mutex.withLock {
        val before = notifications.size
        notifications.removeAll { it.id == id }
        before != notifications.size
    }

    suspend fun deleteAllForUser(userId: String): Int = mutex.withLock {
        val before = notifications.size
        notifications.removeAll { it.userId == userId }
        before - notifications.size
    }

    suspend fun getAll(userId: String? = null): List<Notification> = mutex.withLock {
        if (userId == null) notifications.toList()
        else notifications.filter { it.userId == userId }
    }

    suspend fun getUnread(userId: String? = null): List<Notification> = mutex.withLock {
        notifications.filter { !it.read && (userId == null || it.userId == userId) }
    }

    suspend fun getByType(type: NotificationType, userId: String? = null): List<Notification> = mutex.withLock {
        notifications.filter { it.type == type && (userId == null || it.userId == userId) }
    }

    suspend fun getByPriority(priority: Priority, userId: String? = null): List<Notification> = mutex.withLock {
        notifications.filter { it.priority == priority && (userId == null || it.userId == userId) }
    }

    suspend fun getHighPriorityUnread(userId: String? = null): List<Notification> = mutex.withLock {
        notifications.filter {
            !it.read && it.priority.weight >= Priority.HIGH.weight &&
            (userId == null || it.userId == userId)
        }.sortedByDescending { it.priority.weight }
    }

    suspend fun countUnread(userId: String? = null): Int = mutex.withLock {
        notifications.count { !it.read && (userId == null || it.userId == userId) }
    }

    suspend fun countByType(type: NotificationType, userId: String? = null): Int = mutex.withLock {
        notifications.count { it.type == type && (userId == null || it.userId == userId) }
    }

    suspend fun clear(): Int = mutex.withLock {
        val size = notifications.size
        notifications.clear()
        size
    }

    suspend fun getStatistics(userId: String? = null): NotificationStats = mutex.withLock {
        val relevant = if (userId == null) notifications else notifications.filter { it.userId == userId }
        NotificationStats(
            total = relevant.size,
            unread = relevant.count { !it.read },
            urgent = relevant.count { it.priority == Priority.URGENT },
            byType = NotificationType.entries.associateWith { t -> relevant.count { it.type == t } }
        )
    }

    data class NotificationStats(
        val total: Int,
        val unread: Int,
        val urgent: Int,
        val byType: Map<NotificationType, Int>
    )

    companion object {
        const val MAX_STORAGE = 1000
        const val DEFAULT_TTL_MS = 30L * 24 * 60 * 60 * 1000
    }
}