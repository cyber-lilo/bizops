package com.example.bizops.ui.operations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bizops.data.model.Client
import com.example.bizops.data.model.Invoice
import com.example.bizops.data.model.OperationTask
import com.example.bizops.data.model.TaskPriority
import com.example.bizops.data.model.TaskStatus
import com.example.bizops.ui.components.EmptyStateView
import com.example.bizops.ui.components.MetricStatCard
import com.example.bizops.ui.components.MonthlyInvoiceChartCard
import com.example.bizops.ui.components.TaskPriorityBadge
import com.example.bizops.ui.components.TaskStatusChip
import com.example.bizops.ui.viewmodel.BizOpsViewModel
import com.example.bizops.ui.viewmodel.OperationsMetrics
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsHubScreen(
    viewModel: BizOpsViewModel,
    metrics: OperationsMetrics,
    tasks: List<OperationTask>,
    clients: List<Client>,
    invoices: List<Invoice>,
    onNavigateToInvoices: () -> Unit,
    onNavigateToEmails: () -> Unit,
    onNavigateToClients: () -> Unit,
    onOpenCreateInvoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStatusFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var taskToEdit by remember { mutableStateOf<OperationTask?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }

    val filteredTasks = remember(tasks, selectedStatusFilter, searchQuery) {
        tasks.filter { task ->
            val matchesStatus = selectedStatusFilter == null || task.status == selectedStatusFilter
            val matchesQuery = searchQuery.isBlank() ||
                    task.title.contains(searchQuery, ignoreCase = true) ||
                    task.description.contains(searchQuery, ignoreCase = true) ||
                    task.category.contains(searchQuery, ignoreCase = true) ||
                    task.clientName.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Operations System",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Real-time workflow, ledger & task ops",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            taskToEdit = null
                            showTaskDialog = true
                        },
                        modifier = Modifier.testTag("add_task_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showTaskDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Ops Task") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("new_task_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // --- 1. Executive Operations Metrics Carousel ---
            item {
                Text(
                    text = "Executive Ledger & Ops Health",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        MetricStatCard(
                            title = "Settled Revenue",
                            value = "${metrics.currency}${String.format(Locale.US, "%,.0f", metrics.totalRevenue)}",
                            subtitle = "Paid Invoices",
                            icon = Icons.Default.Payments,
                            gradientColors = listOf(SuccessGreen, Color(0xFF10B981)),
                            modifier = Modifier.width(170.dp)
                        )
                    }
                    item {
                        MetricStatCard(
                            title = "Pending Invoices",
                            value = "${metrics.currency}${String.format(Locale.US, "%,.0f", metrics.pendingRevenue + metrics.overdueRevenue)}",
                            subtitle = if (metrics.overdueRevenue > 0) "${metrics.currency}${String.format(Locale.US, "%,.0f", metrics.overdueRevenue)} Overdue" else "Awaiting payment",
                            icon = Icons.Default.ReceiptLong,
                            gradientColors = if (metrics.overdueRevenue > 0) listOf(ErrorRed, WarningAmber) else listOf(PrimaryBlue, AccentTeal),
                            modifier = Modifier.width(170.dp)
                        )
                    }
                    item {
                        MetricStatCard(
                            title = "Active Ops Tasks",
                            value = "${metrics.activeTasksCount}",
                            subtitle = "${metrics.completionRatePercent}% SLA completed",
                            icon = Icons.Default.Assignment,
                            gradientColors = listOf(InfoPurple, PrimaryBlue),
                            modifier = Modifier.width(160.dp)
                        )
                    }
                    item {
                        MetricStatCard(
                            title = "Clients Managed",
                            value = "${metrics.totalClientsCount}",
                            subtitle = "Active CRM accounts",
                            icon = Icons.Default.CorporateFare,
                            gradientColors = listOf(AccentTeal, PrimaryBlue),
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
            }

            // --- 2. Monthly Invoices Visual Chart ---
            item {
                MonthlyInvoiceChartCard(
                    invoices = invoices,
                    currencySymbol = metrics.currency,
                    onNavigateToInvoices = onNavigateToInvoices
                )
            }

            // --- 3. Quick Operations Action Hub ---
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.Addchart,
                                label = "+ Invoice",
                                containerColor = PrimaryBlue.copy(alpha = 0.12f),
                                contentColor = PrimaryBlue,
                                onClick = onOpenCreateInvoice
                            )
                            QuickActionButton(
                                icon = Icons.Default.AddTask,
                                label = "+ Task",
                                containerColor = AccentTeal.copy(alpha = 0.12f),
                                contentColor = AccentTeal,
                                onClick = {
                                    taskToEdit = null
                                    showTaskDialog = true
                                }
                            )
                            QuickActionButton(
                                icon = Icons.Default.AutoAwesome,
                                label = "AI Email",
                                containerColor = InfoPurple.copy(alpha = 0.12f),
                                contentColor = InfoPurple,
                                onClick = onNavigateToEmails
                            )
                            QuickActionButton(
                                icon = Icons.Default.PersonAdd,
                                label = "+ Client",
                                containerColor = WarningAmber.copy(alpha = 0.12f),
                                contentColor = WarningAmber,
                                onClick = onNavigateToClients
                            )
                        }
                    }
                }
            }

            // --- 3. Search & Workflow Status Tabs ---
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tasks by title, client, or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_search_field")
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == null,
                            onClick = { selectedStatusFilter = null },
                            label = { Text("All (${tasks.size})") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    TaskStatus.values().forEach { status ->
                        val count = tasks.count { it.status == status }
                        val label = when (status) {
                            TaskStatus.BACKLOG -> "Backlog"
                            TaskStatus.IN_PROGRESS -> "In Progress"
                            TaskStatus.IN_REVIEW -> "Review"
                            TaskStatus.COMPLETED -> "Done"
                        }
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == status,
                                onClick = { selectedStatusFilter = status },
                                label = { Text("$label ($count)") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // --- 4. Operations Task List ---
            if (filteredTasks.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.Assignment,
                        title = if (searchQuery.isNotBlank()) "No Matching Tasks" else "No Operations Tasks",
                        description = if (searchQuery.isNotBlank()) "Try refining your search keyword" else "Track project deliverables, audits, and operational milestones.",
                        actionLabel = if (searchQuery.isBlank()) "+ Add First Task" else null,
                        onAction = {
                            taskToEdit = null
                            showTaskDialog = true
                        }
                    )
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    OperationTaskCard(
                        task = task,
                        onStatusChange = { newStatus ->
                            viewModel.updateTaskStatus(task.id, newStatus)
                        },
                        onEdit = {
                            taskToEdit = task
                            showTaskDialog = true
                        },
                        onDelete = {
                            viewModel.deleteTask(task)
                        }
                    )
                }
            }
        }
    }

    if (showTaskDialog) {
        CreateEditTaskDialog(
            task = taskToEdit,
            clients = clients,
            onDismiss = { showTaskDialog = false },
            onSave = { savedTask ->
                viewModel.saveTask(savedTask)
                showTaskDialog = false
            }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun OperationTaskCard(
    task: OperationTask,
    onStatusChange: (TaskStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.US) }
    val isPastDue = task.dueDate < System.currentTimeMillis() && task.status != TaskStatus.COMPLETED

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = task.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TaskPriorityBadge(priority = task.priority)
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Status chip with switcher dropdown
                Box {
                    TaskStatusChip(
                        status = task.status,
                        onClick = { showStatusMenu = true }
                    )
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        TaskStatus.values().forEach { statusOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (statusOption) {
                                            TaskStatus.BACKLOG -> "Backlog"
                                            TaskStatus.IN_PROGRESS -> "In Progress"
                                            TaskStatus.IN_REVIEW -> "In Review"
                                            TaskStatus.COMPLETED -> "Mark Completed"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    if (task.status == statusOption) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    onStatusChange(statusOption)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }

                // Due date and client tags
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.clientName.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = task.clientName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = 90.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (isPastDue) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = dateFormat.format(Date(task.dueDate)),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isPastDue) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isPastDue) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTaskDialog(
    task: OperationTask?,
    clients: List<Client>,
    onDismiss: () -> Unit,
    onSave: (OperationTask) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var category by remember { mutableStateOf(task?.category ?: "Operations") }
    var priority by remember { mutableStateOf(task?.priority ?: TaskPriority.MEDIUM) }
    var status by remember { mutableStateOf(task?.status ?: TaskStatus.IN_PROGRESS) }
    var assignedTo by remember { mutableStateOf(task?.assignedTo ?: "Admin") }
    var selectedClient by remember { mutableStateOf<Client?>(clients.find { it.id == task?.clientId }) }
    var estimatedHoursStr by remember { mutableStateOf((task?.estimatedHours ?: 2.0).toString()) }

    val categories = listOf("Operations", "Finance", "Client Work", "Compliance", "Procurement", "Marketing", "Engineering")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (task == null) "New Operations Task" else "Edit Operations Task",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title *") },
                        placeholder = { Text("e.g., Q3 Financial Audit & Reconcile") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Notes") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskPriority.values().forEach { prio ->
                            FilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = { Text(prio.name) }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskStatus.values().forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.name.replace("_", " ")) }
                            )
                        }
                    }
                }

                if (clients.isNotEmpty()) {
                    item {
                        Text(
                            text = "Linked Client (Optional)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedClient == null,
                                    onClick = { selectedClient = null },
                                    label = { Text("None (Internal)") }
                                )
                            }
                            items(clients) { client ->
                                FilterChip(
                                    selected = selectedClient?.id == client.id,
                                    onClick = { selectedClient = client },
                                    label = { Text(client.company.ifBlank { client.name }) }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = assignedTo,
                            onValueChange = { assignedTo = it },
                            label = { Text("Assignee") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = estimatedHoursStr,
                            onValueChange = { estimatedHoursStr = it },
                            label = { Text("Est. Hours") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val hours = estimatedHoursStr.toDoubleOrNull() ?: 2.0
                        val updated = (task ?: OperationTask(title = title)).copy(
                            title = title.trim(),
                            description = description.trim(),
                            category = category,
                            priority = priority,
                            status = status,
                            assignedTo = assignedTo.trim(),
                            estimatedHours = hours,
                            clientId = selectedClient?.id,
                            clientName = selectedClient?.company?.ifBlank { selectedClient?.name } ?: ""
                        )
                        onSave(updated)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
