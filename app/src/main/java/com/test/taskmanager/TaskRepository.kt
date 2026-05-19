package com.test.taskmanager
interface TaskRepository { 
    fun getTasks(): List<Task> 
    fun addTask(task: Task)
}