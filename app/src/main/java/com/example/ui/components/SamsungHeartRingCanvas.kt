package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Delegator for Samsung Health 3 Concentric Activity Rings
 */
@Composable
fun SamsungHeartRingCanvas(
    stepsProgress: Float,
    activeMinutesProgress: Float,
    caloriesProgress: Float,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 96.dp
) {
    ActivityRingsCanvas(
        stepsProgress = stepsProgress,
        activeMinutesProgress = activeMinutesProgress,
        caloriesProgress = caloriesProgress,
        modifier = modifier,
        sizeDp = sizeDp
    )
}
