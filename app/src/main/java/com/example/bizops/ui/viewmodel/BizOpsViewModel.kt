package com.example.bizops.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bizops.data.ai.GeminiOpsService
import com.example.bizops.data.db.BizOpsDatabase
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
import com.example.bizops.data.repository.BizOpsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OperationsMetrics(
    val totalRevenue: Double = 0.0,
    val pendingRevenue: Double = 0.0,
    val overdueRevenue: Double = 0.0,
    val activeTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val totalClientsCount: Int = 0,
    val completionRatePercent: Int = 0,
    val currency: String = "$"
)

class BizOpsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BizOpsRepository

    init {
        val db = BizOpsDatabase.getDatabase(application, viewModelScope)
        repository = BizOpsRepository(db)
    }

    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<OperationTask>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emailTemplates: StateFlow<List<EmailTemplate>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val companyProfile: StateFlow<CompanyProfile> = repository.companyProfile
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CompanyProfile()
        ).combine(repository.companyProfile) { _, profile ->
            profile ?: CompanyProfile()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompanyProfile())

    // Metrics
    val metrics: StateFlow<OperationsMetrics> = combine(invoices, tasks, clients, companyProfile) { invList, taskList, clientList, profile ->
        val paidRev = invList.filter { it.status == InvoiceStatus.PAID }.sumOf { it.totalAmount }
        val sentRev = invList.filter { it.status == InvoiceStatus.SENT }.sumOf { it.totalAmount }
        val overdueRev = invList.filter { it.status == InvoiceStatus.OVERDUE }.sumOf { it.totalAmount }
        val activeTasks = taskList.count { it.status != TaskStatus.COMPLETED }
        val completedTasks = taskList.count { it.status == TaskStatus.COMPLETED }
        val totalTasks = taskList.size
        val rate = if (totalTasks > 0) ((completedTasks.toDouble() / totalTasks) * 100).toInt() else 100

        OperationsMetrics(
            totalRevenue = paidRev,
            pendingRevenue = sentRev,
            overdueRevenue = overdueRev,
            activeTasksCount = activeTasks,
            completedTasksCount = completedTasks,
            totalClientsCount = clientList.size,
            completionRatePercent = rate,
            currency = profile?.defaultCurrency ?: "$"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OperationsMetrics())

    // AI Generation State
    private val _isGeneratingAi = MutableStateFlow(false)
    val isGeneratingAi: StateFlow<Boolean> = _isGeneratingAi.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    // --- Task Actions ---
    fun saveTask(task: OperationTask) {
        viewModelScope.launch {
            if (task.id == 0L) {
                repository.insertTask(task)
            } else {
                repository.updateTask(task)
            }
        }
    }

    fun updateTaskStatus(id: Long, status: TaskStatus) {
        viewModelScope.launch {
            repository.updateTaskStatus(id, status)
        }
    }

    fun deleteTask(task: OperationTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // --- Invoice Actions ---
    fun saveInvoice(invoice: Invoice) {
        viewModelScope.launch {
            if (invoice.id == 0L) {
                repository.insertInvoice(invoice)
            } else {
                repository.updateInvoice(invoice)
            }
        }
    }

    fun updateInvoiceStatus(invoice: Invoice, newStatus: InvoiceStatus) {
        viewModelScope.launch {
            val updated = invoice.copy(
                status = newStatus,
                paidDate = if (newStatus == InvoiceStatus.PAID) System.currentTimeMillis() else invoice.paidDate
            )
            repository.updateInvoice(updated)
        }
    }

    fun duplicateInvoice(invoice: Invoice) {
        viewModelScope.launch {
            val newNumber = "INV-" + SimpleDateFormat("yyyy-MMdd", Locale.US).format(Date()) + "-" + (100..999).random()
            val duplicated = invoice.copy(
                id = 0,
                invoiceNumber = newNumber,
                status = InvoiceStatus.DRAFT,
                issueDate = System.currentTimeMillis(),
                dueDate = System.currentTimeMillis() + 86400000L * 14,
                paidDate = null,
                createdAt = System.currentTimeMillis()
            )
            repository.insertInvoice(duplicated)
        }
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
        }
    }

    // --- Client Actions ---
    fun saveClient(client: Client) {
        viewModelScope.launch {
            if (client.id == 0L) {
                repository.insertClient(client)
            } else {
                repository.updateClient(client)
            }
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    // --- Template Actions ---
    fun saveTemplate(template: EmailTemplate) {
        viewModelScope.launch {
            if (template.id == 0L) {
                repository.insertTemplate(template)
            } else {
                repository.updateTemplate(template)
            }
        }
    }

    fun deleteTemplate(template: EmailTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    fun recordTemplateUsage(templateId: Long) {
        viewModelScope.launch {
            repository.recordTemplateUsage(templateId)
        }
    }

    // --- Company Profile Actions ---
    fun saveCompanyProfile(profile: CompanyProfile) {
        viewModelScope.launch {
            repository.saveCompanyProfile(profile)
        }
    }

    // --- AI Generator Actions ---
    fun generateAiEmail(
        prompt: String,
        tone: String,
        recipientName: String,
        companyName: String,
        onSuccess: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            _isGeneratingAi.value = true
            _aiError.value = null
            try {
                val profile = companyProfile.value
                val senderName = profile.ownerName.ifBlank { "Operations Lead" }
                val compName = if (companyName.isNotBlank()) companyName else profile.companyName
                val client = if (recipientName.isNotBlank()) recipientName else "Valued Client"

                val result = GeminiOpsService.generateCustomEmail(
                    prompt = prompt,
                    tone = tone,
                    senderName = senderName,
                    companyName = compName,
                    clientName = client
                )
                onSuccess(result.subject, result.body)
            } catch (e: Exception) {
                _aiError.value = e.message ?: "Failed to generate email"
            } finally {
                _isGeneratingAi.value = false
            }
        }
    }

    fun enhanceAiEmail(
        currentSubject: String,
        currentBody: String,
        tone: String,
        instruction: String,
        onSuccess: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            _isGeneratingAi.value = true
            _aiError.value = null
            try {
                val result = GeminiOpsService.enhanceOrRephraseEmail(
                    currentSubject = currentSubject,
                    currentBody = currentBody,
                    tone = tone,
                    instruction = instruction
                )
                onSuccess(result.subject, result.body)
            } catch (e: Exception) {
                _aiError.value = e.message ?: "Failed to enhance email"
            } finally {
                _isGeneratingAi.value = false
            }
        }
    }

    // --- Helper for Template Variable Replacement ---
    fun populateTemplateVariables(
        templateSubject: String,
        templateBody: String,
        client: Client? = null,
        invoice: Invoice? = null,
        company: CompanyProfile? = null,
        customParams: Map<String, String> = emptyMap()
    ): Pair<String, String> {
        val comp = company ?: companyProfile.value
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val todayStr = dateFormat.format(Date())

        val values = mutableMapOf<String, String>()
        values["date"] = todayStr
        values["company_name"] = comp.companyName
        values["sender_name"] = comp.ownerName
        values["sender_email"] = comp.email
        values["sender_phone"] = comp.phone
        values["payment_details"] = comp.paymentInstructions

        client?.let {
            values["client_name"] = it.name
            values["client_company"] = it.company.ifBlank { it.name }
            values["client_email"] = it.email
            values["client_phone"] = it.phone
            values["payment_terms"] = it.paymentTerms
        }

        invoice?.let {
            values["invoice_number"] = it.invoiceNumber
            values["amount"] = it.formattedTotal()
            values["subtotal"] = it.formattedSubtotal()
            values["due_date"] = dateFormat.format(Date(it.dueDate))
            values["issue_date"] = dateFormat.format(Date(it.issueDate))
            if (it.clientName.isNotBlank()) values["client_name"] = it.clientName
            if (it.clientCompany.isNotBlank()) values["client_company"] = it.clientCompany
            if (it.paymentDetails.isNotBlank()) values["payment_details"] = it.paymentDetails
            values["project_name"] = it.items.firstOrNull()?.description ?: "Operations Services"
        }

        // Add custom overrides
        values.putAll(customParams)

        var resSubject = templateSubject
        var resBody = templateBody

        values.forEach { (key, value) ->
            resSubject = resSubject.replace("{{$key}}", value)
            resBody = resBody.replace("{{$key}}", value)
        }

        // Clean any leftover unmatched curly tags
        resSubject = resSubject.replace(Regex("\\{\\{.*?\\}\\}"), "")
        resBody = resBody.replace(Regex("\\{\\{.*?\\}\\}"), "[Details]")

        return Pair(resSubject, resBody)
    }

    fun formatInvoiceAsShareableText(invoice: Invoice, company: CompanyProfile): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("==========================================")
        sb.appendLine("               INVOICE                    ")
        sb.appendLine("==========================================")
        sb.appendLine("Invoice No: ${invoice.invoiceNumber}")
        sb.appendLine("Status:     ${invoice.status.name}")
        sb.appendLine("Issue Date: ${dateFormat.format(Date(invoice.issueDate))}")
        sb.appendLine("Due Date:   ${dateFormat.format(Date(invoice.dueDate))}")
        sb.appendLine("------------------------------------------")
        sb.appendLine("FROM:")
        sb.appendLine(company.companyName)
        if (company.ownerName.isNotBlank()) sb.appendLine("Attn: ${company.ownerName}")
        if (company.email.isNotBlank()) sb.appendLine("Email: ${company.email}")
        if (company.phone.isNotBlank()) sb.appendLine("Tel: ${company.phone}")
        sb.appendLine("------------------------------------------")
        sb.appendLine("BILLED TO:")
        sb.appendLine(invoice.clientName)
        if (invoice.clientCompany.isNotBlank()) sb.appendLine(invoice.clientCompany)
        if (invoice.clientEmail.isNotBlank()) sb.appendLine("Email: ${invoice.clientEmail}")
        if (invoice.clientAddress.isNotBlank()) sb.appendLine("Address: ${invoice.clientAddress}")
        sb.appendLine("------------------------------------------")
        sb.appendLine("LINE ITEMS:")
        invoice.items.forEachIndexed { index, item ->
            sb.appendLine("${index + 1}. ${item.description}")
            sb.appendLine("   Qty: ${item.quantity} x ${invoice.currency}${String.format(Locale.US, "%.2f", item.unitPrice)} = ${invoice.currency}${String.format(Locale.US, "%.2f", item.total)}")
        }
        sb.appendLine("------------------------------------------")
        sb.appendLine("Subtotal:        ${invoice.formattedSubtotal()}")
        if (invoice.discountPercent > 0) {
            sb.appendLine("Discount (${invoice.discountPercent}%): -${invoice.currency}${String.format(Locale.US, "%.2f", invoice.discountAmount)}")
        }
        if (invoice.taxPercent > 0) {
            sb.appendLine("Tax (${invoice.taxPercent}%):        +${invoice.currency}${String.format(Locale.US, "%.2f", invoice.taxAmount)}")
        }
        if (invoice.shippingOrFee > 0) {
            sb.appendLine("Shipping/Fee:    +${invoice.currency}${String.format(Locale.US, "%.2f", invoice.shippingOrFee)}")
        }
        sb.appendLine("TOTAL AMOUNT:    ${invoice.formattedTotal()}")
        sb.appendLine("------------------------------------------")
        if (invoice.paymentDetails.isNotBlank()) {
            sb.appendLine("PAYMENT INSTRUCTIONS:")
            sb.appendLine(invoice.paymentDetails)
            sb.appendLine("------------------------------------------")
        }
        if (invoice.notes.isNotBlank()) {
            sb.appendLine("Notes: ${invoice.notes}")
        }
        sb.appendLine("==========================================")
        return sb.toString()
    }
}
