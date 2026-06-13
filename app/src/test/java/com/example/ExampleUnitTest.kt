package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testFifoCoverageCalculation() {
    val nominal = 2000
    val coverageDays = nominal / 500
    assertEquals(4, coverageDays)

    val existingCoverages = listOf("2026-06-10")
    val today = "2026-06-14"
    val startDateStr = existingCoverages.firstOrNull() ?: today

    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val cal = java.util.Calendar.getInstance()
    cal.time = sdf.parse(startDateStr)!!
    
    if (existingCoverages.isNotEmpty()) {
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
    }

    val results = mutableListOf<String>()
    for (i in 0 until coverageDays) {
        results.add(sdf.format(cal.time))
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
    }

    assertEquals(listOf("2026-06-11", "2026-06-12", "2026-06-13", "2026-06-14"), results)
  }
}
