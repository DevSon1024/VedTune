package com.devson.vedtune.data.remote

import com.devson.vedtune.data.remote.api.LrcLibApi
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object NetworkClient {

    private const val BASE_URL = "https://lrclib.net/"
    private const val USER_AGENT = "VedTune v1.0 (https://github.com/DevSon1024/VedTune)"

    // Verified Cloudflare Anycast IPv4 and IPv6 addresses for lrclib.net
    private val LRCLIB_FALLBACK_IPS = listOf(
        // IPv4 Anycast
        byteArrayOf(104.toByte(), 21.toByte(), 13.toByte(), 116.toByte()),
        byteArrayOf(172.toByte(), 67.toByte(), 167.toByte(), 238.toByte()),
        // IPv6 Anycast
        byteArrayOf(
            0x26.toByte(), 0x06.toByte(), 0x47.toByte(), 0x00.toByte(),
            0x30.toByte(), 0x31.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0xac.toByte(), 0x43.toByte(), 0xa7.toByte(), 0xee.toByte()
        ),
        byteArrayOf(
            0x26.toByte(), 0x06.toByte(), 0x47.toByte(), 0x00.toByte(),
            0x30.toByte(), 0x33.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x68.toByte(), 0x15.toByte(), 0x0d.toByte(), 0x74.toByte()
        )
    )

    private val resilientDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            // 1. Try standard system DNS first
            try {
                val systemAddresses = Dns.SYSTEM.lookup(hostname)
                if (systemAddresses.isNotEmpty()) {
                    return systemAddresses
                }
            } catch (_: Exception) {
                // System DNS failed, fallback to direct Anycast IP resolution
            }

            // 2. Direct Anycast IP resolution for lrclib.net to bypass carrier DNS drops / blocks
            if (hostname.equals("lrclib.net", ignoreCase = true)) {
                val fallbackAddresses = LRCLIB_FALLBACK_IPS.mapNotNull { ipBytes ->
                    try {
                        InetAddress.getByAddress(hostname, ipBytes)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (fallbackAddresses.isNotEmpty()) {
                    return fallbackAddresses
                }
            }

            // 3. Fallback to standard DNS lookup
            return Dns.SYSTEM.lookup(hostname)
        }
    }

    private val userAgentInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", USER_AGENT)
            .build()
        chain.proceed(requestWithUserAgent)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(resilientDns)
            .addInterceptor(userAgentInterceptor)
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
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
