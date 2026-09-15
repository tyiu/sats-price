package xyz.tyiu.satsprice.ui

internal data class CurrencyGridCellBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val centerX: Float get() = (left + right) / 2f
}

/**
 * Resolves a pointer position to one of the gaps around a row-major currency grid.
 * Gap zero is before the first item; gap [order.size] is after the last item.
 */
internal fun currencyDropGap(
    order: List<String>,
    boundsByCode: Map<String, CurrencyGridCellBounds>,
    pointerX: Float,
    pointerY: Float,
    isRtl: Boolean = false,
): Int {
    val measured = order.mapIndexedNotNull { index, code ->
        boundsByCode[code]?.let { bounds -> Triple(index, code, bounds) }
    }
    if (measured.isEmpty()) return order.size
    val lastBounds = boundsByCode[order.last()]
    val isInOddTrailingSlot = lastBounds != null && order.size % 2 == 1 && pointerY >= lastBounds.top &&
        if (isRtl) pointerX < lastBounds.left else pointerX > lastBounds.right
    if (isInOddTrailingSlot) {
        return order.size
    }

    val nearest = measured.minBy { (_, _, bounds) ->
        val dx = when {
            pointerX < bounds.left -> bounds.left - pointerX
            pointerX > bounds.right -> pointerX - bounds.right
            else -> 0f
        }
        val dy = when {
            pointerY < bounds.top -> bounds.top - pointerY
            pointerY > bounds.bottom -> pointerY - bounds.bottom
            else -> 0f
        }
        dx * dx + dy * dy
    }
    val isBefore = if (isRtl) pointerX > nearest.third.centerX else pointerX < nearest.third.centerX
    return nearest.first + if (isBefore) 0 else 1
}

/** Moves [code] to the requested gap, accounting for its removal shifting later gaps left. */
internal fun moveCurrencyToGap(order: List<String>, code: String, gap: Int): List<String> {
    val sourceIndex = order.indexOf(code)
    if (sourceIndex < 0 || order.distinct().size != order.size) return order

    val withoutSource = order.toMutableList().apply { removeAt(sourceIndex) }
    val insertionIndex = (gap.coerceIn(0, order.size) - if (sourceIndex < gap) 1 else 0)
        .coerceIn(0, withoutSource.size)
    withoutSource.add(insertionIndex, code)
    return withoutSource
}
