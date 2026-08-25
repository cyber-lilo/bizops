package com.example.bizops.data.db

import androidx.room.TypeConverter
import com.example.bizops.data.model.EmailCategory
import com.example.bizops.data.model.InvoiceItem
import com.example.bizops.data.model.InvoiceStatus
import com.example.bizops.data.model.TaskPriority
import com.example.bizops.data.model.TaskStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val invoiceItemListType = Types.newParameterizedType(List::class.java, InvoiceItem::class.java)
    private val invoiceItemAdapter = moshi.adapter<List<InvoiceItem>>(invoiceItemListType)

    @TypeConverter
    fun fromInvoiceItemList(items: List<InvoiceItem>?): String {
        return if (items == null) "[]" else invoiceItemAdapter.toJson(items)
    }

    @TypeConverter
    fun toInvoiceItemList(json: String?): List<InvoiceItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            invoiceItemAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromInvoiceStatus(status: InvoiceStatus?): String {
        return status?.name ?: InvoiceStatus.DRAFT.name
    }

    @TypeConverter
    fun toInvoiceStatus(value: String?): InvoiceStatus {
        return try {
            if (value != null) InvoiceStatus.valueOf(value) else InvoiceStatus.DRAFT
        } catch (e: Exception) {
            InvoiceStatus.DRAFT
        }
    }

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority?): String {
        return priority?.name ?: TaskPriority.MEDIUM.name
    }

    @TypeConverter
    fun toTaskPriority(value: String?): TaskPriority {
        return try {
            if (value != null) TaskPriority.valueOf(value) else TaskPriority.MEDIUM
        } catch (e: Exception) {
            TaskPriority.MEDIUM
        }
    }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus?): String {
        return status?.name ?: TaskStatus.IN_PROGRESS.name
    }

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus {
        return try {
            if (value != null) TaskStatus.valueOf(value) else TaskStatus.IN_PROGRESS
        } catch (e: Exception) {
            TaskStatus.IN_PROGRESS
        }
    }

    @TypeConverter
    fun fromEmailCategory(category: EmailCategory?): String {
        return category?.name ?: EmailCategory.OPERATIONS.name
    }

    @TypeConverter
    fun toEmailCategory(value: String?): EmailCategory {
        return try {
            if (value != null) EmailCategory.valueOf(value) else EmailCategory.OPERATIONS
        } catch (e: Exception) {
            EmailCategory.OPERATIONS
        }
    }
}
