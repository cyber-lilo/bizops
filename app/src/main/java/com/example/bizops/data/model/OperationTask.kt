package com.example.bizops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

enum class TaskStatus {
    BACKLOG, IN_PROGRESS, IN_REVIEW, COMPLETED
}

@Entity(tableName = "operation_tasks")
data class OperationTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Operations", // Operations, Finance, Client Work, Procurement, Legal & Compliance
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.IN_PROGRESS,
    val dueDate: Long = System.currentTimeMillis() + 86400000L * 3, // default 3 days
    val clientId: Long? = null,
    val clientName: String = "",
    val assignedTo: String = "Admin",
    val estimatedHours: Double = 2.0,
    val loggedHours: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
