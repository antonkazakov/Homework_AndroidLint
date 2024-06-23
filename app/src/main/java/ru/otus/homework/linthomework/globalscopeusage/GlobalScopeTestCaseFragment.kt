@file:OptIn(DelicateCoroutinesApi::class)

package ru.otus.homework.linthomework.globalscopeusage

import androidx.fragment.app.Fragment
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GlobalScopeTestCaseFragment : Fragment() {
    init {
        GlobalScope.launch {
        }
    }
}
