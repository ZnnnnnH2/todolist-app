package com.example.hybridtodo.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context

object RetrofitClient {
    private const val BASE_URL = "http://20.193.248.140/" // Replace with actual URL

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    // Cookie Interceptor - reads cookie dynamically from SharedPreferences on each request
    private val cookieInterceptor by lazy {
        okhttp3.Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            
            // Get Cookie from SharedPreferences - this runs on each request so appContext should be set
            appContext?.let { ctx ->
                val prefs = ctx.getSharedPreferences("widget_auth", Context.MODE_PRIVATE)
                val cookie = prefs.getString("cookie", null)
                android.util.Log.d("RetrofitClient", "Cookie from prefs: ${cookie?.take(50)}...")
                if (!cookie.isNullOrEmpty()) {
                    requestBuilder.addHeader("Cookie", cookie)
                }
            } ?: run {
                android.util.Log.e("RetrofitClient", "appContext is null! Cookie will not be sent.")
            }
            
            chain.proceed(requestBuilder.build())
        }
    }

    // Lazy initialization ensures init() is called first
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(cookieInterceptor)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
