package com.example.dice

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.random.Random

class DiceViewModel : ViewModel() {

    private val _diceValues = MutableLiveData<List<Int>>(listOf(6, 6, 6, 6, 6))
    val diceValues: LiveData<List<Int>> = _diceValues

    private val _isRolling = MutableLiveData<Boolean>(false)
    val isRolling: LiveData<Boolean> = _isRolling

    @Volatile
    private var isCancelled = false

    // Зберігаємо список усіх 5 активних потоків
    private val activeThreads = mutableListOf<Thread>()

    fun rollDice() {
        if (_isRolling.value == true) return

        _isRolling.value = true
        isCancelled = false
        activeThreads.clear()

        Log.d("DiceViewModel", "Запущено обертання 5 кісток у 5 окремих потоках")

        // Копіюємо поточні значення кубиків для синхронізованої зміни
        val currentValues = _diceValues.value?.toMutableList() ?: mutableListOf(6, 6, 6, 6, 6)

        // Потокобезпечний лічильник активних потоків
        val activeThreadsCount = AtomicInteger(5)

        // Частина 3: Запускаємо 5 ОКОРЕМИХ фонових потоків (по одному на кожен кубик)
        for (i in 0 until 5) {
            val t = thread {
                // Випадкова кількість ітерацій для кожного кубика (від 15 до 30 = 1.5 - 3.0 секунди)
                val iterations = Random.nextInt(15, 30)

                for (step in 0 until iterations) {
                    if (isCancelled || Thread.currentThread().isInterrupted) break

                    try {
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                        break
                    }

                    if (isCancelled) break

                    val randomValue = Random.nextInt(1, 7)

                    // Синхронізовано оновлюємо значення i-го кубика
                    synchronized(currentValues) {
                        currentValues[i] = randomValue
                        _diceValues.postValue(currentValues.toList())
                    }
                }

                // Коли потік завершує роботу — зменшуємо лічильник
                if (activeThreadsCount.decrementAndGet() == 0) {
                    _isRolling.postValue(false)
                    Log.d("DiceViewModel", "Усі 5 потоків завершили роботу. Кнопка ROLL розблокована!")
                }
            }

            activeThreads.add(t)
        }
    }

    override fun onCleared() {
        super.onCleared()
        isCancelled = true
        // Зупиняємо всі 5 потоків при закритті екрана (Захист від зомбі-потоків)
        for (t in activeThreads) {
            if (t.isAlive) {
                t.interrupt()
            }
        }
        activeThreads.clear()
        Log.d("DiceViewModel", "onCleared() викликано: фонові потоки зупинено")
    }
}