package com.example.hybridtodo.data.repository

import android.util.Log
import com.example.hybridtodo.data.model.Task
import com.example.hybridtodo.data.model.ToggleTaskRequest
import com.example.hybridtodo.data.remote.RetrofitClient

object TaskRepository {
    private const val TAG = "TaskRepository"

    suspend fun fetchTasks(): List<Task> {
        return try {
            RetrofitClient.apiService.getTasks()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks", e)
            emptyList()
        }
    }

    suspend fun toggleTask(id: String, isCompleted: Boolean): Boolean {
        return try {
            val response = RetrofitClient.apiService.toggleTask(ToggleTaskRequest(id, isCompleted))
            if (response.isSuccessful) {
                true
            } else {
                Log.e(TAG, "Toggle task failed: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling task", e)
            false
        }
    }
}
