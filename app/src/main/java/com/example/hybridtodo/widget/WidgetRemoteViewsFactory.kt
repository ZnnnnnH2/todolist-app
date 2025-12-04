package com.example.hybridtodo.widget

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.hybridtodo.R
import com.example.hybridtodo.data.model.Task
import com.example.hybridtodo.data.repository.TaskRepository
import com.example.hybridtodo.data.remote.RetrofitClient
import kotlinx.coroutines.runBlocking

class WidgetRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var taskList: List<Task> = emptyList()

    override fun onCreate() {
        // Initialize data source if needed
    }

    override fun onDataSetChanged() {
        Log.d("WidgetFactory", "onDataSetChanged called")
        
        // Initialize RetrofitClient
        RetrofitClient.init(context)
        
        val identity = Binder.clearCallingIdentity()
        try {
            // Check for cookies from SharedPreferences
            val prefs = context.getSharedPreferences("widget_auth", Context.MODE_PRIVATE)
            val cookie = prefs.getString("cookie", null)
            
            if (cookie.isNullOrEmpty()) {
                Log.e("WidgetFactory", "Cookie is missing")
                taskList = listOf(
                    Task(
                        id = "error_login",
                        title = "未登录，请打开App刷新",
                        isCompleted = false,
                        priority = "High"
                    )
                )
                return
            }

            runBlocking {
                try {
                    taskList = TaskRepository.fetchTasks()
                    Log.d("WidgetFactory", "Fetched ${taskList.size} tasks")
                    
                    if (taskList.isEmpty()) {
                        taskList = listOf(
                            Task(
                                id = "empty",
                                title = "暂无待办事项",
                                isCompleted = false,
                                priority = "Low"
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("WidgetFactory", "Error fetching tasks", e)
                    taskList = listOf(
                        Task(
                            id = "error_network",
                            title = "网络错误，点击刷新",
                            isCompleted = false,
                            priority = "High"
                        )
                    )
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity)
        }
    }

    override fun onDestroy() {
        taskList = emptyList()
    }

    override fun getCount(): Int {
        return taskList.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (position == -1 || position >= taskList.size) {
            return RemoteViews(context.packageName, R.layout.widget_item)
        }

        val task = taskList[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)

        views.setTextViewText(R.id.tv_task_title, task.title)

        // Set Checkbox Icon
        if (task.isCompleted) {
            views.setImageViewResource(R.id.iv_check, R.drawable.ic_check_filled)
        } else {
            views.setImageViewResource(R.id.iv_check, R.drawable.ic_check_outline)
        }

        // Set Priority Color
        val priorityColor = when (task.priority.lowercase()) {
            "high" -> context.getColor(R.color.priority_high)
            "medium" -> context.getColor(R.color.priority_medium)
            "low" -> context.getColor(R.color.priority_low)
            else -> context.getColor(R.color.teal_200)
        }
        
        try {
            views.setInt(R.id.view_priority, "setBackgroundColor", priorityColor)
        } catch (e: Exception) {
            Log.e("WidgetFactory", "Error setting priority color", e)
        }

        // Fill In Intent for Click
        val fillInIntent = Intent().apply {
            putExtra(WidgetConstants.EXTRA_TASK_ID, task.id)
            putExtra(WidgetConstants.EXTRA_TASK_COMPLETED, task.isCompleted)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews {
        // Return a custom loading view
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        views.setTextViewText(R.id.tv_task_title, "加载中...")
        return views
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
