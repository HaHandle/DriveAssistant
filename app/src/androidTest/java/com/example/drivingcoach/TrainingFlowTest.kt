package com.example.drivingcoach

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
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

    @Test
    fun repeatedStopScenarioShowsExpectedObservation() {
        compose.mainClock.autoAdvance = false
        click("시나리오 · 부드러운 감속")
        click("시나리오 · 급한 감속")
        click("연수 시작")
        finishReplay()

        show("정차 제어 관찰")
        show("접근 속도 30–60 km/h")
        show("강한 감속 후보 미감지 2/3회 (67%)")
        saveScreenshot("repeated-stop-result")
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
        val automation = instrumentation.uiAutomation
        val directory = "/data/local/tmp/driveassistant-ui"
        val commands = listOf("mkdir -p $directory", "screencap -p $directory/$name.png")
        for (command in commands) {
            val output = automation.executeShellCommand(command)
            ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
        }
        val output = automation.executeShellCommand("stat -c %s $directory/$name.png")
        val size = ParcelFileDescriptor.AutoCloseInputStream(output).bufferedReader().use {
            it.readText().trim().toLongOrNull()
        }
        check(size != null && size > 0) { "Screenshot was not saved" }
    }
}
