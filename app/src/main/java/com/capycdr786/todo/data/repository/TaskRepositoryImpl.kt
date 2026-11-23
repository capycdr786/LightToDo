package com.capycdr786.todo.data.repository

import com.capycdr786.todo.data.local.TaskDao
import com.capycdr786.todo.data.model.Task
import com.capycdr786.todo.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    override fun getTasksByDate(date: LocalDate): Flow<List<Task>> = taskDao.getTasksByDate(date)

    override fun getTaskCountsByDate(): Flow<Map<LocalDate, Int>> =
        taskDao.getTaskCountsByDate().map { list ->
            list.associate { it.date to it.count }
        }

    override suspend fun getTaskById(id: String): Task? = taskDao.getTaskById(id)

    override suspend fun addTask(task: Task) {
        taskDao.insertTask(task)
    }

    override suspend fun insertTasks(tasks: List<Task>) = taskDao.insertTasks(tasks)

    override suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    override suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    override suspend fun deleteTaskById(id: String) = taskDao.deleteTaskById(id)

    override suspend fun deleteTasksInRange(startDate: LocalDate, endDate: LocalDate): Int =
        taskDao.deleteTasksInRange(startDate, endDate)

    override suspend fun deleteTasksByRepeatGroupId(repeatGroupId: String): Int =
        taskDao.deleteTasksByRepeatGroupId(repeatGroupId)

    override suspend fun deleteRepeatGroupTasksInRange(
        repeatGroupId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Int = taskDao.deleteRepeatGroupTasksInRange(repeatGroupId, startDate, endDate)

    override suspend fun toggleTaskCompletion(taskId: String) {
        val task = taskDao.getTaskById(taskId) ?: return
        val newIsCompleted = !task.isCompleted
        val newStatus = if (newIsCompleted) TaskStatus.DONE else TaskStatus.PENDING
        taskDao.updateTask(task.copy(isCompleted = newIsCompleted, status = newStatus))
    }

    override suspend fun rescheduleTask(taskId: String, newDate: LocalDate) {
        val task = taskDao.getTaskById(taskId) ?: return
        taskDao.updateTask(task.copy(date = newDate, status = TaskStatus.PENDING, isCompleted = false))
    }

    override suspend fun setTaskStatus(taskId: String, status: TaskStatus, cancellationReason: String?) {
        val task = taskDao.getTaskById(taskId) ?: return
        val isCompleted = (status == TaskStatus.DONE)
        val reason = if (status == TaskStatus.CANCELLED) cancellationReason else null
        taskDao.updateTask(task.copy(status = status, isCompleted = isCompleted, cancellationReason = reason))
    }
}
