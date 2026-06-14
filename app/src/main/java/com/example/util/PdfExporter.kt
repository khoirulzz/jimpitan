package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.entity.PembayaranEntity
import com.example.data.local.entity.WargaEntity
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    /**
     * Generate laporan pembayaran sebagai PDF dan simpan ke Downloads/Jimpitan/.
     * Returns true jika berhasil.
     */
    fun exportLaporanPdf(
        context: Context,
        pembayaranList: List<PembayaranEntity>,
        wargaMap: Map<String, WargaEntity>,
        judulLaporan: String = "Laporan Jimpitan"
    ): Boolean {
        return try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val paintTitle = Paint().apply {
                textSize = 18f
                isFakeBoldText = true
                color = android.graphics.Color.parseColor("#138A4A")
            }
            val paintHeader = Paint().apply {
                textSize = 11f
                isFakeBoldText = true
                color = android.graphics.Color.DKGRAY
            }
            val paintBody = Paint().apply {
                textSize = 10f
                color = android.graphics.Color.DKGRAY
            }
            val paintLine = Paint().apply {
                color = android.graphics.Color.LTGRAY
                strokeWidth = 1f
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displaySdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val now = displaySdf.format(Date())

            var y = 60f
            val marginL = 40f
            val marginR = 555f

            // Title
            canvas.drawText(judulLaporan, marginL, y, paintTitle)
            y += 20f
            canvas.drawText("Dicetak: $now  ·  Total: ${pembayaranList.size} transaksi", marginL, y, paintBody)
            y += 10f
            canvas.drawLine(marginL, y, marginR, y, paintLine)
            y += 16f

            // Table header
            canvas.drawText("No", marginL, y, paintHeader)
            canvas.drawText("Tanggal", marginL + 30, y, paintHeader)
            canvas.drawText("Nama Warga", marginL + 110, y, paintHeader)
            canvas.drawText("Nominal", marginL + 290, y, paintHeader)
            canvas.drawText("Coverage", marginL + 380, y, paintHeader)
            canvas.drawText("Status", marginL + 460, y, paintHeader)
            y += 6f
            canvas.drawLine(marginL, y, marginR, y, paintLine)
            y += 14f

            // Table rows
            pembayaranList.forEachIndexed { idx, item ->
                if (y > 800f) {
                    // Simple: skip if too long (can be extended with multi-page)
                    return@forEachIndexed
                }
                val warga = wargaMap[item.wargaId]
                val namaWarga = warga?.nama ?: item.wargaId.take(8) + "..."
                val tgl = runCatching {
                    displaySdf.format(sdf.parse(item.tanggalBayar)!!)
                }.getOrDefault(item.tanggalBayar)

                canvas.drawText("${idx + 1}", marginL, y, paintBody)
                canvas.drawText(tgl, marginL + 30, y, paintBody)
                canvas.drawText(namaWarga.take(20), marginL + 110, y, paintBody)
                canvas.drawText("Rp%,d".format(item.nominal), marginL + 290, y, paintBody)
                canvas.drawText("${item.coverageDays} hari", marginL + 380, y, paintBody)
                canvas.drawText(item.syncStatus, marginL + 460, y, paintBody)
                y += 16f
            }

            y += 10f
            canvas.drawLine(marginL, y, marginR, y, paintLine)
            y += 16f
            val total = pembayaranList.sumOf { it.nominal }
            canvas.drawText("Total Pemasukan:", marginL + 290, y, paintHeader)
            canvas.drawText("Rp%,d".format(total), marginL + 380, y, paintTitle.apply { textSize = 11f })

            pdfDoc.finishPage(page)

            // Save to Downloads/Jimpitan/
            val fileName = "Laporan_Jimpitan_${
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            }.pdf"

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Jimpitan")
                }
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return false

            val out: OutputStream = resolver.openOutputStream(uri) ?: return false
            pdfDoc.writeTo(out)
            out.close()
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Generate QR Sheet PDF — satu warga per baris dengan QR code.
     * Untuk cetak massal QR.
     */
    fun exportQrSheetPdf(
        context: Context,
        wargaList: List<WargaEntity>
    ): Boolean {
        return try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val paintTitle = Paint().apply {
                textSize = 16f
                isFakeBoldText = true
                color = android.graphics.Color.parseColor("#138A4A")
            }
            val paintBody = Paint().apply {
                textSize = 10f
                color = android.graphics.Color.DKGRAY
            }
            val paintName = Paint().apply {
                textSize = 12f
                isFakeBoldText = true
                color = android.graphics.Color.BLACK
            }

            var y = 50f
            canvas.drawText("Kartu QR Warga — Jimpitan Digital", 40f, y, paintTitle)
            y += 20f

            wargaList.forEachIndexed { _, warga ->
                if (y > 760f) return@forEachIndexed
                val content = "JMP|${warga.qrUuid}"
                // Generate QR bitmap locally (without name label for smaller size)
                try {
                    val pngBytes = io.github.g0dkar.qrcode.QRCode(content).render().getBytes()
                    val qrBitmap = android.graphics.BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
                    val scaledQr = android.graphics.Bitmap.createScaledBitmap(qrBitmap, 100, 100, false)
                    canvas.drawBitmap(scaledQr, 40f, y, null)
                } catch (e: Exception) {
                    // Draw placeholder if QR generation fails
                    val p = Paint().apply { color = android.graphics.Color.LTGRAY }
                    canvas.drawRect(40f, y, 140f, y + 100f, p)
                }
                canvas.drawText(warga.nama, 160f, y + 20f, paintName)
                canvas.drawText("Rumah: ${warga.nomorRumah}  ·  RT ${warga.rt} / RW ${warga.rw}", 160f, y + 38f, paintBody)
                canvas.drawText("ID: ${warga.qrUuid}", 160f, y + 52f, paintBody)
                y += 110f
            }

            pdfDoc.finishPage(page)


            val fileName = "QR_Sheet_${
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            }.pdf"

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Jimpitan")
                }
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return false
            val out: OutputStream = resolver.openOutputStream(uri) ?: return false
            pdfDoc.writeTo(out)
            out.close()
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Generate laporan Buku Kas sebagai PDF dan simpan ke Downloads/Jimpitan/.
     * Returns true jika berhasil.
     */
    fun exportBukuKasPdf(
        context: Context,
        pengeluaranList: List<com.example.data.local.entity.PengeluaranEntity>,
        totalPemasukan: Int,
        totalPengeluaran: Int,
        judulLaporan: String = "Laporan Buku Kas RT 03 / RW 01"
    ): Boolean {
        return try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val paintTitle = Paint().apply {
                textSize = 18f
                isFakeBoldText = true
                color = android.graphics.Color.parseColor("#138A4A")
            }
            val paintHeader = Paint().apply {
                textSize = 11f
                isFakeBoldText = true
                color = android.graphics.Color.DKGRAY
            }
            val paintBody = Paint().apply {
                textSize = 10f
                color = android.graphics.Color.DKGRAY
            }
            val paintLine = Paint().apply {
                color = android.graphics.Color.LTGRAY
                strokeWidth = 1f
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displaySdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val now = displaySdf.format(Date())

            var y = 60f
            val marginL = 40f
            val marginR = 555f

            // Title
            canvas.drawText(judulLaporan, marginL, y, paintTitle)
            y += 20f
            canvas.drawText("Dicetak: $now", marginL, y, paintBody)
            y += 16f
            canvas.drawLine(marginL, y, marginR, y, paintLine)
            y += 20f

            // Summary box/info
            canvas.drawText("Ringkasan Kas:", marginL, y, paintHeader)
            y += 18f
            canvas.drawText("Total Pemasukan (Jimpitan):", marginL, y, paintBody)
            canvas.drawText("Rp%,d".format(totalPemasukan), marginL + 200, y, paintBody)
            y += 16f
            canvas.drawText("Total Pengeluaran:", marginL, y, paintBody)
            canvas.drawText("Rp%,d".format(totalPengeluaran), marginL + 200, y, paintBody)
            y += 16f
            val saldo = totalPemasukan - totalPengeluaran
            canvas.drawText("Saldo Kas saat ini:", marginL, y, paintHeader)
            canvas.drawText("Rp%,d".format(saldo), marginL + 200, y, Paint().apply {
                textSize = 12f
                isFakeBoldText = true
                color = android.graphics.Color.parseColor("#138A4A")
            })
            y += 24f
            canvas.drawLine(marginL, y, marginR, y, paintLine)
            y += 20f

            // Table Header for Pengeluaran
            canvas.drawText("Daftar Pengeluaran:", marginL, y, paintHeader)
            y += 18f
            canvas.drawText("No", marginL, y, paintHeader)
            canvas.drawText("Tanggal", marginL + 30, y, paintHeader)
            canvas.drawText("Keterangan", marginL + 130, y, paintHeader)
            canvas.drawText("Nominal", marginL + 380, y, paintHeader)
            canvas.drawText("Status", marginL + 470, y, paintHeader)
            y += 6f
            canvas.drawLine(marginL, y, marginR, y, paintLine)
            y += 16f

            // Table rows
            pengeluaranList.forEachIndexed { idx, item ->
                if (y > 800f) {
                    return@forEachIndexed
                }
                val tgl = runCatching {
                    displaySdf.format(sdf.parse(item.tanggal)!!)
                }.getOrDefault(item.tanggal)

                canvas.drawText("${idx + 1}", marginL, y, paintBody)
                canvas.drawText(tgl, marginL + 30, y, paintBody)
                canvas.drawText(item.keterangan.take(45), marginL + 130, y, paintBody)
                canvas.drawText("Rp%,d".format(item.nominal), marginL + 380, y, paintBody)
                canvas.drawText(item.syncStatus, marginL + 470, y, paintBody)
                y += 18f
            }

            pdfDoc.finishPage(page)

            // Save to Downloads/Jimpitan/
            val fileName = "Laporan_Buku_Kas_${
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            }.pdf"

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Jimpitan")
                }
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return false

            val out: OutputStream = resolver.openOutputStream(uri) ?: return false
            pdfDoc.writeTo(out)
            out.close()
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
