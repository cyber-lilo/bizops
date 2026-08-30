package com.example.bizops.ui.operations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bizops.data.model.Client
import com.example.bizops.data.model.Invoice
import com.example.bizops.data.model.InvoiceStatus
import com.example.bizops.data.model.OperationTask
import com.example.bizops.data.model.TaskPriority
import com.example.bizops.data.model.TaskStatus
import com.example.bizops.ui.components.EmptyStateView
import com.example.bizops.ui.components.MonthlyInvoiceChartCard
import com.example.bizops.ui.components.TaskPriorityBadge
import com.example.bizops.ui.components.TaskStatusChip
import com.example.bizops.ui.viewmodel.BizOpsViewModel
import com.example.bizops.ui.viewmodel.OperationsMetrics
import com.example.ui.theme.*
import kotlinx.coroutines.launch
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
    onNavigateToInvoices: (InvoiceStatus?) -> Unit,
    onNavigateToEmails: (Invoice?) -> Unit,
    onNavigateToClients: () -> Unit,
    onOpenCreateInvoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
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

    val paidInvoicesCount = remember(invoices) { invoices.count { it.status == InvoiceStatus.PAID } }
    val sentInvoicesCount = remember(invoices) { invoices.count { it.status == InvoiceStatus.SENT } }
    val draftInvoicesCount = remember(invoices) { invoices.count { it.status == InvoiceStatus.DRAFT } }
    val overdueInvoicesCount = remember(invoices) { invoices.count { it.status == InvoiceStatus.OVERDUE } }
    val totalBilled = remember(metrics) { metrics.totalRevenue + metrics.pendingRevenue + metrics.overdueRevenue }
    val collectionRate = remember(totalBilled, metrics.totalRevenue) {
        if (totalBilled > 0) ((metrics.totalRevenue / totalBilled) * 100).toInt() else 100
    }
    val earliestUnpaidInvoice = remember(invoices) {
        invoices.filter { it.status == InvoiceStatus.SENT || it.status == InvoiceStatus.OVERDUE }
            .minByOrNull { it.dueDate }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlue.copy(alpha = 0.14f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = "Diamonds OPS Logo",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Diamonds OPS",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.2).sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = PrimaryBlue.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "INTERNAL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = PrimaryBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Enterprise Ledger, Invoicing & Operations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenCreateInvoice,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hub_create_invoice_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Addchart,
                            contentDescription = "New Invoice",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            taskToEdit = null
                            showTaskDialog = true
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("add_task_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
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
                text = { Text("New Ops Task", fontWeight = FontWeight.SemiBold) },
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
            // --- 0. Internal Operator Welcome Banner ---
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Operator Console",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${clients.size} CRM Accounts • ${tasks.count { it.status != TaskStatus.COMPLETED }} Active Tasks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Systems Active",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // --- 1. CORE SECTION: QUICK ACCESS CARDS ---
            item {
                Text(
                    text = "Core Modules & Quick Access",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // === CARD 1: INVOICE MANAGEMENT ===
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 6.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_invoice_management")
                        .clickable { onNavigateToInvoices(null) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Card Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PrimaryBlue.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = "Invoice Management",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Invoice Management",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Create, style, digitally sign & export PDFs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = PrimaryBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${invoices.size} Invoices",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status Distribution Pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatusMiniChip(
                                label = "$paidInvoicesCount Paid",
                                color = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                            StatusMiniChip(
                                label = "$sentInvoicesCount Sent",
                                color = PrimaryBlue,
                                modifier = Modifier.weight(1f)
                            )
                            StatusMiniChip(
                                label = "$draftInvoicesCount Drafts",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (overdueInvoicesCount > 0) {
                                StatusMiniChip(
                                    label = "$overdueInvoicesCount Overdue",
                                    color = ErrorRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Features hint
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "7 Document Layouts • Canvas Signature • Client Logos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onOpenCreateInvoice,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("quick_create_invoice_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Invoice", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { onNavigateToInvoices(null) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("quick_view_invoices_btn")
                            ) {
                                Text("Open Manager", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // === CARD 2: BILLING TRACKING ===
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 6.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_billing_tracking")
                        .clickable { onNavigateToInvoices(null) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Card Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SuccessGreen.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = "Billing Tracking",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Billing Tracking",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Receivables, collection rate & financial cashflow",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$collectionRate% Collected",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Financial Metrics Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Settled Revenue",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreen
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${metrics.currency}${String.format(Locale.US, "%,.0f", metrics.totalRevenue)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = SuccessGreen
                                    )
                                }
                            }

                            Surface(
                                color = (if (metrics.overdueRevenue > 0) WarningAmber else PrimaryBlue).copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, (if (metrics.overdueRevenue > 0) WarningAmber else PrimaryBlue).copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Pending Receivables",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (metrics.overdueRevenue > 0) WarningAmber else PrimaryBlue
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${metrics.currency}${String.format(Locale.US, "%,.0f", metrics.pendingRevenue + metrics.overdueRevenue)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (metrics.overdueRevenue > 0) WarningAmber else PrimaryBlue
                                    )
                                }
                            }
                        }

                        // Overdue Alert Banner if present
                        if (metrics.overdueRevenue > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = ErrorRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToInvoices(InvoiceStatus.OVERDUE) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = ErrorRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${metrics.currency}${String.format(Locale.US, "%,.0f", metrics.overdueRevenue)} past due ($overdueInvoicesCount invoices)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ErrorRed
                                        )
                                    }
                                    Text(
                                        text = "Filter Overdue →",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ErrorRed
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onNavigateToInvoices(null) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("quick_billing_ledger_btn")
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Inspect Ledger", fontWeight = FontWeight.SemiBold)
                            }

                            if (overdueInvoicesCount > 0) {
                                OutlinedButton(
                                    onClick = { onNavigateToInvoices(InvoiceStatus.OVERDUE) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Text("Overdue ($overdueInvoicesCount)", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onNavigateToInvoices(InvoiceStatus.SENT) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Text("Pending ($sentInvoicesCount)", color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // === CARD 3: EMAIL GENERATOR FEATURE ===
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, InfoPurple.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 6.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_email_generator")
                        .clickable { onNavigateToEmails(null) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Card Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(InfoPurple.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Email Generator",
                                        tint = InfoPurple,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Email Generator",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "AI-assisted client drafts, reminders & follow-ups",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = InfoPurple.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "AI Powered",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = InfoPurple
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Email Generator capabilities tags
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Payment Due Reminders",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Receipts & Thanks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Custom AI Prompt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onNavigateToEmails(null) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = InfoPurple),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("quick_launch_email_studio_btn")
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Launch Studio", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onNavigateToEmails(earliestUnpaidInvoice)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, InfoPurple.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("quick_draft_reminder_btn")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = InfoPurple, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Draft Reminder", color = InfoPurple, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // --- 2. Monthly Invoices & Billing Visual Chart ---
            item {
                Text(
                    text = "Financial Trends & Historical Billings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                MonthlyInvoiceChartCard(
                    invoices = invoices,
                    currencySymbol = metrics.currency,
                    onNavigateToInvoices = { onNavigateToInvoices(null) }
                )
            }

            // --- 3. Internal Operations Tasks Section ---
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Internal Ops Tasks & Deliverables",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            taskToEdit = null
                            showTaskDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add Task")
                    }
                }
            }

            // Search Bar for Tasks
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search internal tasks by title, client, or category...") },
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

            // Task Status Filter Chips
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

            // Operations Task List Items
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
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteTask(task)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Task \"${task.title}\" deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.saveTask(task)
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) ErrorRed else ErrorRed.copy(alpha = 0.85f),
                                label = "swipe_delete_task_bg"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Task",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Delete",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Task \"${task.title}\" deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.saveTask(task)
                                    }
                                }
                            }
                        )
                    }
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
private fun StatusMiniChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
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
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
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
