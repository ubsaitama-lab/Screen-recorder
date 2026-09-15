package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Apex Screen Record", appName)
  }

  @Test
  fun `hardware profiler detects configuration`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val profiler = com.example.engine.HardwareProfiler(context)
    val report = profiler.getHardwareReport()
    org.junit.Assert.assertNotNull(report)
  }

  @Test
  fun `main activity launches without crashing`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    org.junit.Assert.assertNotNull(controller.get())
  }
}
