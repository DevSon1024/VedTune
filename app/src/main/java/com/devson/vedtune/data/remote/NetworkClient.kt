package com.devson.vedtune.data.remote

import com.devson.vedtune.data.remote.api.LrcLibApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {

    private const val BASE_URL = "https://lrclib.net/"
    private const val USER_AGENT = "VedTune v1.0 (https://github.com/DevSon1024/VedTune)"

    private val userAgentInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", USER_AGENT)
            .build()
        chain.proceed(requestWithUserAgent)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val lrcLibApi: LrcLibApi by lazy {
        retrofit.create(LrcLibApi::class.java)
    }
}
