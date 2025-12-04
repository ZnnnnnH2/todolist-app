package com.example.hybridtodo.data.model

import com.google.gson.annotations.SerializedName

data class Task(
    val id: String,
    val title: String,
    @SerializedName("completed")
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
