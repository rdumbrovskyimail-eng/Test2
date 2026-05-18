package com.test2.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Главный репозиторий пользователей. Хранит данные в памяти,
 * поддерживает CRUD-операции и поток обновлений через StateFlow.
 */
@Singleton
class UserRepository @Inject constructor() {

    data class User(
        val id: String,
        val name: String,
        val email: String,
        val age: Int,
        val role: UserRole = UserRole.MEMBER,
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis(),
        val isActive: Boolean = true
    )

    enum class UserRole {
        ADMIN, MODERATOR, MEMBER, GUEST
    }

    private val mutex = Mutex()
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: Flow<List<User>> = _users.asStateFlow()

    private var nextIdCounter = 1L

    suspend fun addUser(name: String, email: String, age: Int): Result<User> = mutex.withLock {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be blank"))
        }
        if (!email.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email"))
        }
        if (age < 0 || age > 150) {
            return Result.failure(IllegalArgumentException("Invalid age"))
        }

        val existing = _users.value.find { it.email == email }
        if (existing != null) {
            return Result.failure(IllegalStateException("Email already exists"))
        }

        val newUser = User(
            id = "user_${nextIdCounter++}",
            name = name.trim(),
            email = email.lowercase().trim(),
            age = age
        )

        _users.value = _users.value + newUser
        Result.success(newUser)
    }

    suspend fun removeUser(id: String): Boolean = mutex.withLock {
        val before = _users.value.size
        _users.value = _users.value.filterNot { it.id == id }
        before != _users.value.size
    }

    suspend fun updateUser(
        id: String,
        name: String? = null,
        email: String? = null,
        age: Int? = null,
        role: UserRole? = null
    ): Result<User> = mutex.withLock {
        val current = _users.value.find { it.id == id }
            ?: return Result.failure(NoSuchElementException("User not found: $id"))

        val updated = current.copy(
            name = name?.takeIf { it.isNotBlank() } ?: current.name,
            email = email?.lowercase()?.trim() ?: current.email,
            age = age?.coerceIn(0, 150) ?: current.age,
            role = role ?: current.role,
            updatedAt = System.currentTimeMillis()
        )

        _users.value = _users.value.map { if (it.id == id) updated else it }
        Result.success(updated)
    }

    suspend fun findById(id: String): User? = mutex.withLock {
        _users.value.find { it.id == id }
    }

    suspend fun findByEmail(email: String): User? = mutex.withLock {
        _users.value.find { it.email.equals(email, ignoreCase = true) }
    }

    suspend fun findByRole(role: UserRole): List<User> = mutex.withLock {
        _users.value.filter { it.role == role }
    }

    suspend fun searchByName(query: String): List<User> = mutex.withLock {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        _users.value.filter { it.name.lowercase().contains(q) }
    }

    suspend fun activateUser(id: String): Boolean = mutex.withLock {
        val user = _users.value.find { it.id == id } ?: return false
        if (user.isActive) return false
        _users.value = _users.value.map {
            if (it.id == id) it.copy(isActive = true, updatedAt = System.currentTimeMillis()) else it
        }
        true
    }

    suspend fun deactivateUser(id: String): Boolean = mutex.withLock {
        val user = _users.value.find { it.id == id } ?: return false
        if (!user.isActive) return false
        _users.value = _users.value.map {
            if (it.id == id) it.copy(isActive = false, updatedAt = System.currentTimeMillis()) else it
        }
        true
    }

    suspend fun getActiveUsers(): List<User> = mutex.withLock {
        _users.value.filter { it.isActive }
    }

    suspend fun getInactiveUsers(): List<User> = mutex.withLock {
        _users.value.filter { !it.isActive }
    }

    suspend fun count(): Int = mutex.withLock { _users.value.size }

    suspend fun countActive(): Int = mutex.withLock {
        _users.value.count { it.isActive }
    }

    suspend fun getStatistics(): UserStatistics = mutex.withLock {
        val all = _users.value
        UserStatistics(
            totalUsers = all.size,
            activeUsers = all.count { it.isActive },
            inactiveUsers = all.count { !it.isActive },
            adminCount = all.count { it.role == UserRole.ADMIN },
            moderatorCount = all.count { it.role == UserRole.MODERATOR },
            memberCount = all.count { it.role == UserRole.MEMBER },
            guestCount = all.count { it.role == UserRole.GUEST },
            averageAge = if (all.isEmpty()) 0.0 else all.map { it.age }.average()
        )
    }

    suspend fun clearAll(): Int = mutex.withLock {
        val size = _users.value.size
        _users.value = emptyList()
        nextIdCounter = 1L
        size
    }

    data class UserStatistics(
        val totalUsers: Int,
        val activeUsers: Int,
        val inactiveUsers: Int,
        val adminCount: Int,
        val moderatorCount: Int,
        val memberCount: Int,
        val guestCount: Int,
        val averageAge: Double
    )

    companion object {
        const val MAX_USERS = 10_000
        const val MIN_AGE = 0
        const val MAX_AGE = 150
    }
}