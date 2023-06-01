package ru.otus.homework.linttests

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import java.io.File
import kotlin.system.exitProcess

abstract class AndroidSdkLintDetectorTest: LintDetectorTest() {

    private fun getSdkHome(): File {
        val androidHome = "D:\\AndroidSDK"//System.getenv("ANDROID_HOME")
        if(androidHome.isBlank()) {
            System.err.println("ANDROID_HOME must be available wherever you are executing these tests. " +
                    "For example, when running Android Studio in Linux, ANDROID_HOME should be set in /etc/environment, " +
                    "because if it is only set in .bashrc, Android Studio will not have access.")
            exitProcess(1)
        }
        val file = File(androidHome)
        if(!file.exists()) {
            System.err.println("$androidHome does not exist")
            exitProcess(1)
        }
        return file
    }

    override fun lint(): TestLintTask {
        return super.lint().sdkHome(getSdkHome())
    }

}