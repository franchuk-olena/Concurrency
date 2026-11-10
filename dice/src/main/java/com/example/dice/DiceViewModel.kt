package com.example.dice

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class DiceViewModel : ViewModel() {

    private val _diceValues = MutableLiveData<List<Int>>(listOf(6, 6, 6, 6, 6))
    val diceValues: LiveData<List<Int>> = _diceValues

    private val _isRolling = MutableLiveData<Boolean>(false)
    val isRolling: LiveData<Boolean> = _isRolling

    // Стан для тексту кнопки ("ROLL" або "RESTART")
    private val _buttonText = MutableLiveData<String>("ROLL")
    val buttonText: LiveData<String> = _buttonText

    // Зберігаємо поточний Job для можливості скасування при перезапуску
    private var rollJob: Job? = null

    fun rollDice() {
        // Якщо обертання вже йде — скасовуємо поточну корутину (RESTART)
        if (rollJob?.isActive == true) {
            rollJob?.cancel()
            Log.d("DiceViewModel", "Попереднє обертання скасовано (RESTART)")
        }

        // Запускаємо нову корутину у viewModelScope
        rollJob = viewModelScope.launch {
            _isRolling.value = true
            _buttonText.value = "RESTART"
            Log.d("DiceViewModel", "Запущено паралельне обертання 5 кісток через async")

            try {
                // Паралельний запуск 5 корутин через async та очікування awaitAll
                coroutineScope {
                    val deferreds = (0 until 5).map { index ->
                        async {
                            rollSingleDie(index)
                        }
                    }
                    deferreds.awaitAll()
                }
            } finally {
                // Скидаємо стан тільки після завершення всіх корутин (якщо Job не скасовано)
                if (rollJob?.isCancelled != true) {
                    _isRolling.value = false
                    _buttonText.value = "ROLL"
                    Log.d("DiceViewModel", "Усі 5 корутин завершили роботу. Стан скинуто на ROLL")
                }
            }
        }
    }

    // Suspend функція обертання однієї кістки
    private suspend fun rollSingleDie(index: Int) {
        val iterations = Random.nextInt(15, 30) // 1.5 - 3.0 секунди

        for (step in 0 until iterations) {
            delay(100) // Неблокуюча призупиняюча затримка замість Thread.sleep

            val randomValue = Random.nextInt(1, 7)

            // Оновлюємо значення конкретного кубика в LiveData
            val currentList = _diceValues.value?.toMutableList() ?: mutableListOf(6, 6, 6, 6, 6)
            currentList[index] = randomValue
            _diceValues.value = currentList
        }
    }

    // onCleared більше не потребує ручного зупинення потоків,
    // оскільки viewModelScope скасовує всі корутини автоматично.
}