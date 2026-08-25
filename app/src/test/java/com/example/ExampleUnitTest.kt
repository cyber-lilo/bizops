package com.example

import com.example.bizops.data.model.Client
import com.example.bizops.data.model.CompanyProfile
import com.example.bizops.data.model.Invoice
import com.example.bizops.data.model.InvoiceItem
import com.example.bizops.data.model.InvoiceStatus
import com.example.bizops.ui.components.computeMonthlyBuckets
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun invoice_calculation_isAccurate() {
    val items = listOf(
      InvoiceItem(description = "Ops Consulting", quantity = 10.0, unitPrice = 150.0), // 1500
      InvoiceItem(description = "Server Setup", quantity = 1.0, unitPrice = 500.0)      // 500
    )
    val invoice = Invoice(
      invoiceNumber = "INV-2026-001",
      items = items,
      discountPercent = 10.0, // 2000 - 200 = 1800
      taxPercent = 10.0,      // 1800 * 0.10 = 180
      shippingOrFee = 50.0    // 1800 + 180 + 50 = 2030.0
    )

    assertEquals(2000.0, invoice.subtotal, 0.01)
    assertEquals(200.0, invoice.discountAmount, 0.01)
    assertEquals(1800.0, invoice.taxableAmount, 0.01)
    assertEquals(180.0, invoice.taxAmount, 0.01)
    assertEquals(2030.0, invoice.totalAmount, 0.01)
  }

  @Test
  fun invoice_zeroAdjustments_matchesSubtotal() {
    val items = listOf(
      InvoiceItem(description = "Logistics Plan", quantity = 2.0, unitPrice = 300.0)
    )
    val invoice = Invoice(
      invoiceNumber = "INV-2026-002",
      items = items
    )

    assertEquals(600.0, invoice.subtotal, 0.01)
    assertEquals(0.0, invoice.discountAmount, 0.01)
    assertEquals(0.0, invoice.taxAmount, 0.01)
    assertEquals(600.0, invoice.totalAmount, 0.01)
  }

  @Test
  fun invoice_formattedMethods_includeCurrency() {
    val items = listOf(
      InvoiceItem(description = "Security Audit", quantity = 1.0, unitPrice = 1250.50)
    )
    val invoice = Invoice(
      invoiceNumber = "INV-2026/003#A",
      currency = "€",
      items = items
    )

    assertTrue(invoice.formattedTotal().contains("€"))
    assertTrue(invoice.formattedTotal().contains("1,250.50"))
    val safeName = invoice.invoiceNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    assertEquals("INV-2026_003_A", safeName)
  }

  @Test
  fun computeMonthlyBuckets_returnsCorrectNumberOfMonthsAndAggregates() {
    val now = System.currentTimeMillis()
    val dayMs = 86400000L

    val invoices = listOf(
      Invoice(
        invoiceNumber = "INV-01",
        issueDate = now - dayMs * 5,
        status = InvoiceStatus.PAID,
        items = listOf(InvoiceItem(description = "Service A", quantity = 1.0, unitPrice = 1000.0))
      ),
      Invoice(
        invoiceNumber = "INV-02",
        issueDate = now - dayMs * 5,
        status = InvoiceStatus.SENT,
        items = listOf(InvoiceItem(description = "Service B", quantity = 1.0, unitPrice = 500.0))
      )
    )

    val buckets6M = computeMonthlyBuckets(invoices, 6)
    assertEquals(6, buckets6M.size)

    val currentMonthBucket = buckets6M.last()
    assertEquals(1500.0, currentMonthBucket.totalAmount, 0.01)
    assertEquals(1000.0, currentMonthBucket.paidAmount, 0.01)
    assertEquals(500.0, currentMonthBucket.pendingAmount, 0.01)
    assertEquals(2, currentMonthBucket.invoiceCount)

    val buckets12M = computeMonthlyBuckets(invoices, 12)
    assertEquals(12, buckets12M.size)
  }
}

