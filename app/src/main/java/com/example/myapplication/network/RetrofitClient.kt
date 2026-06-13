package com.example.myapplication.network

import com.example.myapplication.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    // Interceptor to remove "Secure", "HttpOnly", and "SameSite" attributes from cookies 
    // for local development over HTTP, ensuring the CookieJar accepts and sends them.
    private val cookieInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        val cookies = response.headers("Set-Cookie")
        if (cookies.isNotEmpty()) {
            val modifiedResponse = response.newBuilder()
            modifiedResponse.removeHeader("Set-Cookie")
            for (cookie in cookies) {
                val modifiedCookie = cookie
                    .replace(Regex(";\\s*Secure", RegexOption.IGNORE_CASE), "")
                    .replace(Regex(";\\s*HttpOnly", RegexOption.IGNORE_CASE), "")
                    .replace(Regex(";\\s*SameSite=[a-zA-Z]+", RegexOption.IGNORE_CASE), "")
                modifiedResponse.addHeader("Set-Cookie", modifiedCookie)
            }
            modifiedResponse.build()
        } else {
            response
        }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(cookieManager))
        .addInterceptor(logging)
        .addNetworkInterceptor(cookieInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val authApiClient: AuthApiClient by lazy {
        retrofit.create(AuthApiClient::class.java)
    }

    val poApiService: PoApiService by lazy {
        retrofit.create(PoApiService::class.java)
    }

    val checkerOutputApiService: CheckerOutputApiService by lazy {
        retrofit.create(CheckerOutputApiService::class.java)
    }
}
