package com.example.ui.calculator

import java.text.DecimalFormat

object CalculatorEngine {

    private val df = DecimalFormat("#.########")

    fun evaluate(expression: String): String {
        return try {
            val sanitized = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("%", "*0.01")
            
            if (sanitized.isBlank()) return "0"
            val parser = MathParser(sanitized)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) {
                "Error"
            } else {
                df.format(result)
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    private class MathParser(private val str: String) {
        private var pos = -1
        private var ch = -1

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) return Double.NaN
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        x = if (divisor != 0.0) x / divisor else Double.NaN
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDoubleOrNull() ?: Double.NaN
            } else {
                x = Double.NaN
            }

            return x
        }
    }
}
