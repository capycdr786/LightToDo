package com.capycdr786.todo.data.repository

import com.capycdr786.todo.data.model.Task
import com.capycdr786.todo.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByDate(date: LocalDate): Flow<List<Task>>
    fun getTaskCountsByDate(): Flow<Map<LocalDate, Int>>
    suspend fun getTaskById(id: String): Task?
    suspend fun addTask(task: Task)
    suspend fun insertTasks(tasks: List<Task>)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun deleteTaskById(id: String)
    suspend fun deleteTasksInRange(startDate: LocalDate, endDate: LocalDate): Int
    suspend fun deleteTasksByRepeatGroupId(repeatGroupId: String): Int
    suspend fun deleteRepeatGroupTasksInRange(
        repeatGroupId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Int
    suspend fun toggleTaskCompletion(taskId: String)
    suspend fun rescheduleTask(taskId: String, newDate: LocalDate)
    suspend fun setTaskStatus(taskId: String, status: TaskStatus, cancellationReason: String? = null)
}
