package app.zangdale.kanjian.ui.utils


fun clamp(value: Float, min: Float, max: Float): Float {
    return value
        .coerceAtLeast(min)
        .coerceAtMost(max)
}
