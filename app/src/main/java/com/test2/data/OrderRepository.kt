package com.test2.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor() {

    data class Order(
        val id: String = UUID.randomUUID().toString(),
        val userId: String,
        val items: Map<String, Int>,
        val totalCents: Long,
        val status: OrderStatus = OrderStatus.PENDING,
        val createdAt: Long = System.currentTimeMillis()
    )

    enum class OrderStatus { PENDING, PAID, SHIPPED, DELIVERED, CANCELLED }

    private val mutex = Mutex()
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: Flow<List<Order>> = _orders.asStateFlow()

    suspend fun createOrder(userId: String, items: Map<String, Int>, totalCents: Long): Order = mutex.withLock {
        val order = Order(userId = userId, items = items, totalCents = totalCents)
        _orders.value = _orders.value + order
        order
    }

    suspend fun updateStatus(id: String, status: OrderStatus): Boolean = mutex.withLock {
        val current = _orders.value.find { it.id == id } ?: return false
        _orders.value = _orders.value.map { if (it.id == id) it.copy(status = status) else it }
        true
    }

    suspend fun getByUser(userId: String): List<Order> = mutex.withLock {
        _orders.value.filter { it.userId == userId }
    }

    suspend fun getByStatus(status: OrderStatus): List<Order> = mutex.withLock {
        _orders.value.filter { it.status == status }
    }

    suspend fun getTotalRevenue(): Long = mutex.withLock {
        _orders.value.filter { it.status != OrderStatus.CANCELLED }.sumOf { it.totalCents }
    }

    suspend fun count(): Int = mutex.withLock { _orders.value.size }
}