package com.example.util

import com.example.data.local.entity.WargaEntity
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.util.UUID

object ExcelParser {

    fun parseWargaExcel(inputStream: InputStream): List<WargaEntity> {
        val wargaList = mutableListOf<WargaEntity>()
        try {
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0) ?: return emptyList()

            // Skip header row at index 0
            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                
                // Expecting Columns: Index 0 (No), Index 1 (Nama Warga), Index 2 (RT), Index 3 (No Rumah)
                val cellNama = row.getCell(1) ?: continue
                val cellRt = row.getCell(2)
                val cellNoRumah = row.getCell(3)

                val nama = getCellStringValue(cellNama).trim()
                if (nama.isBlank()) continue

                val rtVal = getCellStringValue(cellRt).trim()
                val noRumahVal = getCellStringValue(cellNoRumah).trim()

                // Format RT to 2 digits if numeric (e.g. 3 -> "03")
                val formattedRt = if (rtVal.length == 1 && rtVal[0].isDigit()) "0$rtVal" else rtVal

                wargaList.add(
                    WargaEntity(
                        id = UUID.randomUUID().toString(),
                        qrUuid = "", // to be populated sequentially in repository
                        nama = nama,
                        rt = formattedRt,
                        rw = "01", // Default RW 01
                        nomorRumah = noRumahVal,
                        alamat = "Jl. Jimpitan No. $noRumahVal",
                        isActive = true,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            workbook.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return wargaList
    }

    private fun getCellStringValue(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellTypeEnum) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                val num = cell.numericCellValue
                if (num == num.toInt().toDouble()) {
                    num.toInt().toString()
                } else {
                    num.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    cell.stringCellValue
                } catch (e: Exception) {
                    try {
                        cell.numericCellValue.toString()
                    } catch (e2: Exception) {
                        ""
                    }
                }
            }
            else -> ""
        }
    }
}
