package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.manager.AssistantAvailabilityHelper
import com.example.model.AssistantAvailabilityMode
import com.example.service.AssistantServiceBridge
import com.example.service.ServiceUserAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VirJoy Assistant", appName)
  }

  @Test
  fun `test availability helper active mode is always active`() {
    assertTrue(AssistantAvailabilityHelper.isListeningAllowed(AssistantAvailabilityMode.ACTIVE, 8, 0, 22, 0))
  }

  @Test
  fun `test availability helper sleep mode is never active`() {
    assertFalse(AssistantAvailabilityHelper.isListeningAllowed(AssistantAvailabilityMode.SLEEP, 8, 0, 22, 0))
  }

  @Test
  fun `test availability helper 24 hours mode is always active`() {
    assertTrue(AssistantAvailabilityHelper.isListeningAllowed(AssistantAvailabilityMode.HOURS_24, 8, 0, 22, 0))
  }

  @Test
  fun `test availability helper schedule day window`() {
    val calInside = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 14)
      set(Calendar.MINUTE, 30)
    }
    assertTrue(AssistantAvailabilityHelper.isScheduledTimeActive(8, 0, 22, 0, calInside))

    val calBefore = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 7)
      set(Calendar.MINUTE, 59)
    }
    assertFalse(AssistantAvailabilityHelper.isScheduledTimeActive(8, 0, 22, 0, calBefore))

    val calAfter = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 22)
      set(Calendar.MINUTE, 1)
    }
    assertFalse(AssistantAvailabilityHelper.isScheduledTimeActive(8, 0, 22, 0, calAfter))
  }

  @Test
  fun `test availability helper schedule overnight window`() {
    // 22:00 to 06:00
    val calLateNight = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 15)
    }
    assertTrue(AssistantAvailabilityHelper.isScheduledTimeActive(22, 0, 6, 0, calLateNight))

    val calEarlyMorning = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 3)
      set(Calendar.MINUTE, 0)
    }
    assertTrue(AssistantAvailabilityHelper.isScheduledTimeActive(22, 0, 6, 0, calEarlyMorning))

    val calMidday = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 12)
      set(Calendar.MINUTE, 0)
    }
    assertFalse(AssistantAvailabilityHelper.isScheduledTimeActive(22, 0, 6, 0, calMidday))
  }

  @Test
  fun `test service bridge state post and retrieve`() {
    AssistantServiceBridge.updateState {
      it.copy(isServiceRunning = true, isListening = true, assistantName = "VirJoy Test")
    }
    assertEquals(true, AssistantServiceBridge.serviceState.value.isServiceRunning)
    assertEquals(true, AssistantServiceBridge.serviceState.value.isListening)
    assertEquals("VirJoy Test", AssistantServiceBridge.serviceState.value.assistantName)

    var receivedAction: ServiceUserAction? = null
    val job = AssistantServiceBridge.userActions
    AssistantServiceBridge.postAction(ServiceUserAction.ReloadSettings)
    assertEquals(true, AssistantServiceBridge.serviceState.value.isServiceRunning)
  }
}

