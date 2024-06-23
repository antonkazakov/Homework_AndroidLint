package ru.otus.homework.linthomework.jobinbuilderusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JobInBuilderTestCase(
    private val job: Job
) : ViewModel() {

    private val otherScope = CoroutineScope(Dispatchers.Main)

    fun case1() {
        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
            delay(1000)
            println("Hello World")
        }

        viewModelScope.launch(SupervisorJob()) {
            delay(1000)
            println("Hello World")
        }

        otherScope.launch(SupervisorJob()) {
            delay(1000)
            println("Hello World")
        }
    }

    fun case2() {
        viewModelScope.launch(Job()) {
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
        viewModelScope.launch(NonCancellable + Dispatchers.IO) {
            delay(1000)
            println("Hello World")
        }
    }
}

class NotVMJobInBuilderTestCase() {
    private val otherScope = CoroutineScope(Dispatchers.Main)

    init {
        otherScope.launch(Job()) {
        }
    }
}
