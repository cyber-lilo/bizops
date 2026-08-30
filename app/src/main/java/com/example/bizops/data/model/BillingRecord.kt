package com.example.bizops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "billing_records")
data class BillingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long? = null,
    val invoiceNumber: String = "",
    val clientName: String = "",
    val amount: Double = 0.0,
    val currency: String = "$",
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: String = "Bank Wire", // Bank Wire, ACH, Credit Card, PayPal, Check, Cash
    val transactionReference: String = "",
    val notes: String = "",
    val status: String = "SETTLED", // SETTLED, PENDING, REFUNDED
    val recordedAt: Long = System.currentTimeMillis()
) {
    fun formattedAmount(): String {
        return "$currency${String.format(Locale.US, "%,.2f", amount)}"
    }
}
