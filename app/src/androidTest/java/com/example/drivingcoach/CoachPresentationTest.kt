package com.example.drivingcoach

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.example.drivingcoach.ui.DrivingCoachApp
import com.example.drivingcoach.vehicle.VehicleUxState
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CoachPresentationTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int) = context.getString(id)

    @Before
    fun resetPresentationPreferences() {
        context.getSharedPreferences("coach_presentation", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun actualRestrictionsRemoveAnAlreadyOpenHistoryScreen() {
        val vehicle = mutableStateOf(VehicleUxState.AVAILABLE)
        compose.setContent { DrivingCoachApp(vehicle.value, foreground = true) }
        compose.onNodeWithText(text(R.string.tab_history)).performClick()
        compose.onNodeWithText(text(R.string.empty_history_title)).assertIsDisplayed()
        compose.runOnIdle { vehicle.value = VehicleUxState.RESTRICTED }
        compose.onNodeWithTag("restricted-screen").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.empty_history_title)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.tab_history)).assertIsNotEnabled()
        compose.onNodeWithTag("exit-driving-preview").assertDoesNotExist()
        compose.runOnIdle { vehicle.value = VehicleUxState.AVAILABLE }
        compose.onNodeWithText(text(R.string.tab_coach)).assertIsSelected()
    }

    @Test
    fun missingCarStateNeverShowsSampleDataOrUnlockedControls() {
        compose.setContent { DrivingCoachApp(VehicleUxState.UNAVAILABLE, foreground = true) }
        compose.onNodeWithTag("restricted-screen").assertIsDisplayed()
        compose.onNodeWithTag("sample-data-label").assertDoesNotExist()
        compose.onNodeWithText(text(R.string.tab_settings)).assertIsNotEnabled()
        compose.onNodeWithTag("coach-preview").assertDoesNotExist()
    }

    @Test
    fun previewFocusAndCoachingSessionRespondToInput() {
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, foreground = true) }
        compose.onNodeWithText(text(R.string.focus_smooth)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.focus_smooth)).assertIsSelected()
        compose.onNodeWithText(text(R.string.smooth_title)).assertIsDisplayed()
        compose.onNodeWithTag("coach-preview").performScrollTo().performClick()
        compose.onNodeWithTag("preview-active").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("coach-preview").performScrollTo().performClick()
        compose.onNodeWithTag("preview-active").assertDoesNotExist()
    }

    @Test
    fun sampleTripOpensDetailsAndReturnsToList() {
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, foreground = true) }
        compose.onNodeWithText(text(R.string.tab_history)).performClick()
        compose.onNodeWithText(text(R.string.trip_city)).performClick()
        compose.onNodeWithTag("trip-detail").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.back_to_history)).performClick()
        compose.onNodeWithTag("history-list").assertIsDisplayed()
    }

    @Test
    fun onlyPhonePreviewOffersAnExitFromSimulatedDrivingRestrictions() {
        compose.setContent { DrivingCoachApp(VehicleUxState.PREVIEW, foreground = true) }
        compose.onNodeWithText(text(R.string.tab_settings)).performClick()
        compose.onNodeWithTag("preview-driving").performScrollTo().performClick()
        compose.onNodeWithTag("restricted-screen").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.tab_settings)).assertIsNotEnabled()
        compose.onNodeWithTag("exit-driving-preview").performScrollTo().performClick()
        compose.onNodeWithTag("restricted-screen").assertDoesNotExist()
    }
}
