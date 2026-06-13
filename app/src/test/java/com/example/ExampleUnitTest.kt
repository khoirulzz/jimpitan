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
    val sharedStringsXml = """
      <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="5" uniqueCount="5">
          <si><t>No</t></si>
          <si><t>Nama Warga</t></si>
          <si><t>RT</t></si>
          <si><t>No Rumah</t></si>
          <si><t>Ahmad</t></si>
      </sst>
    """.trimIndent()

    val sheetXml = """
      <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <sheetData>
              <row r="1">
                  <c r="A1" t="s"><v>0</v></c>
                  <c r="B1" t="s"><v>1</v></c>
                  <c r="C1" t="s"><v>2</v></c>
                  <c r="D1" t="s"><v>3</v></c>
              </row>
              <row r="2">
                  <c r="A2"><v>1</v></c>
                  <c r="B2" t="s"><v>4</v></c>
                  <c r="C2"><v>3</v></c>
                  <c r="D2"><v>012</v></c>
              </row>
          </sheetData>
      </worksheet>
    """.trimIndent()

    val bos = java.io.ByteArrayOutputStream()
    val zos = java.util.zip.ZipOutputStream(bos)
    
    zos.putNextEntry(java.util.zip.ZipEntry("xl/sharedStrings.xml"))
    zos.write(sharedStringsXml.toByteArray())
    zos.closeEntry()

    zos.putNextEntry(java.util.zip.ZipEntry("xl/worksheets/sheet1.xml"))
    zos.write(sheetXml.toByteArray())
    zos.closeEntry()

    zos.close()

    val inputStream = java.io.ByteArrayInputStream(bos.toByteArray())
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
