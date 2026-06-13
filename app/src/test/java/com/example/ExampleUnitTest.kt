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

  @Test
  fun testExcelParser() {
    val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
    val sheet = workbook.createSheet("Sheet 1")
    val headerRow = sheet.createRow(0)
    headerRow.createCell(0).setCellValue("No")
    headerRow.createCell(1).setCellValue("Nama Warga")
    headerRow.createCell(2).setCellValue("RT")
    headerRow.createCell(3).setCellValue("No Rumah")

    val dataRow = sheet.createRow(1)
    dataRow.createCell(0).setCellValue(1.0)
    dataRow.createCell(1).setCellValue("Ahmad")
    dataRow.createCell(2).setCellValue(3.0)
    dataRow.createCell(3).setCellValue("012")

    val outputStream = java.io.ByteArrayOutputStream()
    workbook.write(outputStream)
    workbook.close()

    val inputStream = java.io.ByteArrayInputStream(outputStream.toByteArray())
    val parsedList = com.example.util.ExcelParser.parseWargaExcel(inputStream)

    assertEquals(1, parsedList.size)
    val warga = parsedList[0]
    assertEquals("Ahmad", warga.nama)
    assertEquals("03", warga.rt) // formatted to 2 digits
    assertEquals("01", warga.rw) // default rw
    assertEquals("012", warga.nomorRumah)
    assertEquals("Jl. Jimpitan No. 012", warga.alamat)
    assertTrue(warga.isActive)
  }
}
