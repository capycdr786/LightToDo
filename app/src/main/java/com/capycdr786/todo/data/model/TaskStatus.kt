package com.capycdr786.todo.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TaskStatus(val displayName: String) {
    PENDING("Pending"),
    ONGOING("Ongoing"),
    DONE("Done"),
    CANCELLED("Cancelled"),
}
