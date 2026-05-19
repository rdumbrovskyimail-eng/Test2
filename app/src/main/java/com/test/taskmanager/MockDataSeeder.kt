package com.test.taskmanager

import java.time.LocalDateTime

enum class TaskPriority { LOW, MEDIUM, HIGH, URGENT }

data class Task(
    val id: String,
    val title: String,
    val isDone: Boolean,
    val description: String,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val tags: Set<String> = emptySet(),
    val dueDate: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class TaskStats(val total: Int, val completed: Int, val pending: Int)
data class TaskFilter(val priority: TaskPriority? = null, val tags: Set<String> = emptySet())
data class TaskBatch(val tasks: List<Task>, val batchId: String)
data class TaskChangelog(val taskId: String, val change: String, val timestamp: LocalDateTime = LocalDateTime.now())

object MockDataSeeder {
    val preloadTasks = listOf(
        Task("1", "Купить молоко", false, "Сходить в магазин вечером", TaskPriority.MEDIUM, setOf("shopping")),
        Task("2", "Позвонить маме", true, "Узнать как дела"),
        Task("3", "Написать код", false, "Доделать Pipeline в OpusIDE"),
        Task("4", "Покормить кота", false, "Купить влажный корм"),
        Task("5", "Заплатить за интернет", true, "Провайдер Ростелеком"),
        Task("6", "Сходить в зал", false, "Тренировка спины"),
        Task("7", "Прочитать статью", false, "Статья про Kotlin Coroutines"),
        Task("8", "Заказать воду", true, "2 бутылки по 19 литров"),
        Task("9", "Помыть машину", false, "Комплексная мойка"),
        Task("10", "Купить билеты", false, "Билеты в кино на выходные"),
        Task("11", "Забрать посылку", true, "Пункт выдачи Ozon"),
        Task("12", "Приготовить ужин", false, "Паста с курицей"),
        Task("13", "Сдать вещи в химчистку", false, "Зимняя куртка"),
        Task("14", "Обновить резюме", false, "Добавить новые навыки"),
        Task("15", "Полить цветы", true, "Особенно фикус"),
        Task("16", "Записаться к врачу", false, "Стоматолог"),
        Task("17", "Купить подарок", false, "День рождения друга"),
        Task("18", "Починить кран", false, "Капает на кухне"),
        Task("19", "Разобрать почту", true, "Удалить спам"),
        Task("20", "Сделать бэкап", false, "Скопировать фото на жесткий диск"),
        Task("21", "Купить батарейки", false, "ААА для пульта"),
        Task("22", "Выбросить мусор", true, "Раздельный сбор"),
        Task("23", "Погладить рубашки", false, "На рабочую неделю"),
        Task("24", "Заправить машину", false, "Полный бак 95-го"),
        Task("25", "Купить хлеб", true, "Цельнозерновой"),
        Task("26", "Проверить баланс", false, "На банковской карте"),
        Task("27", "Написать отчет", false, "За прошедший месяц"),
        Task("28", "Спланировать отпуск", false, "Посмотреть билеты"),
        Task("29", "Купить кофе", true, "Зерновой, арабика"),
        Task("30", "Почистить ноутбук", false, "От пыли"),
        Task("31", "Заменить лампочку", false, "В коридоре"),
        Task("32", "Купить чай", true, "Зеленый с жасмином"),
        Task("33", "Проверить почту", false, "Рабочий ящик"),
        Task("34", "Сделать зарядку", true, "15 минут"),
        Task("35", "Купить фрукты", false, "Яблоки и бананы"),
        Task("36", "Помыть посуду", true, "После ужина"),
        Task("37", "Заказать пиццу", false, "Маргарита"),
        Task("38", "Посмотреть фильм", false, "Новинка в кино"),
        Task("39", "Купить овощи", true, "Огурцы и помидоры"),
        Task("40", "Сделать уборку", false, "Пропылесосить"),
        Task("41", "Купить сыр", true, "Пармезан"),
        Task("42", "Проверить штрафы", false, "ГИБДД"),
        Task("43", "Купить мясо", false, "Куриное филе"),
        Task("44", "Сделать уроки", true, "Английский язык"),
        Task("45", "Купить рыбу", false, "Лосось"),
        Task("46", "Проверить налоги", false, "Госуслуги"),
        Task("47", "Купить яйца", true, "Десяток"),
        Task("48", "Сделать проект", false, "По работе"),
        Task("49", "Купить масло", false, "Сливочное"),
        Task("50", "Проверить счета", true, "ЖКХ")
    )

    fun getTaskById(id: String): Task? {
        return preloadTasks.find { it.id == id }
    }

    fun getCompletedTasks(): List<Task> {
        return preloadTasks.filter { it.isDone }
    }

    fun getPendingTasks(): List<Task> {
        return preloadTasks.filter { !it.isDone }
    }

    fun getTasksByPriority(priority: TaskPriority) = preloadTasks.filter { it.priority == priority }
    fun getTasksByTag(tag: String) = preloadTasks.filter { it.tags.contains(tag) }
    fun getOverdueTasks() = preloadTasks.filter { it.dueDate?.isBefore(LocalDateTime.now()) == true }
    fun getTasksDueWithin(hours: Long) = preloadTasks.filter { it.dueDate?.isBefore(LocalDateTime.now().plusHours(hours)) == true }
    fun getTasksSortedByPriority() = preloadTasks.sortedBy { it.priority }
    fun getTasksSortedByDueDate() = preloadTasks.sortedBy { it.dueDate }
    fun getTasksSortedByCreatedAt() = preloadTasks.sortedBy { it.createdAt }
    fun searchTasks(query: String) = preloadTasks.filter { it.title.contains(query, true) || it.description.contains(query, true) }
    fun getTaskStats() = TaskStats(preloadTasks.size, preloadTasks.count { it.isDone }, preloadTasks.count { !it.isDone })
    fun getTasksGroupedByPriority() = preloadTasks.groupBy { it.priority }
    fun getTasksGroupedByStatus() = preloadTasks.groupBy { it.isDone }
    fun getTasksGroupedByTag() = preloadTasks.flatMap { task -> task.tags.map { it to task } }.groupBy({ it.first }, { it.second })
    fun getRecentlyUpdatedTasks(limit: Int = 5) = preloadTasks.sortedByDescending { it.createdAt }.take(limit)
    fun getRandomTask() = preloadTasks.randomOrNull()
    fun getRandomPendingTask() = getPendingTasks().randomOrNull()
    fun generateTaskId() = java.util.UUID.randomUUID().toString()
    fun createTask(title: String, description: String) = Task(generateTaskId(), title, false, description)
    fun duplicateTask(task: Task) = task.copy(id = generateTaskId())
    fun markTaskDone(task: Task) = task.copy(isDone = true)
    fun markTaskPending(task: Task) = task.copy(isDone = false)
    fun updateTaskPriority(task: Task, priority: TaskPriority) = task.copy(priority = priority)
    fun addTagToTask(task: Task, tag: String) = task.copy(tags = task.tags + tag)
    fun removeTagFromTask(task: Task, tag: String) = task.copy(tags = task.tags - tag)
    fun applyFilter(tasks: List<Task>, filter: TaskFilter) = tasks.filter { (filter.priority == null || it.priority == filter.priority) && (filter.tags.isEmpty() || it.tags.containsAll(filter.tags)) }
    fun splitIntoBatches(tasks: List<Task>, size: Int) = tasks.chunked(size).mapIndexed { i, list -> TaskBatch(list, "batch-$i") }
    fun getPaginatedTasks(page: Int, size: Int) = preloadTasks.drop(page * size).take(size)
    fun getTaskCompletionTimeline() = preloadTasks.filter { it.isDone }.sortedBy { it.createdAt }
    fun exportToJson(tasks: List<Task>): String = "[]"
    fun importFromJson(json: String): List<Task> = emptyList()
    fun getAssigneeSummary() = mapOf<String, Int>()
    fun validateTask(task: Task) = task.title.isNotBlank()
    fun getTopTags(limit: Int = 5) = preloadTasks.flatMap { it.tags }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }.take(limit)
    fun mergeTaskLists(list1: List<Task>, list2: List<Task>) = (list1 + list2).distinctBy { it.id }
    fun generateStressTestTasks(count: Int) = List(count) { createTask("Stress $it", "Desc $it") }
    fun computeTaskHash(task: Task) = task.hashCode()
    fun diffTasks(t1: Task, t2: Task) = t1 != t2
    fun getTasksWithAllTags(tags: Set<String>) = preloadTasks.filter { it.tags.containsAll(tags) }
    fun getTasksWithAnyTag(tags: Set<String>) = preloadTasks.filter { it.tags.any { tag -> tags.contains(tag) } }
}

object TaskIndex {
    private val index = mutableMapOf<String, Task>()
    fun add(task: Task) { index[task.id] = task }
    fun get(id: String) = index[id]
}

object MockDataSeederV2 {
    fun seed() = MockDataSeeder.preloadTasks
}