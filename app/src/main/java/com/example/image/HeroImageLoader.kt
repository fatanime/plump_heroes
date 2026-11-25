package com.example.image

import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.util.TypedValue
import androidx.annotation.DrawableRes
import coil.ImageLoader
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import com.example.R
import com.example.model.inferHeroIdFromDrawableName
import com.example.model.resolveHeroFallbackDrawableRes
import com.example.model.resolveHeroImageNameForLevel

/**
 * Central Coil ImageLoader configuration with graceful resource resolution and
 * level-based character portrait fallback handling.
 */
object HeroImageLoader {
    private const val TAG = "HeroImageLoader"

    const val PARAM_HERO_ID = "hero_id"
    const val PARAM_HERO_NAME = "hero_name"
    const val PARAM_HERO_LEVEL = "hero_level"
    const val PARAM_FALLBACK_RES = "fallback_res"

    @Volatile
    private var instance: ImageLoader? = null

    fun getOrCreate(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }
    }

    fun create(context: Context): ImageLoader {
        val appContext = context.applicationContext
        return ImageLoader.Builder(appContext)
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            .crossfade(false)
            .components {
                add(SafeHeroResourceInterceptor(appContext))
            }
            .build()
    }

    fun buildHeroImageRequest(
        context: Context,
        data: Any?,
        heroId: String = "",
        heroName: String = "",
        heroLevel: Int = 1,
        @DrawableRes fallbackRes: Int = 0
    ): ImageRequest {
        val safeFallback = resolveSafeFallbackRes(
            context = context,
            heroId = heroId,
            heroName = heroName,
            explicitFallbackRes = fallbackRes
        )
        return ImageRequest.Builder(context)
            .data(data ?: safeFallback)
            .fallback(safeFallback)
            .error(safeFallback)
            .setParameter(PARAM_HERO_ID, heroId)
            .setParameter(PARAM_HERO_NAME, heroName)
            .setParameter(PARAM_HERO_LEVEL, heroLevel)
            .setParameter(PARAM_FALLBACK_RES, safeFallback)
            .build()
    }

    /**
     * Resolves a hero's level-specific drawable resource safely. If the expected
     * level-based resource file is missing or invalid, gracefully falls back to the
     * hero's base portrait or app fallback.
     */
    @DrawableRes
    fun resolveLevelDrawableSafely(
        context: Context,
        heroId: String,
        heroLevel: Int,
        heroName: String = "",
        @DrawableRes currentRes: Int = 0
    ): Int {
        val expectedName = resolveHeroImageNameForLevel(heroId, heroLevel, "hero_$heroId")
        val byName = lookupDrawableByName(context, expectedName)
        if (byName != 0 && isDrawableResValid(context, byName)) {
            return byName
        }
        if (currentRes != 0 && isDrawableResValid(context, currentRes)) {
            return currentRes
        }
        return resolveSafeFallbackRes(context, heroId, heroName, currentRes)
    }

    @DrawableRes
    fun resolveSafeFallbackRes(
        context: Context,
        heroId: String = "",
        heroName: String = "",
        @DrawableRes explicitFallbackRes: Int = 0
    ): Int {
        if (explicitFallbackRes != 0 && isDrawableResValid(context, explicitFallbackRes)) {
            return explicitFallbackRes
        }
        val inferred = resolveHeroFallbackDrawableRes(heroId, heroName)
        if (inferred != 0 && isDrawableResValid(context, inferred)) {
            return inferred
        }
        return R.drawable.app_logo
    }

    @DrawableRes
    fun lookupDrawableByName(context: Context, drawableName: String): Int {
        val cleanName = drawableName.trim().lowercase()
            .substringAfterLast('/')
            .substringBefore('.')
        if (cleanName.isEmpty() || !cleanName.matches(Regex("^[a-z0-9_]+$"))) {
            return 0
        }
        return try {
            context.resources.getIdentifier(cleanName, "drawable", context.packageName)
        } catch (_: Exception) {
            0
        }
    }

    fun isDrawableResValid(context: Context, @DrawableRes resId: Int): Boolean {
        if (resId == 0) return false
        return try {
            val typeName = context.resources.getResourceTypeName(resId)
            if (typeName != "drawable" && typeName != "mipmap") return false
            val typedValue = TypedValue()
            context.resources.getValue(resId, typedValue, true)
            typedValue.string != null && typedValue.string.isNotEmpty()
        } catch (_: Resources.NotFoundException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    internal class SafeHeroResourceInterceptor(
        private val context: Context
    ) : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val request = chain.request
            val heroId = request.parameters.value<String>(PARAM_HERO_ID).orEmpty()
            val heroName = request.parameters.value<String>(PARAM_HERO_NAME).orEmpty()
            val explicitFallback = request.parameters.value<Int>(PARAM_FALLBACK_RES) ?: 0
            val fallbackRes = resolveSafeFallbackRes(context, heroId, heroName, explicitFallback)

            val resolvedRequest = when (val data = request.data) {
                is Int -> {
                    if (isDrawableResValid(context, data)) {
                        request
                    } else {
                        Log.w(TAG, "Invalid or missing drawable resource ID ($data) for hero '$heroId'; using fallback $fallbackRes")
                        request.newBuilder().data(fallbackRes).build()
                    }
                }
                is String -> {
                    val trimmed = data.trim()
                    val isResourceRef = trimmed.startsWith("android.resource://") ||
                        (!trimmed.contains("://") && !trimmed.startsWith('/'))
                    if (!isResourceRef) {
                        request
                    } else {
                        val candidateId = lookupDrawableByName(context, trimmed)
                        if (candidateId != 0 && isDrawableResValid(context, candidateId)) {
                            request.newBuilder().data(candidateId).build()
                        } else {
                            val inferredHeroId = heroId.ifBlank { inferHeroIdFromDrawableName(trimmed) }
                            val stringFallback = resolveSafeFallbackRes(
                                context = context,
                                heroId = inferredHeroId,
                                heroName = heroName,
                                explicitFallbackRes = explicitFallback
                            )
                            Log.w(TAG, "Missing level-based drawable '$trimmed'; falling back to resource ID $stringFallback")
                            request.newBuilder().data(stringFallback).build()
                        }
                    }
                }
                else -> request
            }

            val result = chain.proceed(resolvedRequest)
            if (result is ErrorResult && resolvedRequest.data != fallbackRes) {
                Log.w(
                    TAG,
                    "Coil failed to decode image data '${resolvedRequest.data}' for hero '$heroId', retrying with fallback $fallbackRes",
                    result.throwable
                )
                return chain.proceed(
                    resolvedRequest.newBuilder()
                        .data(fallbackRes)
                        .build()
                )
            }
            return result
        }
    }
}
