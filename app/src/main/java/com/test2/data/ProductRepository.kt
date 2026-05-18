package com.test2.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий товаров. Поддерживает каталог, фильтрацию по категории,
 * поиск и расчёт сумм корзины. Цены в евроцентах (Int) для избежания
 * проблем с плавающей точкой.
 */
@Singleton
class ProductRepository @Inject constructor() {

    data class Product(
        val id: String,
        val name: String,
        val description: String,
        val priceCents: Int,
        val category: Category,
        val stock: Int,
        val tags: List<String> = emptyList(),
        val createdAt: Long = System.currentTimeMillis()
    ) {
        val priceEur: Double get() = priceCents / 100.0
        val inStock: Boolean get() = stock > 0
    }

    enum class Category {
        ELECTRONICS, CLOTHING, BOOKS, FOOD, HOME, SPORTS, TOYS, OTHER
    }

    private val mutex = Mutex()
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: Flow<List<Product>> = _products.asStateFlow()

    private var nextIdCounter = 1L

    suspend fun addProduct(
        name: String,
        description: String,
        priceCents: Int,
        category: Category,
        stock: Int = 0,
        tags: List<String> = emptyList()
    ): Result<Product> = mutex.withLock {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Name required"))
        }
        if (priceCents < 0) {
            return Result.failure(IllegalArgumentException("Price cannot be negative"))
        }
        if (stock < 0) {
            return Result.failure(IllegalArgumentException("Stock cannot be negative"))
        }

        val product = Product(
            id = "prod_${nextIdCounter++}",
            name = name.trim(),
            description = description.trim(),
            priceCents = priceCents,
            category = category,
            stock = stock,
            tags = tags.map { it.trim() }.filter { it.isNotBlank() }
        )
        _products.value = _products.value + product
        Result.success(product)
    }

    suspend fun removeProduct(id: String): Boolean = mutex.withLock {
        val before = _products.value.size
        _products.value = _products.value.filterNot { it.id == id }
        before != _products.value.size
    }

    suspend fun updateStock(id: String, newStock: Int): Result<Product> = mutex.withLock {
        if (newStock < 0) {
            return Result.failure(IllegalArgumentException("Stock cannot be negative"))
        }
        val current = _products.value.find { it.id == id }
            ?: return Result.failure(NoSuchElementException("Product not found"))

        val updated = current.copy(stock = newStock)
        _products.value = _products.value.map { if (it.id == id) updated else it }
        Result.success(updated)
    }

    suspend fun decreaseStock(id: String, amount: Int): Result<Product> = mutex.withLock {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }
        val current = _products.value.find { it.id == id }
            ?: return Result.failure(NoSuchElementException("Product not found"))
        if (current.stock < amount) {
            return Result.failure(IllegalStateException("Not enough stock"))
        }

        val updated = current.copy(stock = current.stock - amount)
        _products.value = _products.value.map { if (it.id == id) updated else it }
        Result.success(updated)
    }

    suspend fun findByCategory(category: Category): List<Product> = mutex.withLock {
        _products.value.filter { it.category == category }
    }

    suspend fun searchByName(query: String): List<Product> = mutex.withLock {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        _products.value.filter {
            it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }

    suspend fun searchByTag(tag: String): List<Product> = mutex.withLock {
        if (tag.isBlank()) return emptyList()
        val t = tag.lowercase()
        _products.value.filter { it.tags.any { tg -> tg.lowercase() == t } }
    }

    suspend fun findInPriceRange(minCents: Int, maxCents: Int): List<Product> = mutex.withLock {
        _products.value.filter { it.priceCents in minCents..maxCents }
    }

    suspend fun getInStockProducts(): List<Product> = mutex.withLock {
        _products.value.filter { it.inStock }
    }

    suspend fun getOutOfStockProducts(): List<Product> = mutex.withLock {
        _products.value.filter { !it.inStock }
    }

    suspend fun calculateTotalInventoryValue(): Long = mutex.withLock {
        _products.value.sumOf { (it.priceCents.toLong() * it.stock) }
    }

    suspend fun getCheapestProduct(): Product? = mutex.withLock {
        _products.value.minByOrNull { it.priceCents }
    }

    suspend fun getMostExpensiveProduct(): Product? = mutex.withLock {
        _products.value.maxByOrNull { it.priceCents }
    }

    suspend fun getCategoryStatistics(): Map<Category, CategoryStats> = mutex.withLock {
        Category.entries.associateWith { cat ->
            val items = _products.value.filter { it.category == cat }
            CategoryStats(
                productCount = items.size,
                totalStock = items.sumOf { it.stock },
                averagePriceCents = if (items.isEmpty()) 0 else items.map { it.priceCents }.average().toInt(),
                inStockCount = items.count { it.inStock }
            )
        }
    }

    data class CategoryStats(
        val productCount: Int,
        val totalStock: Int,
        val averagePriceCents: Int,
        val inStockCount: Int
    )

    suspend fun calculateBasketTotal(items: Map<String, Int>): Long = mutex.withLock {
        var total = 0L
        for ((id, quantity) in items) {
            val product = _products.value.find { it.id == id } ?: continue
            total += product.priceCents.toLong() * quantity
        }
        total
    }

    suspend fun count(): Int = mutex.withLock { _products.value.size }

    suspend fun clearAll(): Int = mutex.withLock {
        val size = _products.value.size
        _products.value = emptyList()
        nextIdCounter = 1L
        size
    }

    companion object {
        const val MAX_PRODUCTS = 100_000
        const val PRICE_DISPLAY_PRECISION = 2
    }
}