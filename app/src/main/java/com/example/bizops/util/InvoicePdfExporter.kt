package com.example.bizops.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.bizops.data.model.CompanyProfile
import com.example.bizops.data.model.Invoice
import com.example.bizops.data.model.InvoiceStatus
import com.example.bizops.data.model.InvoiceTemplateStyle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfExporter {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 36f

    /**
     * Generates a PDF file in the application cache directory.
     */
    fun generatePdfFile(context: Context, invoice: Invoice, companyProfile: CompanyProfile): File {
        val pdfDir = File(context.cacheDir, "invoices")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        val safeInvoiceNo = invoice.invoiceNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(pdfDir, "Invoice_${safeInvoiceNo}.pdf")

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawInvoice(canvas, invoice, companyProfile)

        pdfDocument.finishPage(page)

        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        pdfDocument.close()

        return file
    }

    /**
     * Shares the generated PDF file via Android Share sheet.
     */
    fun sharePdf(context: Context, invoice: Invoice, companyProfile: CompanyProfile) {
        try {
            val file = generatePdfFile(context, invoice, companyProfile)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice ${invoice.invoiceNumber} from ${companyProfile.companyName.ifBlank { "BizOps" }}")
                val clientMsg = if (invoice.clientName.isNotBlank()) "Dear ${invoice.clientName},\n\n" else "Hello,\n\n"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "${clientMsg}Please find attached invoice ${invoice.invoiceNumber} for ${invoice.currency}${String.format(Locale.US, "%.2f", invoice.totalAmount)}.\n\nThank you for your business!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Invoice ${invoice.invoiceNumber} (PDF)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Views the PDF in an external viewer.
     */
    fun viewPdf(context: Context, invoice: Invoice, companyProfile: CompanyProfile) {
        try {
            val file = generatePdfFile(context, invoice, companyProfile)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No PDF viewer found on device. Use Share PDF instead.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves the generated PDF into public Downloads storage or Documents.
     */
    fun savePdfToDownloads(context: Context, invoice: Invoice, companyProfile: CompanyProfile): Boolean {
        return try {
            val safeInvoiceNo = invoice.invoiceNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "Invoice_${safeInvoiceNo}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BizOpsInvoices")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    val pdfFile = generatePdfFile(context, invoice, companyProfile)
                    resolver.openOutputStream(uri)?.use { out ->
                        pdfFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    Toast.makeText(context, "Saved to Downloads/BizOpsInvoices/$fileName", Toast.LENGTH_LONG).show()
                    true
                } else {
                    false
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "BizOpsInvoices")
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, fileName)

                val pdfFile = generatePdfFile(context, invoice, companyProfile)
                pdfFile.copyTo(targetFile, overwrite = true)
                Toast.makeText(context, "Saved to ${targetFile.absolutePath}", Toast.LENGTH_LONG).show()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Draws the complete visual invoice on the PDF canvas.
     */
    private fun drawInvoice(canvas: Canvas, invoice: Invoice, companyProfile: CompanyProfile) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        var y = MARGIN

        val style = InvoiceTemplateStyle.fromId(invoice.templateStyle)

        val baseTypeface = when (style.fontStyle) {
            "Serif" -> Typeface.SERIF
            "Monospace" -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }

        // Extract color components from style
        fun composeColorToInt(c: androidx.compose.ui.graphics.Color): Int {
            return Color.argb(
                (c.alpha * 255).toInt(),
                (c.red * 255).toInt(),
                (c.green * 255).toInt(),
                (c.blue * 255).toInt()
            )
        }

        val primaryColor = composeColorToInt(style.primaryColor)
        val secondaryColor = composeColorToInt(style.secondaryColor)
        val accentColor = composeColorToInt(style.accentColor)

        val darkTextColor = Color.rgb(15, 23, 42) // #0F172A Slate 900
        val secondaryTextColor = Color.rgb(100, 116, 139) // #64748B Slate 500
        val borderLightColor = Color.rgb(226, 232, 240) // #E2E8F0 Slate 200
        val lightBgColor = when (style) {
            InvoiceTemplateStyle.EMERALD_GROWTH -> Color.rgb(240, 253, 244)
            InvoiceTemplateStyle.CREATIVE_CORAL -> Color.rgb(255, 247, 237)
            InvoiceTemplateStyle.ROYAL_ENTERPRISE -> Color.rgb(245, 243, 255)
            InvoiceTemplateStyle.CLASSIC_CORPORATE -> Color.rgb(248, 250, 252)
            else -> Color.rgb(248, 250, 252)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Top Decorative Brand Bar or Dark Hero Header
        if (style.isDarkHero) {
            // Dark Hero Header spanning full top
            paint.color = Color.rgb(15, 23, 42) // Slate 900
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 72f, paint)

            paint.color = accentColor
            canvas.drawRect(0f, 72f, PAGE_WIDTH.toFloat(), 75f, paint)
        } else if (style == InvoiceTemplateStyle.MINIMALIST_CLEAN) {
            // Minimalist: Subtle single hairline bar
            paint.color = borderLightColor
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 3f, paint)
        } else if (style == InvoiceTemplateStyle.CLASSIC_CORPORATE) {
            // Classic: Double border line
            paint.color = primaryColor
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 6f, paint)
            paint.color = secondaryColor
            canvas.drawRect(0f, 8f, PAGE_WIDTH.toFloat(), 10f, paint)
        } else {
            // Standard / Modern / Emerald / Coral / Royal solid top bar
            paint.color = primaryColor
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 8f, paint)
        }

        y += 18f

        // 2. Company Logo & Company Name
        val companyLogoBitmap = LogoPresetManager.getLogoBitmap(invoice.companyLogo, size = 96)
        val logoSize = 38f
        val logoRect = RectF(MARGIN, y, MARGIN + logoSize, y + logoSize)
        canvas.drawBitmap(companyLogoBitmap, null, logoRect, null)

        val compTextStartX = MARGIN + logoSize + 10f
        paint.color = if (style.isDarkHero) Color.WHITE else primaryColor
        paint.typeface = Typeface.create(baseTypeface, Typeface.BOLD)
        paint.textSize = 18f
        val compName = companyProfile.companyName.ifBlank { invoice.senderCompany.ifBlank { "BizOps Systems" } }
        canvas.drawText(compName, compTextStartX, y + 16f, paint)

        // Subtitle tag below company name if dark hero
        if (style.isDarkHero) {
            paint.color = accentColor
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(baseTypeface, Typeface.NORMAL)
            canvas.drawText("ENTERPRISE OPERATIONS & BILLING", compTextStartX, y + 28f, paint)
        }

        // Right side: INVOICE title & number
        paint.color = if (style.isDarkHero) Color.WHITE else darkTextColor
        paint.textSize = 22f
        paint.typeface = Typeface.create(baseTypeface, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("INVOICE", PAGE_WIDTH - MARGIN, y + 16f, paint)
        paint.textAlign = Paint.Align.LEFT

        y += if (style.isDarkHero) 52f else 42f

        // Company Details & Invoice Metadata
        paint.typeface = Typeface.create(baseTypeface, Typeface.NORMAL)
        paint.textSize = 9.5f
        paint.color = secondaryTextColor

        var leftY = y
        val compAddress = companyProfile.address.ifBlank { invoice.senderAddress }
        if (compAddress.isNotBlank()) {
            compAddress.lines().take(2).forEach { line ->
                canvas.drawText(line, MARGIN, leftY, paint)
                leftY += 13f
            }
        }
        val senderEmail = companyProfile.email.ifBlank { invoice.senderEmail }
        val senderPhone = companyProfile.phone.ifBlank { invoice.senderPhone }
        val compContact = listOf(senderEmail, senderPhone).filter { it.isNotBlank() }.joinToString(" • ")
        if (compContact.isNotBlank()) {
            canvas.drawText(compContact, MARGIN, leftY, paint)
            leftY += 13f
        }
        val taxId = companyProfile.taxId.ifBlank { invoice.senderTaxId }
        if (taxId.isNotBlank()) {
            canvas.drawText("Tax/VAT ID: $taxId", MARGIN, leftY, paint)
            leftY += 13f
        }

        // Right Side: Invoice No, Status, Issue Date, Due Date
        var rightY = y
        paint.textAlign = Paint.Align.RIGHT

        paint.typeface = Typeface.create(baseTypeface, Typeface.BOLD)
        paint.color = darkTextColor
        paint.textSize = 12f
        canvas.drawText(invoice.invoiceNumber, PAGE_WIDTH - MARGIN, rightY, paint)
        rightY += 15f

        // Status Badge Pill
        val statusText = invoice.status.name
        val statusColor = when (invoice.status) {
            InvoiceStatus.PAID -> Color.rgb(16, 185, 129) // Emerald
            InvoiceStatus.OVERDUE -> Color.rgb(239, 68, 68) // Red
            InvoiceStatus.SENT -> primaryColor
            InvoiceStatus.DRAFT -> Color.rgb(107, 114, 128) // Gray
            InvoiceStatus.CANCELLED -> Color.rgb(156, 163, 175) // Neutral
        }

        val badgeWidth = paint.measureText(statusText) + 20f
        val badgeRect = RectF(PAGE_WIDTH - MARGIN - badgeWidth, rightY - 11f, PAGE_WIDTH - MARGIN, rightY + 5f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusColor
            this.style = Paint.Style.FILL
        }
        val cornerRadius = if (style == InvoiceTemplateStyle.MINIMALIST_CLEAN) 2f else 8f
        canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, pillPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(baseTypeface, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(statusText, badgeRect.centerX(), rightY - 1f, textPaint)
        rightY += 16f

        paint.typeface = Typeface.create(baseTypeface, Typeface.NORMAL)
        paint.color = secondaryTextColor
        paint.textSize = 9.5f
        canvas.drawText("Issue Date: ${dateFormat.format(Date(invoice.issueDate))}", PAGE_WIDTH - MARGIN, rightY, paint)
        rightY += 13f
        canvas.drawText("Due Date: ${dateFormat.format(Date(invoice.dueDate))}", PAGE_WIDTH - MARGIN, rightY, paint)
        rightY += 13f

        paint.textAlign = Paint.Align.LEFT
        y = maxOf(leftY, rightY) + 10f

        // Divider
        paint.color = borderLightColor
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)
        y += 14f

        // 3. "Billed To" Client Card Box (with Client Logo)
        val clientCardHeight = 64f
        val clientCardRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + clientCardHeight)
        paint.color = lightBgColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(clientCardRect, cornerRadius + 2f, cornerRadius + 2f, paint)

        paint.color = if (style == InvoiceTemplateStyle.MINIMALIST_CLEAN) borderLightColor else primaryColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (style == InvoiceTemplateStyle.CLASSIC_CORPORATE) 1.5f else 1f
        canvas.drawRoundRect(clientCardRect, cornerRadius + 2f, cornerRadius + 2f, paint)
        paint.style = Paint.Style.FILL

        // Client Logo
        val clientLogoBitmap = LogoPresetManager.getLogoBitmap(invoice.clientLogo, size = 80)
        val clientLogoSize = 34f
        val clientLogoRect = RectF(MARGIN + 12f, y + 14f, MARGIN + 12f + clientLogoSize, y + 14f + clientLogoSize)
        canvas.drawBitmap(clientLogoBitmap, null, clientLogoRect, null)

        val clientTextX = MARGIN + 12f + clientLogoSize + 12f
        var cardY = y + 15f
        paint.color = primaryColor
        paint.typeface = Typeface.create(baseTypeface, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawText("BILLED TO", clientTextX, cardY, paint)
        cardY += 13f

        paint.color = darkTextColor
        paint.textSize = 11.5f
        val clientDisplayName = invoice.clientName.ifBlank { "Valued Client" }
        val companyTag = if (invoice.clientCompany.isNotBlank()) " (${invoice.clientCompany})" else ""
        canvas.drawText(clientDisplayName + companyTag, clientTextX, cardY, paint)
        cardY += 13f

        paint.color = secondaryTextColor
        paint.typeface = Typeface.create(baseTypeface, Typeface.NORMAL)
        paint.textSize = 9f
        val clientDetails = listOf(invoice.clientAddress, invoice.clientEmail)
            .filter { it.isNotBlank() }
            .joinToString("  •  ")
        canvas.drawText(clientDetails.ifBlank { "Billing contact on file" }, clientTextX, cardY, paint)

        y += clientCardHeight + 16f

        // 4. Line Items Table Header (with # Num column)
        val colNumX = MARGIN + 10f
        val colDescX = MARGIN + 36f
        val colQtyX = 330f
        val colUnitPriceX = 405f
        val colTotalX = PAGE_WIDTH - MARGIN - 10f

        val tableHeaderHeight = 24f
        val headerRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + tableHeaderHeight)
        paint.color = when (style) {
            InvoiceTemplateStyle.TECH_DARK_HERO -> Color.rgb(15, 23, 42)
            InvoiceTemplateStyle.MINIMALIST_CLEAN -> Color.rgb(241, 245, 249)
            InvoiceTemplateStyle.CLASSIC_CORPORATE -> primaryColor
            else -> Color.rgb(30, 41, 59)
        }
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(headerRect, cornerRadius, cornerRadius, paint)

        paint.color = if (style == InvoiceTemplateStyle.MINIMALIST_CLEAN) darkTextColor else Color.WHITE
        paint.typeface = Typeface.create(baseTypeface, Typeface.BOLD)
        paint.textSize = 9f

        canvas.drawText("#", colNumX, y + 16f, paint)
        canvas.drawText("ITEM DESCRIPTION", colDescX, y + 16f, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("QTY", colQtyX, y + 16f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("UNIT PRICE", colUnitPriceX, y + 16f, paint)
        canvas.drawText("TOTAL (${invoice.currency})", colTotalX, y + 16f, paint)
        paint.textAlign = Paint.Align.LEFT

        y += tableHeaderHeight + 4f

        // 5. Line Items Rows
        val rowHeight = 22f
        paint.typeface = Typeface.create(baseTypeface, Typeface.NORMAL)
        paint.textSize = 9f

        invoice.items.forEachIndexed { index, item ->
            if (index % 2 == 1) {
                paint.color = lightBgColor
                paint.style = Paint.Style.FILL
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, paint)
            }

            paint.color = secondaryTextColor
            canvas.drawText("${index + 1}", colNumX, y + 15f, paint)

            paint.color = darkTextColor
            canvas.drawText(item.description.ifBlank { "Item ${index + 1}" }, colDescX, y + 15f, paint)

            paint.textAlign = Paint.Align.CENTER
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            canvas.drawText(qtyStr, colQtyX, y + 15f, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                "${invoice.currency}${String.format(Locale.US, "%.2f", item.unitPrice)}",
                colUnitPriceX,
                y + 15f,
                paint
            )

            paint.typeface = Typeface.create(baseTypeface, Typeface.BOLD)
            canvas.drawText(
                "${invoice.currency}${String.format(Locale.US, "%.2f", item.total)}",
                colTotalX,
                y + 15f,
                paint
            )
            paint.typeface = Typeface.create(baseTypeface, Typeface.NORMAL)
            paint.textAlign = Paint.Align.LEFT

            y += rowHeight
        }

        // Table bottom border
        paint.color = if (style == InvoiceTemplateStyle.CLASSIC_CORPORATE) primaryColor else borderLightColor
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, y + 2f, PAGE_WIDTH - MARGIN, y + 2f, paint)
        y += 12f

        // 6. Summary Totals Section & Notes Layout
        val totalsBlockWidth = 200f
        val totalsStartX = PAGE_WIDTH - MARGIN - totalsBlockWidth

        // Left side: Payment Info & Notes
        var leftNotesY = y
        val paymentInfo = invoice.paymentDetails.ifBlank { companyProfile.paymentInstructions }
        if (paymentInfo.isNotBlank()) {
            paint.typeface = Typeface.create(baseTypeface, Typeface.BOLD)
            paint.color = primaryColor
            paint.textSize = 9f
            canvas.drawText("PAYMENT INSTRUCTIONS", MARGIN, leftNotesY, paint)
            leftNotesY += 12f

            paint.typeface = Typeface.create(baseTypeface, Typeface.NORMAL)
            paint.color = darkTextColor
            paint.textSize = 8.5f
            paymentInfo.lines().take(3).forEach { line ->
                canvas.drawText(line, MARGIN, leftNotesY, paint)
                leftNotesY += 11f
            }
            leftNotesY += 4f
        }

        if (invoice.notes.isNotBlank()) {
            paint.typeface = Typeface.create(baseTypeface, Typeface.BOLD)
            paint.color = secondaryTextColor
            paint.textSize = 9f
            canvas.drawText("NOTES / TERMS", MARGIN, leftNotesY, paint)
            leftNotesY += 12f

            paint.typeface = Typeface.create(baseTypeface, Typeface.NORMAL)
            paint.color = secondaryTextColor
            paint.textSize = 8f
            invoice.notes.lines().take(2).forEach { line ->
                canvas.drawText(line, MARGIN, leftNotesY, paint)
                leftNotesY += 11f
            }
        }

        // Right side: Financial Totals Breakdown
        var rightTotalsY = y
        fun drawTotalLine(label: String, amountStr: String, isBold: Boolean = false, textColor: Int = darkTextColor) {
            paint.color = textColor
            paint.typeface = if (isBold) Typeface.create(baseTypeface, Typeface.BOLD) else Typeface.create(baseTypeface, Typeface.NORMAL)
            paint.textSize = 9f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, totalsStartX, rightTotalsY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(amountStr, PAGE_WIDTH - MARGIN - 6f, rightTotalsY, paint)
            rightTotalsY += 14f
        }

        drawTotalLine("Subtotal:", "${invoice.currency}${String.format(Locale.US, "%.2f", invoice.subtotal)}")

        if (invoice.discountPercent > 0) {
            drawTotalLine(
                "Discount (${invoice.discountPercent}%):",
                "-${invoice.currency}${String.format(Locale.US, "%.2f", invoice.discountAmount)}",
                textColor = Color.rgb(220, 38, 38)
            )
        }

        if (invoice.taxPercent > 0) {
            drawTotalLine(
                "Tax / VAT (${invoice.taxPercent}%):",
                "+${invoice.currency}${String.format(Locale.US, "%.2f", invoice.taxAmount)}"
            )
        }

        if (invoice.shippingOrFee > 0) {
            drawTotalLine(
                "Shipping / Fee:",
                "+${invoice.currency}${String.format(Locale.US, "%.2f", invoice.shippingOrFee)}"
            )
        }

        // Final Total Highlight Box
        val grandTotalBoxHeight = 28f
        val grandTotalRect = RectF(totalsStartX - 8f, rightTotalsY, PAGE_WIDTH - MARGIN, rightTotalsY + grandTotalBoxHeight)
        paint.color = if (style == InvoiceTemplateStyle.CLASSIC_CORPORATE) secondaryColor else primaryColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(grandTotalRect, cornerRadius, cornerRadius, paint)

        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(baseTypeface, Typeface.BOLD)
        }
        canvas.drawText("TOTAL DUE", totalsStartX, rightTotalsY + 18f, whitePaint)

        whitePaint.textAlign = Paint.Align.RIGHT
        whitePaint.textSize = 12f
        canvas.drawText(
            "${invoice.currency}${String.format(Locale.US, "%.2f", invoice.totalAmount)}",
            PAGE_WIDTH - MARGIN - 8f,
            rightTotalsY + 18f,
            whitePaint
        )

        // 7. Signature & Authorization Section
        val signY = maxOf(leftNotesY, rightTotalsY + grandTotalBoxHeight) + 20f
        val sigBoxWidth = 200f
        val sigBoxHeight = 65f
        val sigStartX = PAGE_WIDTH - MARGIN - sigBoxWidth

        // Draw signature line / box
        paint.color = Color.rgb(241, 245, 249) // Slate 100
        paint.style = Paint.Style.FILL
        val sigRect = RectF(sigStartX, signY, PAGE_WIDTH - MARGIN, signY + sigBoxHeight)
        canvas.drawRoundRect(sigRect, 8f, 8f, paint)

        paint.color = borderLightColor
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(sigRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Draw Signature bitmap
        val sigBitmap = LogoPresetManager.getSignatureBitmap(invoice.signatureData, invoice.signatoryName, width = 240, height = 70)
        val sigBitmapRect = RectF(sigStartX + 10f, signY + 4f, PAGE_WIDTH - MARGIN - 10f, signY + 38f)
        canvas.drawBitmap(sigBitmap, null, sigBitmapRect, null)

        // Signature baseline
        paint.color = Color.rgb(203, 213, 225)
        paint.strokeWidth = 1f
        canvas.drawLine(sigStartX + 12f, signY + 42f, PAGE_WIDTH - MARGIN - 12f, signY + 42f, paint)

        // Signatory Name & Title
        paint.color = darkTextColor
        paint.typeface = Typeface.create(baseTypeface, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(invoice.signatoryName.ifBlank { "Authorized Signature" }, sigStartX + (sigBoxWidth / 2f), signY + 52f, paint)

        paint.color = secondaryTextColor
        paint.typeface = Typeface.create(baseTypeface, Typeface.NORMAL)
        paint.textSize = 7.5f
        canvas.drawText(invoice.signatoryTitle.ifBlank { "Authorized Representative" }, sigStartX + (sigBoxWidth / 2f), signY + 61f, paint)

        // 8. Footer
        val footerY = PAGE_HEIGHT - 28f
        paint.color = borderLightColor
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, footerY - 12f, PAGE_WIDTH - MARGIN, footerY - 12f, paint)

        paint.color = secondaryTextColor
        paint.textSize = 8f
        paint.typeface = Typeface.create(baseTypeface, Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Thank you for your business! • Style: ${style.title} • Generated by BizOps Operations System", PAGE_WIDTH / 2f, footerY, paint)
    }
}
