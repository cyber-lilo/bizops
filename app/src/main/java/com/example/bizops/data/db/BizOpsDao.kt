package com.example.bizops.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bizops.data.model.Client
import com.example.bizops.data.model.CompanyProfile
import com.example.bizops.data.model.EmailTemplate
import com.example.bizops.data.model.Invoice
import com.example.bizops.data.model.OperationTask
import com.example.bizops.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Long): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteClientById(id: Long)
}

@Dao
interface OperationTaskDao {
    @Query("SELECT * FROM operation_tasks ORDER BY dueDate ASC, priority DESC")
    fun getAllTasks(): Flow<List<OperationTask>>

    @Query("SELECT * FROM operation_tasks WHERE status = :status ORDER BY dueDate ASC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<OperationTask>>

    @Query("SELECT * FROM operation_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): OperationTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: OperationTask): Long

    @Update
    suspend fun updateTask(task: OperationTask)

    @Delete
    suspend fun deleteTask(task: OperationTask)

    @Query("UPDATE operation_tasks SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateTaskStatus(id: Long, status: TaskStatus, completedAt: Long?)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): Invoice?

    @Query("SELECT * FROM invoices WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getInvoicesForClient(clientId: Long): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Long)
}

@Dao
interface EmailTemplateDao {
    @Query("SELECT * FROM email_templates ORDER BY isCustom DESC, usageCount DESC, title ASC")
    fun getAllTemplates(): Flow<List<EmailTemplate>>

    @Query("SELECT * FROM email_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): EmailTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: EmailTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<EmailTemplate>)

    @Update
    suspend fun updateTemplate(template: EmailTemplate)

    @Delete
    suspend fun deleteTemplate(template: EmailTemplate)

    @Query("UPDATE email_templates SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun incrementTemplateUsage(id: Long, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface CompanyProfileDao {
    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<CompanyProfile?>

    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): CompanyProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: CompanyProfile)
}
