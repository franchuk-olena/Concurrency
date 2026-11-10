package com.example.dice



import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.random.Random

class DiceModel {

    // Симуляція обертання одного кубика
    suspend fun rollSingleDie(
        onValueChange: (Int) -> Unit
    ): Int {
        // Випадкова тривалість обертання від 1500 мс до 3000 мс (1.5 - 3 сек)
        val durationMs = Random.nextLong(1500, 3000)
        val steps = (durationMs / 100).toInt()

        repeat(steps) {
            val tempValue = Random.nextInt(1, 7)
            onValueChange(tempValue)
            delay(100) // Неблокуюча затримка
        }

        // Фінальне значення
        val finalValue = Random.nextInt(1, 7)
        onValueChange(finalValue)
        return finalValue
    }

    // Паралельний запуск обертання 5 кісток
    suspend fun rollAllDice(
        onDieUpdated: (index: Int, value: Int) -> Unit
    ): List<Int> = coroutineScope {
        // Створюємо 5 паралельних корутин за допомогою async
        val deferredDice = (0 until 5).map { index ->
            async {
                rollSingleDie { newValue ->
                    onDieUpdated(index, newValue)
                }
            }
        }
        // Чекаємо завершення всіх 5 корутин
        deferredDice.awaitAll()
    }
}