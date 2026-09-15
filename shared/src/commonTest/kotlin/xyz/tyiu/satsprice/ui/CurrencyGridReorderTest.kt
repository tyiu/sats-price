package xyz.tyiu.satsprice.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyGridReorderTest {
    private val order = listOf("USD", "EUR", "JPY", "GBP", "CAD")
    private val bounds = mapOf(
        "USD" to CurrencyGridCellBounds(0f, 0f, 100f, 60f),
        "EUR" to CurrencyGridCellBounds(110f, 0f, 210f, 60f),
        "JPY" to CurrencyGridCellBounds(0f, 70f, 100f, 130f),
        "GBP" to CurrencyGridCellBounds(110f, 70f, 210f, 130f),
        "CAD" to CurrencyGridCellBounds(0f, 140f, 100f, 200f),
    )

    @Test
    fun dropGapSupportsEveryRowMajorInsertionPoint() {
        assertEquals(0, currencyDropGap(order, bounds, 10f, -1f))
        assertEquals(1, currencyDropGap(order, bounds, 90f, -1f))
        assertEquals(0, currencyDropGap(order, bounds, 10f, 30f))
        assertEquals(1, currencyDropGap(order, bounds, 90f, 30f))
        assertEquals(1, currencyDropGap(order, bounds, 120f, 30f))
        assertEquals(2, currencyDropGap(order, bounds, 200f, 30f))
        assertEquals(2, currencyDropGap(order, bounds, 10f, 100f))
        assertEquals(3, currencyDropGap(order, bounds, 90f, 100f))
        assertEquals(4, currencyDropGap(order, bounds, 200f, 100f))
        assertEquals(4, currencyDropGap(order, bounds, 10f, 170f))
        assertEquals(5, currencyDropGap(order, bounds, 90f, 170f))
        assertEquals(5, currencyDropGap(order, bounds, 180f, 170f))
        assertEquals(4, currencyDropGap(order, bounds, 10f, 220f))
    }

    @Test
    fun dropGapMirrorsInsertionSidesAndOddTrailingSlotInRtl() {
        val rtlBounds = mapOf(
            "USD" to CurrencyGridCellBounds(110f, 0f, 210f, 60f),
            "EUR" to CurrencyGridCellBounds(0f, 0f, 100f, 60f),
            "JPY" to CurrencyGridCellBounds(110f, 70f, 210f, 130f),
            "GBP" to CurrencyGridCellBounds(0f, 70f, 100f, 130f),
            "CAD" to CurrencyGridCellBounds(110f, 140f, 210f, 200f),
        )
        assertEquals(0, currencyDropGap(order, rtlBounds, 200f, 30f, isRtl = true))
        assertEquals(1, currencyDropGap(order, rtlBounds, 120f, 30f, isRtl = true))
        assertEquals(1, currencyDropGap(order, rtlBounds, 90f, 30f, isRtl = true))
        assertEquals(5, currencyDropGap(order, rtlBounds, 20f, 170f, isRtl = true))
    }

    @Test
    fun moveToGapHandlesBothDirectionsAndNoOps() {
        assertEquals(listOf("EUR", "JPY", "GBP", "CAD", "USD"), moveCurrencyToGap(order, "USD", 5))
        assertEquals(listOf("CAD", "USD", "EUR", "JPY", "GBP"), moveCurrencyToGap(order, "CAD", 0))
        assertEquals(order, moveCurrencyToGap(order, "JPY", 2))
        assertEquals(order, moveCurrencyToGap(order, "JPY", 3))
    }

    @Test
    fun invalidSourceOrDuplicateOrderIsRejected() {
        assertEquals(order, moveCurrencyToGap(order, "AUD", 2))
        val duplicateOrder = listOf("USD", "EUR", "USD")
        assertEquals(duplicateOrder, moveCurrencyToGap(duplicateOrder, "USD", 1))
    }

    @Test
    fun moveClampsOutOfRangeGaps() {
        assertEquals(listOf("CAD", "USD", "EUR", "JPY", "GBP"), moveCurrencyToGap(order, "CAD", -3))
        assertEquals(listOf("EUR", "JPY", "GBP", "CAD", "USD"), moveCurrencyToGap(order, "USD", 99))
    }

    @Test
    fun partialMeasurementsStillReturnAValidGap() {
        val partial = bounds - "CAD" - "GBP"
        assertEquals(3, currencyDropGap(order, partial, 180f, 170f))
    }

    @Test
    fun evenAndEmptyMeasurementsResolveSafely() {
        val evenOrder = order.dropLast(1)
        assertEquals(4, currencyDropGap(evenOrder, bounds - "CAD", 180f, 170f))
        assertEquals(order.size, currencyDropGap(order, emptyMap(), 10f, 10f))
    }
}
