package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.image.HeroImageLoader
import com.example.model.resolveHeroFallbackDrawableRes

/**
 * Renders a hero portrait through Coil's configured [HeroImageLoader], ensuring
 * invalid resource IDs or missing level-based files fall back gracefully without
 * triggering IllegalArgumentException / NotFoundException.
 */
@Composable
fun HeroPortraitImage(
    @DrawableRes imageRes: Int,
    heroId: String = "",
    heroName: String = "",
    heroLevel: Int = 1,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.TopCenter
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { HeroImageLoader.getOrCreate(context) }
    val safeFallbackRes = remember(context, heroId, heroName) {
        HeroImageLoader.resolveSafeFallbackRes(
            context = context,
            heroId = heroId,
            heroName = heroName,
            explicitFallbackRes = resolveHeroFallbackDrawableRes(heroId, heroName)
        )
    }
    val modelRequest = remember(context, imageRes, heroId, heroName, heroLevel, safeFallbackRes) {
        HeroImageLoader.buildHeroImageRequest(
            context = context,
            data = if (imageRes != 0) imageRes else safeFallbackRes,
            heroId = heroId,
            heroName = heroName,
            heroLevel = heroLevel,
            fallbackRes = safeFallbackRes
        )
    }
    val fallbackPainter = painterResource(id = safeFallbackRes)

    AsyncImage(
        model = modelRequest,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        placeholder = fallbackPainter,
        error = fallbackPainter,
        fallback = fallbackPainter,
        contentScale = contentScale,
        alignment = alignment,
        modifier = modifier
    )
}
