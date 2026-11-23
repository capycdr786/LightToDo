package com.capycdr786.todo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.capycdr786.todo.data.model.Task

@Database(entities = [Task::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE tasks_new (id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, date TEXT NOT NULL, status TEXT NOT NULL, isCompleted INTEGER NOT NULL, cancellation_reason TEXT)",
                )
                db.execSQL(
                    "INSERT INTO tasks_new (id, title, description, date, status, isCompleted, cancellation_reason) SELECT CAST(id AS TEXT), title, description, date, status, isCompleted, cancellation_reason FROM tasks",
                )
                db.execSQL("DROP TABLE tasks")
                db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeat_group_id TEXT DEFAULT NULL")
            }
        }
    }
}
