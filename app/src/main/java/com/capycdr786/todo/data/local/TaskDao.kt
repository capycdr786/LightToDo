package com.capycdr786.todo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.capycdr786.todo.data.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, id DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY isCompleted ASC, id DESC")
    fun getTasksByDate(date: LocalDate): Flow<List<Task>>

    @Query("SELECT date, COUNT(*) as count FROM tasks GROUP BY date")
    fun getTaskCountsByDate(): Flow<List<DateCount>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks WHERE date >= :startDate AND date <= :endDate")
    suspend fun deleteTasksInRange(startDate: LocalDate, endDate: LocalDate): Int

    @Query("DELETE FROM tasks WHERE repeat_group_id = :repeatGroupId")
    suspend fun deleteTasksByRepeatGroupId(repeatGroupId: String): Int

    @Query("DELETE FROM tasks WHERE repeat_group_id = :repeatGroupId AND date >= :startDate AND date <= :endDate")
    suspend fun deleteRepeatGroupTasksInRange(
        repeatGroupId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Int
}

data class DateCount(
    val date: LocalDate,
    val count: Int,
)
