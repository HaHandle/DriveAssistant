package com.example.drivingcoach

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.drivingcoach.ui.DrivingCoachApp
import com.example.drivingcoach.ui.TrainingResultDisplay
import com.example.drivingcoach.ui.TrainingUiState
import com.example.drivingcoach.vehicle.VehicleUxState
import org.junit.Rule
import org.junit.Test

class SpeedTrainingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun demoResultIsReachableFromCurrentCoachHome() {
        val training = mutableStateOf(TrainingUiState())
        compose.setContent {
            DrivingCoachApp(
                vehicleUxState = VehicleUxState.PREVIEW,
                foreground = true,
                trainingState = training.value,
                onStartTraining = {
                    training.value = training.value.copy(result = TrainingResultDisplay(
                        feedback = "반복 정차 관찰", score = 75, strongEvents = 1,
                        peakDeceleration = "4.0 m/s²", stopSummary = "30–60 km/h: 3회",
                    ))
                },
            )
        }
        compose.onNodeWithTag("open-speed-training").performScrollTo().performClick()
        compose.onNodeWithText("반복 정차 데모").performClick()
        compose.onNodeWithText("강한 감속 후보 1 회").assertIsDisplayed()
        compose.onNodeWithText("30–60 km/h: 3회").assertIsDisplayed()
    }

    @Test
    fun restrictionHidesOpenTrainingResult() {
        val vehicle = mutableStateOf(VehicleUxState.AVAILABLE)
        val result = TrainingResultDisplay("관찰", null, 1, "4.0 m/s²", "3회")
        compose.setContent {
            DrivingCoachApp(
                vehicleUxState = vehicle.value,
                foreground = true,
                trainingState = TrainingUiState(automotive = true, result = result),
            )
        }
        compose.onNodeWithTag("open-speed-training").performScrollTo().performClick()
        compose.onNodeWithText("강한 감속 후보 1 회").assertIsDisplayed()
        compose.runOnIdle { vehicle.value = VehicleUxState.RESTRICTED }
        compose.onNodeWithTag("restricted-screen").assertIsDisplayed()
        compose.onNodeWithText("강한 감속 후보 1 회").assertDoesNotExist()
    }
}
