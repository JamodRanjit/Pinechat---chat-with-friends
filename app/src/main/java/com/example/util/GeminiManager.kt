package com.example.util

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// Retrofit implementation using Moshi
data class Content(val parts: List<Part>)
data class Part(val text: String)
data class GenerateContentRequest(val contents: List<Content>)
data class ResponsePart(val text: String)
data class Candidate(val content: ResponseContent)
data class ResponseContent(val parts: List<ResponsePart>)
data class GenerateContentResponse(val candidates: List<Candidate>)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiManager {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(OkHttpClient.Builder().build())
        .build()

    private val service = retrofit.create(GeminiApiService::class.java)

    suspend fun generateQuest(): String {
        val request = GenerateContentRequest(
            contents = listOf(Content(listOf(Part("Generate a daily quest for a social chat app. Return just the quest string, like \"Earn 200 points in 5 days to get extra 100 points\". No intro or extra text."))))
        )
        return try {
            val response = service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Earn 100 points today for 10 bonus!"
        } catch (e: Exception) {
            "Earn 100 points today for 10 bonus!"
        }
    }
}
