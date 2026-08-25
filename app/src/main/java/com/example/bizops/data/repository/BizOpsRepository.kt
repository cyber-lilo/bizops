package com.example.bizops.data.repository

import com.example.bizops.data.db.BizOpsDatabase
import com.example.bizops.data.model.Client
import com.example.bizops.data.model.CompanyProfile
import com.example.bizops.data.model.EmailCategory
import com.example.bizops.data.model.EmailTemplate
import com.example.bizops.data.model.Invoice
import com.example.bizops.data.model.InvoiceStatus
import com.example.bizops.data.model.OperationTask
import com.example.bizops.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BizOpsRepository(private val database: BizOpsDatabase) {

    // --- Clients ---
    val allClients: Flow<List<Client>> = database.clientDao().getAllClients()

    suspend fun getClientById(id: Long): Client? = database.clientDao().getClientById(id)

    suspend fun insertClient(client: Client): Long = database.clientDao().insertClient(client)

    suspend fun updateClient(client: Client) = database.clientDao().updateClient(client)

    suspend fun deleteClient(client: Client) = database.clientDao().deleteClient(client)

    suspend fun deleteClientById(id: Long) = database.clientDao().deleteClientById(id)

    // --- Operation Tasks ---
    val allTasks: Flow<List<OperationTask>> = database.taskDao().getAllTasks()

    fun getTasksByStatus(status: TaskStatus): Flow<List<OperationTask>> =
        database.taskDao().getTasksByStatus(status)

    suspend fun getTaskById(id: Long): OperationTask? = database.taskDao().getTaskById(id)

    suspend fun insertTask(task: OperationTask): Long = database.taskDao().insertTask(task)

    suspend fun updateTask(task: OperationTask) = database.taskDao().updateTask(task)

    suspend fun deleteTask(task: OperationTask) = database.taskDao().deleteTask(task)

    suspend fun updateTaskStatus(id: Long, status: TaskStatus) {
        val completedAt = if (status == TaskStatus.COMPLETED) System.currentTimeMillis() else null
        database.taskDao().updateTaskStatus(id, status, completedAt)
    }

    // --- Invoices ---
    val allInvoices: Flow<List<Invoice>> = database.invoiceDao().getAllInvoices()

    suspend fun getInvoiceById(id: Long): Invoice? = database.invoiceDao().getInvoiceById(id)

    fun getInvoicesForClient(clientId: Long): Flow<List<Invoice>> =
        database.invoiceDao().getInvoicesForClient(clientId)

    suspend fun insertInvoice(invoice: Invoice): Long = database.invoiceDao().insertInvoice(invoice)

    suspend fun updateInvoice(invoice: Invoice) = database.invoiceDao().updateInvoice(invoice)

    suspend fun deleteInvoice(invoice: Invoice) = database.invoiceDao().deleteInvoice(invoice)

    suspend fun deleteInvoiceById(id: Long) = database.invoiceDao().deleteInvoiceById(id)

    suspend fun markInvoiceAsPaid(invoice: Invoice) {
        val updated = invoice.copy(
            status = InvoiceStatus.PAID,
            paidDate = System.currentTimeMillis()
        )
        database.invoiceDao().updateInvoice(updated)
    }

    suspend fun markInvoiceAsSent(invoice: Invoice) {
        val updated = invoice.copy(status = InvoiceStatus.SENT)
        database.invoiceDao().updateInvoice(updated)
    }

    // --- Email Templates ---
    val allTemplates: Flow<List<EmailTemplate>> = database.emailTemplateDao().getAllTemplates()

    fun getTemplatesByCategory(category: EmailCategory): Flow<List<EmailTemplate>> {
        return if (category == EmailCategory.ALL) {
            allTemplates
        } else {
            allTemplates.map { list -> list.filter { it.category == category } }
        }
    }

    suspend fun getTemplateById(id: Long): EmailTemplate? =
        database.emailTemplateDao().getTemplateById(id)

    suspend fun insertTemplate(template: EmailTemplate): Long =
        database.emailTemplateDao().insertTemplate(template)

    suspend fun updateTemplate(template: EmailTemplate) =
        database.emailTemplateDao().updateTemplate(template)

    suspend fun deleteTemplate(template: EmailTemplate) =
        database.emailTemplateDao().deleteTemplate(template)

    suspend fun recordTemplateUsage(id: Long) =
        database.emailTemplateDao().incrementTemplateUsage(id)

    // --- Company Profile ---
    val companyProfile: Flow<CompanyProfile?> = database.companyProfileDao().getProfile()

    suspend fun getCompanyProfileSync(): CompanyProfile? =
        database.companyProfileDao().getProfileSync()

    suspend fun saveCompanyProfile(profile: CompanyProfile) =
        database.companyProfileDao().insertOrUpdateProfile(profile)
}
