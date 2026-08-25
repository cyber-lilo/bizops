package com.example.bizops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.util.Locale

enum class InvoiceStatus {
    DRAFT, SENT, PAID, OVERDUE, CANCELLED
}

data class InvoiceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0
) {
    val total: Double
        get() = quantity * unitPrice
}

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val clientId: Long? = null,
    val clientName: String = "",
    val clientCompany: String = "",
    val clientEmail: String = "",
    val clientAddress: String = "",
    val senderName: String = "BizOps Studio LLC",
    val senderCompany: String = "BizOps Enterprise Solutions",
    val senderEmail: String = "billing@bizops.example.com",
    val senderPhone: String = "+1 (555) 019-2834",
    val senderAddress: String = "100 Tech Enterprise Blvd, Suite 400\nSan Francisco, CA 94105",
    val senderTaxId: String = "US-EIN-9482710",
    val paymentDetails: String = "Bank of America | Routing: 121000358 | Acc: 9482-0193-8472\nPayPal: billing@bizops.example.com",
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + 86400000L * 14, // 14 days
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val currency: String = "$",
    val items: List<InvoiceItem> = emptyList(),
    val taxPercent: Double = 0.0,
    val discountPercent: Double = 0.0,
    val shippingOrFee: Double = 0.0,
    val notes: String = "Thank you for your business! Payment is requested within terms.",
    val termsAndConditions: String = "Please pay within 14 days of invoice date. Late payments may incur a 1.5% monthly fee.",
    val companyLogo: String? = "preset:apex",
    val clientLogo: String? = "preset:client_corp",
    val signatureData: String? = null,
    val signatoryName: String = "Jordan Vance",
    val signatoryTitle: String = "Authorized Representative",
    val templateStyle: String = "modern_executive",
    val paidDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val subtotal: Double
        get() = items.sumOf { it.total }

    val discountAmount: Double
        get() = (subtotal * (discountPercent / 100.0))

    val taxableAmount: Double
        get() = (subtotal - discountAmount).coerceAtLeast(0.0)

    val taxAmount: Double
        get() = (taxableAmount * (taxPercent / 100.0))

    val totalAmount: Double
        get() = (taxableAmount + taxAmount + shippingOrFee).coerceAtLeast(0.0)

    fun formattedTotal(): String {
        return "$currency${String.format(Locale.US, "%,.2f", totalAmount)}"
    }

    fun formattedSubtotal(): String {
        return "$currency${String.format(Locale.US, "%,.2f", subtotal)}"
    }
}
