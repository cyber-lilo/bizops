package com.example.bizops.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bizops.data.model.Invoice
import com.example.bizops.data.model.InvoiceStatus
import com.example.bizops.data.model.TaskStatus
import com.example.bizops.ui.clients.ClientsAndSettingsScreen
import com.example.bizops.ui.emails.EmailTemplatesScreen
import com.example.bizops.ui.invoices.CreateEditInvoiceDialog
import com.example.bizops.ui.invoices.InvoicesScreen
import com.example.bizops.ui.operations.OperationsHubScreen
import com.example.bizops.ui.viewmodel.BizOpsViewModel

enum class NavigationDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    OPERATIONS(
        title = "Operations",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_operations"
    ),
    INVOICES(
        title = "Invoices",
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        testTag = "nav_invoices"
    ),
    EMAILS(
        title = "Email Studio",
        selectedIcon = Icons.Filled.Email,
        unselectedIcon = Icons.Outlined.Email,
        testTag = "nav_emails"
    ),
    CRM_SETTINGS(
        title = "Clients & Org",
        selectedIcon = Icons.Filled.CorporateFare,
        unselectedIcon = Icons.Outlined.CorporateFare,
        testTag = "nav_crm"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BizOpsApp(
    viewModel: BizOpsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(NavigationDestination.OPERATIONS) }
    var preselectedInvoiceForEmail by remember { mutableStateOf<Invoice?>(null) }
    var showGlobalCreateInvoiceSheet by remember { mutableStateOf(false) }

    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val emailTemplates by viewModel.emailTemplates.collectAsStateWithLifecycle()
    val companyProfile by viewModel.companyProfile.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()

    val pendingInvoicesCount = remember(invoices) {
        invoices.count { it.status == InvoiceStatus.SENT || it.status == InvoiceStatus.OVERDUE }
    }
    val activeTasksCount = remember(tasks) {
        tasks.count { it.status != TaskStatus.COMPLETED }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (destination == NavigationDestination.INVOICES && pendingInvoicesCount > 0) {
                                        Badge { Text("$pendingInvoicesCount") }
                                    } else if (destination == NavigationDestination.OPERATIONS && activeTasksCount > 0) {
                                        Badge { Text("$activeTasksCount") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            }
                        },
                        label = { Text(destination.title) },
                        modifier = Modifier.testTag(destination.testTag)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Crossfade(
            targetState = currentDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) { dest ->
            when (dest) {
                NavigationDestination.OPERATIONS -> {
                    OperationsHubScreen(
                        viewModel = viewModel,
                        metrics = metrics,
                        tasks = tasks,
                        clients = clients,
                        invoices = invoices,
                        onNavigateToInvoices = { currentDestination = NavigationDestination.INVOICES },
                        onNavigateToEmails = { currentDestination = NavigationDestination.EMAILS },
                        onNavigateToClients = { currentDestination = NavigationDestination.CRM_SETTINGS },
                        onOpenCreateInvoice = { showGlobalCreateInvoiceSheet = true }
                    )
                }

                NavigationDestination.INVOICES -> {
                    InvoicesScreen(
                        viewModel = viewModel,
                        invoices = invoices,
                        clients = clients,
                        companyProfile = companyProfile,
                        onOpenEmailPersonalizerWithInvoice = { invoice ->
                            preselectedInvoiceForEmail = invoice
                            currentDestination = NavigationDestination.EMAILS
                        }
                    )
                }

                NavigationDestination.EMAILS -> {
                    EmailTemplatesScreen(
                        viewModel = viewModel,
                        templates = emailTemplates,
                        clients = clients,
                        invoices = invoices,
                        companyProfile = companyProfile,
                        initialPreselectedInvoice = preselectedInvoiceForEmail,
                        onClearPreselectedInvoice = { preselectedInvoiceForEmail = null }
                    )
                }

                NavigationDestination.CRM_SETTINGS -> {
                    ClientsAndSettingsScreen(
                        viewModel = viewModel,
                        clients = clients,
                        companyProfile = companyProfile
                    )
                }
            }
        }
    }

    if (showGlobalCreateInvoiceSheet) {
        CreateEditInvoiceDialog(
            invoice = null,
            clients = clients,
            companyProfile = companyProfile,
            onDismiss = { showGlobalCreateInvoiceSheet = false },
            onSave = { newInv ->
                viewModel.saveInvoice(newInv)
                showGlobalCreateInvoiceSheet = false
                currentDestination = NavigationDestination.INVOICES
            }
        )
    }
}
