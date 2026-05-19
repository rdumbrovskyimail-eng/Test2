package com.test.taskmanager
interface TaskRepository { 
    fun getTasks(): List<Task> 
}