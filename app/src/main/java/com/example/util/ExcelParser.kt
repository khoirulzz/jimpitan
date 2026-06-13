package com.example.util

import com.example.data.local.entity.WargaEntity
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

object ExcelParser {

    fun parseWargaExcel(inputStream: InputStream): List<WargaEntity> {
        val wargaList = mutableListOf<WargaEntity>()
        try {
            var sharedStrings = listOf<String>()
            var sheetXmlBytes: ByteArray? = null
            var sharedStringsXmlBytes: ByteArray? = null

            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    sharedStringsXmlBytes = zip.readBytes()
                } else if (entry.name == "xl/worksheets/sheet1.xml") {
                    sheetXmlBytes = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            zip.close()

            if (sheetXmlBytes == null) return emptyList()

            // Parse sharedStrings
            if (sharedStringsXmlBytes != null) {
                sharedStrings = parseSharedStrings(sharedStringsXmlBytes.inputStream())
            }

            // Parse sheet1
            wargaList.addAll(parseSheetXml(sheetXmlBytes.inputStream(), sharedStrings))

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return wargaList
    }

    private fun parseSharedStrings(inputStream: InputStream): List<String> {
        val list = mutableListOf<String>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(inputStream)
            val siList = doc.getElementsByTagName("si")
            for (i in 0 until siList.length) {
                val node = siList.item(i)
                list.add(node.textContent ?: "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseSheetXml(inputStream: InputStream, sharedStrings: List<String>): List<WargaEntity> {
        val list = mutableListOf<WargaEntity>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(inputStream)
            
            val rowList = doc.getElementsByTagName("row")
            for (i in 0 until rowList.length) {
                val rowNode = rowList.item(i) as? Element ?: continue
                val rowNumStr = rowNode.getAttribute("r")
                val rowNum = rowNumStr.toIntOrNull() ?: (i + 1)
                
                // Skip header row at index 0 (which is row number 1 in Excel)
                if (rowNum == 1) continue
                
                val cList = rowNode.getElementsByTagName("c")
                val cellValues = mutableMapOf<Int, String>() // colIndex -> value
                
                for (j in 0 until cList.length) {
                    val cNode = cList.item(j) as? Element ?: continue
                    val ref = cNode.getAttribute("r") // e.g. "B2"
                    val colLetter = ref.filter { it.isLetter() }
                    val colIndex = colLetterToNumber(colLetter)
                    
                    val type = cNode.getAttribute("t")
                    val vNode = cNode.getElementsByTagName("v").item(0)
                    val rawVal = vNode?.textContent ?: ""
                    
                    val finalVal = if (type == "s" && rawVal.isNotEmpty()) {
                        val strIdx = rawVal.toIntOrNull()
                        if (strIdx != null && strIdx >= 0 && strIdx < sharedStrings.size) {
                            sharedStrings[strIdx]
                        } else {
                            ""
                        }
                    } else {
                        if (rawVal.endsWith(".0")) {
                            rawVal.substring(0, rawVal.length - 2)
                        } else {
                            rawVal
                        }
                    }
                    
                    cellValues[colIndex] = finalVal
                }
                
                // Columns mapping: Index 0 (No), Index 1 (Nama Warga), Index 2 (RT), Index 3 (No Rumah)
                val nama = cellValues[1]?.trim() ?: ""
                if (nama.isBlank()) continue
                
                val rtVal = cellValues[2]?.trim() ?: ""
                val noRumahVal = cellValues[3]?.trim() ?: ""
                
                val formattedRt = if (rtVal.length == 1 && rtVal[0].isDigit()) "0$rtVal" else rtVal
                
                list.add(
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun colLetterToNumber(colLetter: String): Int {
        var result = 0
        for (i in 0 until colLetter.length) {
            result *= 26
            result += colLetter[i].uppercaseChar() - 'A' + 1
        }
        return result - 1
    }
}
