package ru.otus.homework.linthomework.jobinbuilderusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class JobInBuilderTestCase(
    private val job: Job
) : ViewModel() {

    fun case1() {
        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
            delay(1000)
            println("Hello World")
        }
    }

    fun case2() {
        viewModelScope.launch {
            delay(1000)
            println("Hello World")
        }
    }

    fun case3() {
        viewModelScope.launch(job) {
            delay(1000)
            println("Hello World")
        }
    }

    fun case4() {
        viewModelScope.launch(NonCancellable) {
            delay(1000)
            println("Hello World")
        }
    }
}