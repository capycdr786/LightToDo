package com.capycdr786.todo.data.local

import androidx.room.TypeConverter
import com.capycdr786.todo.data.model.TaskStatus
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus?): String? = status?.name

    @TypeConverter
    fun toTaskStatus(statusString: String?): TaskStatus {
        return statusString?.let {
            try {
                if (it == "TODO") TaskStatus.PENDING else TaskStatus.valueOf(it)
            } catch (_: IllegalArgumentException) {
                TaskStatus.PENDING
            }
        } ?: TaskStatus.PENDING
    }
}
