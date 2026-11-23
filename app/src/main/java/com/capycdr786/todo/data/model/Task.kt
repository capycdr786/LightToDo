package com.capycdr786.todo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@Serializable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate = LocalDate.now(),
    val status: TaskStatus = TaskStatus.PENDING,
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "cancellation_reason")
    val cancellationReason: String? = null,
    @ColumnInfo(name = "repeat_group_id")
    val repeatGroupId: String? = null,
)
