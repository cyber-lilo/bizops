package com.example.bizops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EmailCategory(val displayName: String) {
    ALL("All"),
    INVOICE_BILLING("Billing & Invoices"),
    OPERATIONS("Operations & Daily"),
    CLIENT_PROJECTS("Clients & Projects"),
    MEETING_FOLLOWUP("Meetings & Updates"),
    OUTREACH("Outreach & Proposals"),
    CUSTOM("Custom Templates")
}

@Entity(tableName = "email_templates")
data class EmailTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: EmailCategory = EmailCategory.OPERATIONS,
    val subject: String,
    val body: String,
    val isCustom: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "company_profile")
data class CompanyProfile(
    @PrimaryKey
    val id: Int = 1,
    val companyName: String = "BizOps Studio LLC",
    val ownerName: String = "Alex Morgan",
    val email: String = "contact@bizops.example.com",
    val phone: String = "+1 (555) 019-2834",
    val address: String = "100 Tech Enterprise Blvd, Suite 400\nSan Francisco, CA 94105",
    val website: String = "www.bizops.example.com",
    val taxId: String = "US-EIN-9482710",
    val defaultCurrency: String = "$",
    val defaultTaxPercent: Double = 8.5,
    val defaultPaymentTerms: String = "Net 14",
    val paymentInstructions: String = "Wire Transfer: Bank of America\nRouting: 121000358\nAccount: 9482-0193-8472\nPayPal: billing@bizops.example.com"
)
