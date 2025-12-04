package com.example.hybridtodo.data.model

data class Task(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val priority: String
)

data class ToggleTaskRequest(
    val id: String,
    val isCompleted: Boolean
)

data class BasicResponse(
    val success: Boolean
)
