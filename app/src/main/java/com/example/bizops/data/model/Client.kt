package com.example.bizops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val company: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val taxId: String = "",
    val paymentTerms: String = "Net 30",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
