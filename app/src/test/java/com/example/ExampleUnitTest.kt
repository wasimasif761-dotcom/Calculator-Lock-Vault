package com.example

import com.example.ui.calculator.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCalculatorBasicOperations() {
        assertEquals("4", CalculatorEngine.evaluate("2+2"))
        assertEquals("10", CalculatorEngine.evaluate("5×2"))
        assertEquals("3", CalculatorEngine.evaluate("9÷3"))
        assertEquals("12", CalculatorEngine.evaluate("20-8"))
    }

    @Test
    fun testCalculatorOrderOfOperationsAndParentheses() {
        assertEquals("14", CalculatorEngine.evaluate("2+3×4"))
        assertEquals("20", CalculatorEngine.evaluate("(2+3)×4"))
    }

    @Test
    fun testCalculatorPercentage() {
        assertEquals("5", CalculatorEngine.evaluate("500%"))
        assertEquals("25", CalculatorEngine.evaluate("50×50%"))
    }
}
