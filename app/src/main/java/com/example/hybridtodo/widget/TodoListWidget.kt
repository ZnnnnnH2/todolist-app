package com.example.hybridtodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.example.hybridtodo.R
import com.example.hybridtodo.data.repository.TaskRepository
import com.example.hybridtodo.data.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TodoListWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("TodoListWidget", "onUpdate called for ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        // Also notify data changed to trigger initial data load
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // Initialize RetrofitClient
        RetrofitClient.init(context)
        
        when (intent.action) {
            WidgetConstants.ACTION_REFRESH -> {
                Log.d("TodoListWidget", "ACTION_REFRESH received")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisAppWidget = ComponentName(context.packageName, TodoListWidget::class.java.name)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
                
                Log.d("TodoListWidget", "Refreshing ${appWidgetIds.size} widgets")
                
                // Update widget layout first
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
                
                // Then notify data changed
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
                Toast.makeText(context, "Refreshing...", Toast.LENGTH_SHORT).show()
            }
            WidgetConstants.ACTION_TOGGLE_TASK -> {
                val taskId = intent.getStringExtra(WidgetConstants.EXTRA_TASK_ID)
                val isCompleted = intent.getBooleanExtra(WidgetConstants.EXTRA_TASK_COMPLETED, false)
                
                if (taskId != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val success = TaskRepository.toggleTask(taskId, !isCompleted)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                val appWidgetManager = AppWidgetManager.getInstance(context)
                                val thisAppWidget = ComponentName(context.packageName, TodoListWidget::class.java.name)
                                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
                                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
                            } else {
                                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d("TodoListWidget", "updateAppWidget for id: $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Set up the collection
            val intent = Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("hybridtodo://widget/$appWidgetId")
            }
            views.setRemoteAdapter(R.id.widget_list, intent)
            views.setEmptyView(R.id.widget_list, R.id.empty_view)

            // Refresh button
            val refreshIntent = Intent(context, TodoListWidget::class.java).apply {
                action = WidgetConstants.ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

            // Template for list items click
            val toggleIntent = Intent(context, TodoListWidget::class.java).apply {
                action = WidgetConstants.ACTION_TOGGLE_TASK
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, togglePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
