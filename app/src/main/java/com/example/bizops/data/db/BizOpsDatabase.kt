package com.example.bizops.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.bizops.data.model.Client
import com.example.bizops.data.model.CompanyProfile
import com.example.bizops.data.model.EmailCategory
import com.example.bizops.data.model.EmailTemplate
import com.example.bizops.data.model.Invoice
import com.example.bizops.data.model.InvoiceItem
import com.example.bizops.data.model.InvoiceStatus
import com.example.bizops.data.model.OperationTask
import com.example.bizops.data.model.TaskPriority
import com.example.bizops.data.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Client::class,
        OperationTask::class,
        Invoice::class,
        EmailTemplate::class,
        CompanyProfile::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BizOpsDatabase : RoomDatabase() {

    abstract fun clientDao(): ClientDao
    abstract fun taskDao(): OperationTaskDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun emailTemplateDao(): EmailTemplateDao
    abstract fun companyProfileDao(): CompanyProfileDao

    companion object {
        @Volatile
        private var INSTANCE: BizOpsDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): BizOpsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BizOpsDatabase::class.java,
                    "bizops_master_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(db: BizOpsDatabase) {
            val companyDao = db.companyProfileDao()
            val clientDao = db.clientDao()
            val taskDao = db.taskDao()
            val invoiceDao = db.invoiceDao()
            val templateDao = db.emailTemplateDao()

            // 1. Initial Company Profile
            val company = CompanyProfile(
                id = 1,
                companyName = "Apex Operations & Consulting",
                ownerName = "Jordan Vance",
                email = "operations@apexconsult.example.com",
                phone = "+1 (415) 890-2345",
                address = "450 Mission Street, Floor 18\nSan Francisco, CA 94105",
                website = "www.apexconsult.example.com",
                taxId = "US-EIN-94-8172930",
                defaultCurrency = "$",
                defaultTaxPercent = 8.5,
                defaultPaymentTerms = "Net 14",
                paymentInstructions = "Bank Wire Transfer:\nBank: Silicon Valley Commercial Bank\nRouting / ACH: 121000358\nAccount #: 9482-1049-3829\nSwift: SVCBUS33\nPayPal: payments@apexconsult.example.com"
            )
            companyDao.insertOrUpdateProfile(company)

            // 2. Initial Clients
            val client1 = Client(
                id = 1,
                name = "Sarah Jenkins",
                company = "Nexus Dynamics Corp",
                email = "s.jenkins@nexusdynamics.example.com",
                phone = "+1 (212) 555-0192",
                address = "742 Evergreen Terrace, Suite 300\nNew York, NY 10001",
                taxId = "US-NY-492810",
                paymentTerms = "Net 14",
                notes = "Enterprise enterprise ops & cloud infrastructure client. Monthly retainer."
            )
            val client2 = Client(
                id = 2,
                name = "Marcus Chen",
                company = "Vanguard Logistics",
                email = "mchen@vanguardlogistics.example.com",
                phone = "+1 (312) 555-0144",
                address = "1200 Lake Shore Dr, Chicago, IL 60611",
                taxId = "US-IL-981240",
                paymentTerms = "Net 30",
                notes = "Supply chain and warehouse automation consulting."
            )
            val client3 = Client(
                id = 3,
                name = "Elena Rostova",
                company = "Solaria Biotech Labs",
                email = "elena.r@solariabio.example.com",
                phone = "+1 (617) 555-0188",
                address = "200 Kendall Sq, Cambridge, MA 02142",
                taxId = "US-MA-310928",
                paymentTerms = "Net 15",
                notes = "Laboratory operations system rollout and compliance audits."
            )
            clientDao.insertClient(client1)
            clientDao.insertClient(client2)
            clientDao.insertClient(client3)

            // 3. Initial Operations Tasks
            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            val taskList = listOf(
                OperationTask(
                    title = "Q3 Financial Reconciliation & Ledger Audit",
                    description = "Verify all incoming wires, client retainers, and supplier invoices before quarter-end closing.",
                    category = "Finance",
                    priority = TaskPriority.URGENT,
                    status = TaskStatus.IN_PROGRESS,
                    dueDate = now + dayMs * 1,
                    assignedTo = "Jordan Vance",
                    estimatedHours = 4.5,
                    loggedHours = 2.0
                ),
                OperationTask(
                    title = "Nexus Dynamics: Milestone 2 Cloud Infrastructure Review",
                    description = "Deliver architecture diagrams, terraform configurations, and operational cost breakdown.",
                    category = "Client Work",
                    priority = TaskPriority.HIGH,
                    status = TaskStatus.IN_REVIEW,
                    dueDate = now + dayMs * 2,
                    clientId = 1,
                    clientName = "Nexus Dynamics Corp",
                    assignedTo = "DevOps Lead",
                    estimatedHours = 8.0,
                    loggedHours = 7.5
                ),
                OperationTask(
                    title = "Renew Annual SOC-2 & ISO-27001 Compliance Certificate",
                    description = "Collect access logs, encryption validation reports, and submit audit artifacts to auditor.",
                    category = "Compliance",
                    priority = TaskPriority.HIGH,
                    status = TaskStatus.IN_PROGRESS,
                    dueDate = now + dayMs * 5,
                    assignedTo = "Compliance Team",
                    estimatedHours = 12.0,
                    loggedHours = 4.0
                ),
                OperationTask(
                    title = "Vanguard Logistics: Warehouse Dispatch Optimization SLA",
                    description = "Review automated dispatch rule performance and draft monthly SLA uptime report.",
                    category = "Operations",
                    priority = TaskPriority.MEDIUM,
                    status = TaskStatus.BACKLOG,
                    dueDate = now + dayMs * 7,
                    clientId = 2,
                    clientName = "Vanguard Logistics",
                    assignedTo = "Operations Analyst",
                    estimatedHours = 6.0,
                    loggedHours = 0.0
                ),
                OperationTask(
                    title = "Vendor Software License & Tooling Cost Audit",
                    description = "Review unassigned SaaS licenses across Figma, GitHub Enterprise, and AWS instances.",
                    category = "Procurement",
                    priority = TaskPriority.LOW,
                    status = TaskStatus.COMPLETED,
                    dueDate = now - dayMs * 1,
                    assignedTo = "IT Ops",
                    estimatedHours = 3.0,
                    loggedHours = 3.0,
                    completedAt = now - dayMs * 1
                )
            )
            taskList.forEach { taskDao.insertTask(it) }

            // 4. Initial Invoices
            val invoice1 = Invoice(
                invoiceNumber = "INV-2026-001",
                clientId = 1,
                clientName = "Sarah Jenkins",
                clientCompany = "Nexus Dynamics Corp",
                clientEmail = "s.jenkins@nexusdynamics.example.com",
                clientAddress = "742 Evergreen Terrace, Suite 300\nNew York, NY 10001",
                senderName = "Jordan Vance",
                senderCompany = "Apex Operations & Consulting",
                senderEmail = "operations@apexconsult.example.com",
                senderPhone = "+1 (415) 890-2345",
                senderAddress = "450 Mission Street, Floor 18\nSan Francisco, CA 94105",
                senderTaxId = "US-EIN-94-8172930",
                paymentDetails = "Silicon Valley Commercial Bank\nRouting: 121000358 | Acc: 9482-1049-3829\nPayPal: payments@apexconsult.example.com",
                issueDate = now - dayMs * 3,
                dueDate = now + dayMs * 11,
                status = InvoiceStatus.SENT,
                currency = "$",
                taxPercent = 8.5,
                discountPercent = 5.0,
                shippingOrFee = 0.0,
                notes = "Thank you for partnering with Apex Operations. Payment is due within 14 days.",
                termsAndConditions = "Net 14 payment terms. Wire transfer or ACH preferred.",
                items = listOf(
                    InvoiceItem(description = "Enterprise Operations Consulting - Sprint 3", quantity = 40.0, unitPrice = 125.0),
                    InvoiceItem(description = "Cloud Infrastructure & High-Availability Setup", quantity = 1.0, unitPrice = 2200.0),
                    InvoiceItem(description = "24/7 Production SLA Monitoring & Incident Standby", quantity = 1.0, unitPrice = 850.0)
                )
            )

            val invoice2 = Invoice(
                invoiceNumber = "INV-2026-002",
                clientId = 2,
                clientName = "Marcus Chen",
                clientCompany = "Vanguard Logistics",
                clientEmail = "mchen@vanguardlogistics.example.com",
                clientAddress = "1200 Lake Shore Dr, Chicago, IL 60611",
                senderName = "Jordan Vance",
                senderCompany = "Apex Operations & Consulting",
                senderEmail = "operations@apexconsult.example.com",
                senderPhone = "+1 (415) 890-2345",
                senderAddress = "450 Mission Street, Floor 18\nSan Francisco, CA 94105",
                senderTaxId = "US-EIN-94-8172930",
                paymentDetails = "Silicon Valley Commercial Bank\nRouting: 121000358 | Acc: 9482-1049-3829",
                issueDate = now - dayMs * 18,
                dueDate = now - dayMs * 4,
                status = InvoiceStatus.OVERDUE,
                currency = "$",
                taxPercent = 0.0,
                discountPercent = 0.0,
                shippingOrFee = 0.0,
                notes = "Quarterly warehouse optimization services.",
                termsAndConditions = "Payment was due on Net 14 terms.",
                items = listOf(
                    InvoiceItem(description = "Supply Chain Throughput & Bottleneck Audit", quantity = 1.0, unitPrice = 3400.0),
                    InvoiceItem(description = "Custom Automated Dispatch Scripting & QA", quantity = 25.0, unitPrice = 110.0)
                )
            )

            val invoice3 = Invoice(
                invoiceNumber = "INV-2026-003",
                clientId = 3,
                clientName = "Elena Rostova",
                clientCompany = "Solaria Biotech Labs",
                clientEmail = "elena.r@solariabio.example.com",
                clientAddress = "200 Kendall Sq, Cambridge, MA 02142",
                senderName = "Jordan Vance",
                senderCompany = "Apex Operations & Consulting",
                senderEmail = "operations@apexconsult.example.com",
                senderPhone = "+1 (415) 890-2345",
                senderAddress = "450 Mission Street, Floor 18\nSan Francisco, CA 94105",
                senderTaxId = "US-EIN-94-8172930",
                paymentDetails = "Silicon Valley Commercial Bank\nRouting: 121000358 | Acc: 9482-1049-3829",
                issueDate = now - dayMs * 25,
                dueDate = now - dayMs * 10,
                status = InvoiceStatus.PAID,
                paidDate = now - dayMs * 12,
                currency = "$",
                taxPercent = 8.5,
                discountPercent = 0.0,
                shippingOrFee = 0.0,
                notes = "Paid in full. Thank you for your prompt payment!",
                termsAndConditions = "Standard consulting terms.",
                items = listOf(
                    InvoiceItem(description = "Biotech Lab Compliance System Setup", quantity = 1.0, unitPrice = 4500.0)
                )
            )

            val invoice4 = Invoice(
                invoiceNumber = "INV-2026-004",
                clientId = 1,
                clientName = "Sarah Jenkins",
                clientCompany = "Nexus Dynamics Corp",
                clientEmail = "s.jenkins@nexusdynamics.example.com",
                clientAddress = "742 Evergreen Terrace, Suite 300\nNew York, NY 10001",
                senderName = "Jordan Vance",
                senderCompany = "Apex Operations & Consulting",
                senderEmail = "operations@apexconsult.example.com",
                senderPhone = "+1 (415) 890-2345",
                senderAddress = "450 Mission Street, Floor 18\nSan Francisco, CA 94105",
                senderTaxId = "US-EIN-94-8172930",
                paymentDetails = "Silicon Valley Commercial Bank\nRouting: 121000358 | Acc: 9482-1049-3829",
                issueDate = now - dayMs * 55,
                dueDate = now - dayMs * 40,
                status = InvoiceStatus.PAID,
                paidDate = now - dayMs * 42,
                currency = "$",
                taxPercent = 8.5,
                discountPercent = 0.0,
                shippingOrFee = 0.0,
                notes = "Milestone 1 Architecture and Deployment.",
                items = listOf(
                    InvoiceItem(description = "Systems Architecture Review & SLA Design", quantity = 1.0, unitPrice = 3800.0)
                )
            )

            val invoice5 = Invoice(
                invoiceNumber = "INV-2026-005",
                clientId = 2,
                clientName = "Marcus Chen",
                clientCompany = "Vanguard Logistics",
                clientEmail = "mchen@vanguardlogistics.example.com",
                clientAddress = "1200 Lake Shore Dr, Chicago, IL 60611",
                senderName = "Jordan Vance",
                senderCompany = "Apex Operations & Consulting",
                senderEmail = "operations@apexconsult.example.com",
                senderPhone = "+1 (415) 890-2345",
                senderAddress = "450 Mission Street, Floor 18\nSan Francisco, CA 94105",
                senderTaxId = "US-EIN-94-8172930",
                paymentDetails = "Silicon Valley Commercial Bank\nRouting: 121000358 | Acc: 9482-1049-3829",
                issueDate = now - dayMs * 85,
                dueDate = now - dayMs * 70,
                status = InvoiceStatus.PAID,
                paidDate = now - dayMs * 72,
                currency = "$",
                taxPercent = 0.0,
                discountPercent = 0.0,
                shippingOrFee = 0.0,
                notes = "Initial Supply Chain Pipeline Setup.",
                items = listOf(
                    InvoiceItem(description = "Logistics Engine Workflow Integration", quantity = 1.0, unitPrice = 5200.0)
                )
            )

            val invoice6 = Invoice(
                invoiceNumber = "INV-2026-006",
                clientId = 3,
                clientName = "Elena Rostova",
                clientCompany = "Solaria Biotech Labs",
                clientEmail = "elena.r@solariabio.example.com",
                clientAddress = "200 Kendall Sq, Cambridge, MA 02142",
                senderName = "Jordan Vance",
                senderCompany = "Apex Operations & Consulting",
                senderEmail = "operations@apexconsult.example.com",
                senderPhone = "+1 (415) 890-2345",
                senderAddress = "450 Mission Street, Floor 18\nSan Francisco, CA 94105",
                senderTaxId = "US-EIN-94-8172930",
                paymentDetails = "Silicon Valley Commercial Bank\nRouting: 121000358 | Acc: 9482-1049-3829",
                issueDate = now - dayMs * 115,
                dueDate = now - dayMs * 100,
                status = InvoiceStatus.PAID,
                paidDate = now - dayMs * 102,
                currency = "$",
                taxPercent = 8.5,
                discountPercent = 5.0,
                shippingOrFee = 0.0,
                notes = "Consulting retainer kick-off.",
                items = listOf(
                    InvoiceItem(description = "Enterprise Operations Retainer - Sprint 1", quantity = 30.0, unitPrice = 125.0)
                )
            )

            invoiceDao.insertInvoice(invoice1)
            invoiceDao.insertInvoice(invoice2)
            invoiceDao.insertInvoice(invoice3)
            invoiceDao.insertInvoice(invoice4)
            invoiceDao.insertInvoice(invoice5)
            invoiceDao.insertInvoice(invoice6)

            // 5. Initial Rich Daily Email Template Library
            val emailTemplates = listOf(
                EmailTemplate(
                    title = "Invoice Attached & Payment Details",
                    category = EmailCategory.INVOICE_BILLING,
                    subject = "Invoice #{{invoice_number}} from {{company_name}} [Due: {{due_date}}]",
                    body = """Hi {{client_name}},

I hope you are having a productive week.

Please find attached Invoice #{{invoice_number}} for services provided regarding {{project_name}}. 

Invoice Summary:
• Invoice Number: {{invoice_number}}
• Total Amount Due: {{amount}}
• Due Date: {{due_date}}

Payment Instructions:
{{payment_details}}

If you have any questions regarding the line items or require an updated purchase order reference, please let me know.

Thank you for your business!

Best regards,
{{sender_name}}
{{company_name}}"""
                ),
                EmailTemplate(
                    title = "Friendly Upcoming Payment Reminder",
                    category = EmailCategory.INVOICE_BILLING,
                    subject = "Friendly Reminder: Invoice #{{invoice_number}} Due on {{due_date}}",
                    body = """Hi {{client_name}},

I hope everything is going well on your end.

This is a friendly reminder that Invoice #{{invoice_number}} in the amount of {{amount}} is scheduled for payment on {{due_date}}.

If the payment is already in processing with your accounts payable department, please disregard this note. Otherwise, please let me know if you need another copy of the invoice or alternative payment details.

We appreciate our partnership and look forward to continuing our work together!

Warm regards,
{{sender_name}}
{{company_name}}"""
                ),
                EmailTemplate(
                    title = "Overdue Invoice Notice & Settlement",
                    category = EmailCategory.INVOICE_BILLING,
                    subject = "URGENT: Past Due Invoice #{{invoice_number}} - {{company_name}}",
                    body = """Dear {{client_name}},

Our records indicate that Invoice #{{invoice_number}} for the amount of {{amount}}, which was due on {{due_date}}, is currently past due.

To ensure uninterrupted operational support and project continuity, please arrange for the settlement of this balance at your earliest convenience.

Payment Details:
{{payment_details}}

If there are any billing questions, invoice discrepancies, or if payment has already been remitted, please reply with the transaction confirmation so we can update your ledger immediately.

Thank you for your prompt attention to this matter.

Sincerely,
{{sender_name}}
{{company_name}}"""
                ),
                EmailTemplate(
                    title = "Payment Received & Official Receipt",
                    category = EmailCategory.INVOICE_BILLING,
                    subject = "Payment Received - Thank You! [Invoice #{{invoice_number}}]",
                    body = """Hi {{client_name}},

We have successfully received your payment of {{amount}} for Invoice #{{invoice_number}}.

Your account ledger has been updated, and your balance for this invoice is now $0.00 (Paid in Full).

Thank you for your prompt payment and continued trust in {{company_name}}. Please let us know if you need any official tax receipts or statements for your records.

Have a wonderful week!

Best regards,
{{sender_name}}
{{company_name}}"""
                ),
                EmailTemplate(
                    title = "Daily Operations & Standup Digest",
                    category = EmailCategory.OPERATIONS,
                    subject = "Daily Ops Digest - {{date}} [Status & Blockers]",
                    body = """Team & Stakeholders,

Here is our daily operational status update for today ({{date}}):

Key Accomplishments Today:
• {{completed_tasks}}
• Client deliverable milestones progressed according to schedule.

In-Flight Priorities:
• {{active_tasks}}
• Ongoing sprint execution and quality assurance.

Blockers / Items Requiring Decisions:
• {{blockers_or_notes}}

Next 24-Hour Focus:
• Complete scheduled pipeline deliveries and client check-ins.

Please reach out if you need immediate escalation on any operational track.

Best,
{{sender_name}}"""
                ),
                EmailTemplate(
                    title = "Meeting Request & Agenda Alignment",
                    category = EmailCategory.MEETING_FOLLOWUP,
                    subject = "Meeting Request: {{meeting_topic}} - {{company_name}}",
                    body = """Hi {{client_name}},

I'd like to schedule a brief 25-minute check-in regarding {{meeting_topic}} to review progress, align on milestones, and address any open items.

Proposed Agenda:
1. Operational progress & deliverable walkthrough (10 mins)
2. Budget, scope, and upcoming milestones review (10 mins)
3. Q&A and next action items (5 mins)

Would any of the following time slots work on your calendar?
• Option 1: Tuesday at 10:00 AM PST
• Option 2: Wednesday at 2:00 PM PST
• Option 3: Thursday at 11:30 AM PST

Looking forward to connecting!

Best regards,
{{sender_name}}
{{company_name}}"""
                ),
                EmailTemplate(
                    title = "Post-Meeting Summary & Action Items",
                    category = EmailCategory.MEETING_FOLLOWUP,
                    subject = "Summary & Action Items: {{meeting_topic}} [{{date}}]",
                    body = """Hi {{client_name}},

Thank you for taking the time to meet today! Here is a recap of our key discussion points and agreed action items:

Key Decisions:
• Alignment confirmed on project timeline and scope deliverables.
• Resource assignments validated for upcoming milestones.

Action Items:
• [{{sender_name}}] - Deliver updated technical specifications by Friday.
• [{{client_name}}] - Review draft assets and provide sign-off by next Tuesday.
• [Team] - Schedule Milestone 3 checkpoint review.

Please reply to confirm or add anything I might have missed.

Best regards,
{{sender_name}}
{{company_name}}"""
                ),
                EmailTemplate(
                    title = "Client Welcome & Onboarding Guide",
                    category = EmailCategory.CLIENT_PROJECTS,
                    subject = "Welcome to {{company_name}}! Onboarding & Next Steps",
                    body = """Dear {{client_name}},

Welcome aboard! We are thrilled to partner with {{client_company}} on {{project_name}}.

To ensure a seamless kickoff, here is what you can expect over the next few days:

1. Kickoff Schedule: We will send an invite for our initial alignment call.
2. Workspace Access: You will receive access to our shared client portal & project board.
3. Points of Contact:
   • Primary Lead: {{sender_name}} ({{sender_email}})
   • Billing & Operations: {{sender_email}}

Please let us know if you have any initial questions. We're excited to build something impactful together!

Warm regards,
{{sender_name}}
{{company_name}}"""
                ),
                EmailTemplate(
                    title = "Milestone Delivery & Client Review",
                    category = EmailCategory.CLIENT_PROJECTS,
                    subject = "Milestone Delivered: {{milestone_name}} for Review",
                    body = """Hi {{client_name}},

We are excited to share that we have completed all deliverables for {{milestone_name}} ahead of schedule!

Included in this delivery:
• Comprehensive deliverable package and documentation.
• Performance test benchmarks and implementation summary.

Next Steps:
Please review the attached deliverables at your convenience and let us know your feedback within 5 business days.

Thank you for your ongoing collaboration!

Best regards,
{{sender_name}}
{{company_name}}"""
                ),
                EmailTemplate(
                    title = "New Service Proposal & Scope of Work",
                    category = EmailCategory.OUTREACH,
                    subject = "Proposal: {{project_name}} for {{client_company}}",
                    body = """Hi {{client_name}},

Following our recent conversation, I am pleased to present our tailored proposal for {{project_name}}.

Objective & Scope:
Our engagement will focus on streamlining operational workflows, optimizing cost structures, and accelerating milestone execution.

Key Deliverables & Timeline:
• Phase 1: Operational Discovery & Architecture (Weeks 1-2)
• Phase 2: Implementation & Systems Integration (Weeks 3-6)
• Phase 3: Rollout, QA & Ongoing Support (Weeks 7+)

Estimated Investment: {{estimated_cost}}
Payment Terms: {{payment_terms}}

Please review the attached formal proposal. I would love to schedule a follow-up call this Thursday to answer any questions and discuss next steps.

Best regards,
{{sender_name}}
{{company_name}}"""
                ),
                EmailTemplate(
                    title = "Vendor / Supplier Quote Request (RFQ)",
                    category = EmailCategory.OPERATIONS,
                    subject = "Request for Quote (RFQ): {{service_or_item_name}} - {{company_name}}",
                    body = """Hello {{vendor_contact_name}},

I am reaching out on behalf of {{company_name}} to request a formal price quote and delivery timeline for the following requirements:

Item / Service Requested: {{service_or_item_name}}
Estimated Volume / Quantity: {{quantity_needed}}
Required Delivery Date: {{target_date}}

Please provide:
1. Unit and total pricing breakdown (including any volume tiers).
2. Lead time and delivery options.
3. Standard payment and warranty terms.

We look forward to receiving your quote by {{due_date}}.

Thank you,
{{sender_name}}
{{company_name}}"""
                ),
                EmailTemplate(
                    title = "Client Testimonial & Review Request",
                    category = EmailCategory.CLIENT_PROJECTS,
                    subject = "How did we do? Quick feedback on {{project_name}}",
                    body = """Hi {{client_name}},

It has been an absolute pleasure collaborating with you and the team at {{client_company}} on {{project_name}}!

As we continuously strive to elevate our operational standards, we would greatly appreciate 2 minutes of your feedback.

Could you share a brief review of your experience working with {{company_name}}? A sentence or two about our responsiveness, deliverable quality, and impact would mean the world to our team.

Thank you again for your partnership!

Warmest regards,
{{sender_name}}
{{company_name}}"""
                )
            )

            templateDao.insertTemplates(emailTemplates)
        }
    }
}
