package com.example.drivingcoach

import android.graphics.Bitmap
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test

class TrainingFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun coldLaunchAndBothScenariosShowCalculatedResults() {
        compose.mainClock.autoAdvance = false
        click("연수 시작")
        finishReplay()
        show("실험용 감속 점수 100점")
        saveScreenshot("smooth-result")
        click("홈으로")
        click("시나리오 · 부드러운 감속")
        click("연수 시작")
        finishReplay()
        show("실험용 감속 점수 25점")
        saveScreenshot("abrupt-result")
    }

    @Test
    fun restrictionHidesControlsAndResultsWithoutResettingSession() {
        compose.mainClock.autoAdvance = false
        click("연수 시작")
        click("데모 · 주행 제한 재현")
        show("주행 중 이용 제한")
        compose.onNodeWithText("현재 속도").assertDoesNotExist()
        compose.onNodeWithText("연수 종료 및 결과 보기").assertDoesNotExist()
        saveScreenshot("restricted-session")
        compose.mainClock.advanceTimeBy(12_000)
        click("데모 · 주차 상태로 전환")
        click("연수 종료 및 결과 보기")
        show("실험용 감속 점수 100점")
        click("데모 · 주행 제한 재현")
        compose.onNodeWithText("실험용 감속 점수 100점").assertDoesNotExist()
        show("주행 중 이용 제한")
        click("데모 · 주차 상태로 전환")
        show("실험용 감속 점수 100점")
    }

    private fun finishReplay() {
        compose.mainClock.advanceTimeBy(12_000)
        click("연수 종료 및 결과 보기")
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private fun show(text: String) {
        compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }

    private fun saveScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-evidence")
        check(directory.mkdirs() || directory.isDirectory)
        val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use {
            check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        screenshot.recycle()
    }
}
