package com.akashascent.akashvidya

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object InvoiceGenerator {

    fun generateAndOpenInvoice(
        context: Context,
        paymentId: String,
        purchaseDate: String,
        item: ItemModel,
        userName: String,
        userPhone: String
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val titlePaint = Paint()
        val headerPaint = Paint()
        val contentPaint = Paint()
        val linePaint = Paint()

        // Colors
        val blueColor = Color.parseColor("#007BFF")
        val blackColor = Color.BLACK
        val grayColor = Color.DKGRAY

        // Header: AKASH ASCENT
        titlePaint.color = blueColor
        titlePaint.textSize = 28f
        titlePaint.isFakeBoldText = true
        canvas.drawText("AKASH ASCENT", 40f, 60f, titlePaint)

        // Subtitle
        headerPaint.color = grayColor
        headerPaint.textSize = 12f
        headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Learn, Grow, Succeed", 40f, 80f, headerPaint)

        // Invoice Label
        headerPaint.color = blackColor
        headerPaint.textSize = 20f
        headerPaint.isFakeBoldText = true
        headerPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("INVOICE", 555f, 60f, headerPaint)

        // Line separator
        linePaint.color = Color.LTGRAY
        linePaint.strokeWidth = 2f
        canvas.drawLine(40f, 100f, 555f, 100f, linePaint)

        // Customer Details Section
        var y = 140f
        headerPaint.textAlign = Paint.Align.LEFT
        headerPaint.textSize = 14f
        headerPaint.isFakeBoldText = true
        canvas.drawText("CUSTOMER DETAILS", 40f, y, headerPaint)
        
        y += 25f
        contentPaint.textSize = 12f
        contentPaint.color = blackColor
        canvas.drawText("Name: ${userName.ifEmpty { "N/A" }}", 40f, y, contentPaint)
        
        y += 20f
        canvas.drawText("Phone: ${userPhone.ifEmpty { "N/A" }}", 40f, y, contentPaint)

        // Purchase Details Section
        y += 40f
        headerPaint.isFakeBoldText = true
        canvas.drawText("PURCHASE DETAILS", 40f, y, headerPaint)

        y += 25f
        canvas.drawText("Payment ID:", 40f, y, contentPaint)
        canvas.drawText(paymentId, 160f, y, contentPaint)

        y += 20f
        canvas.drawText("Course Name:", 40f, y, contentPaint)
        canvas.drawText(item.name ?: "N/A", 160f, y, contentPaint)

        y += 20f
        canvas.drawText("Course Validity:", 40f, y, contentPaint)
        canvas.drawText(item.getFormattedValidity(), 160f, y, contentPaint)

        y += 20f
        canvas.drawText("Purchase Date:", 40f, y, contentPaint)
        canvas.drawText(purchaseDate, 160f, y, contentPaint)

        // Amount Section
        y += 40f
        linePaint.strokeWidth = 1f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        
        y += 30f
        headerPaint.textSize = 16f
        canvas.drawText("Total Amount Paid:", 40f, y, headerPaint)
        headerPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${item.price ?: "0"}", 555f, y, headerPaint)

        // Footer
        y = 780f
        headerPaint.textAlign = Paint.Align.CENTER
        headerPaint.textSize = 11f
        headerPaint.isFakeBoldText = false
        headerPaint.color = grayColor
        canvas.drawText("Thank you for your purchase from AKASH ASCENT!", 297.5f, y, headerPaint)
        
        y += 15f
        canvas.drawText("This is a computer generated invoice and does not require a signature.", 297.5f, y, headerPaint)

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), "Invoice_${paymentId}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error generating invoice: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        pdfDocument.close()
    }
}
