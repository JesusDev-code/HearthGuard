package com.example.tfg_apli.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeToAlertButton(
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val width = 300.dp
    val height = 60.dp

    // El estado swipeable funciona igual en Material que en Wear
    val swipeableState = rememberSwipeableState(0)
    val sizePx = with(LocalDensity.current) { (width - height).toPx() }

    // Puntos de anclaje: 0 (inicio) y sizePx (final)
    val anchors = mapOf(0f to 0, sizePx to 1)

    // Detectar cuando llega al final
    LaunchedEffect(swipeableState.currentValue) {
        if (swipeableState.currentValue == 1) {
            onSlideComplete()

            swipeableState.snapTo(0)
        }
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .background(Color(0xFFFFCDD2), RoundedCornerShape(height))
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.8f) },
                orientation = Orientation.Horizontal
            )
    ) {
        // Texto de fondo
        Text(
            "DESLIZA PARA SOS",
            color = Color.Red,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleMedium
        )

        // Botón deslizante
        Box(
            modifier = Modifier
                .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
                .size(height)
                .background(Color.Red, CircleShape),
            contentAlignment = Alignment.Center
        ) {

        }
    }
}