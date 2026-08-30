package com.example.bizops.ui.clients

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bizops.data.model.Client
import com.example.bizops.data.model.CompanyProfile
import com.example.bizops.ui.components.EmptyStateView
import com.example.bizops.ui.viewmodel.BizOpsViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class ClientsTab(val title: String) {
    CLIENTS("Client CRM"),
    PROFILE("Company Profile & Ledger Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsAndSettingsScreen(
    viewModel: BizOpsViewModel,
    clients: List<Client>,
    companyProfile: CompanyProfile,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(ClientsTab.CLIENTS) }
    var searchQuery by remember { mutableStateOf("") }

    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    var showClientDialog by remember { mutableStateOf(false) }

    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) clients
        else clients.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.company.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Clients & Company Settings",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${clients.size} CRM accounts • ${companyProfile.companyName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (selectedTab == ClientsTab.CLIENTS) {
                        IconButton(
                            onClick = {
                                clientToEdit = null
                                showClientDialog = true
                            },
                            modifier = Modifier.testTag("add_client_top_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Client", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab == ClientsTab.CLIENTS) {
                ExtendedFloatingActionButton(
                    onClick = {
                        clientToEdit = null
                        showClientDialog = true
                    },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("New Client") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("new_client_fab")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ClientsTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                ClientsTab.CLIENTS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = innerPadding.calculateBottomPadding() + 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search clients by name, company, or email...") },
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
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (filteredClients.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Outlined.People,
                                    title = if (searchQuery.isNotBlank()) "No Clients Found" else "No Clients in CRM",
                                    description = if (searchQuery.isNotBlank()) "Try refining your search keyword" else "Add your client directory for quick invoice generation and automated email communications.",
                                    actionLabel = "+ Add First Client",
                                    onAction = {
                                        clientToEdit = null
                                        showClientDialog = true
                                    }
                                )
                            }
                        } else {
                            items(filteredClients, key = { it.id }) { client ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.deleteClient(client)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Client \"${client.name.ifBlank { client.company }}\" deleted",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.saveClient(client)
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
                                            label = "swipe_delete_client_bg"
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
                                                    contentDescription = "Delete Client",
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
                                    ClientCard(
                                        client = client,
                                        onEdit = {
                                            clientToEdit = client
                                            showClientDialog = true
                                        },
                                        onDelete = {
                                            viewModel.deleteClient(client)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Client \"${client.name.ifBlank { client.company }}\" deleted",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.saveClient(client)
                                                }
                                            }
                                        },
                                        onCall = {
                                            if (client.phone.isNotBlank()) {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))
                                                context.startActivity(intent)
                                            }
                                        },
                                        onEmail = {
                                            if (client.email.isNotBlank()) {
                                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${client.email}"))
                                                context.startActivity(intent)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                ClientsTab.PROFILE -> {
                    CompanyProfileSettingsView(
                        profile = companyProfile,
                        onSave = { updated ->
                            viewModel.saveCompanyProfile(updated)
                            Toast.makeText(context, "Company profile saved!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding() + 16.dp)
                    )
                }
            }
        }
    }

    if (showClientDialog) {
        CreateEditClientDialog(
            client = clientToEdit,
            onDismiss = { showClientDialog = false },
            onSave = { saved ->
                viewModel.saveClient(saved)
                showClientDialog = false
                Toast.makeText(context, "Client saved successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ClientCard(
    client: Client,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.company.ifBlank { client.name },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (client.company.isNotBlank() && client.name.isNotBlank()) {
                        Text(
                            text = "Contact: ${client.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Terms: ${client.paymentTerms}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (client.email.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(client.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (client.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(client.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (client.phone.isNotBlank()) {
                        FilledTonalIconButton(
                            onClick = onCall,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call Client", modifier = Modifier.size(18.dp))
                        }
                    }
                    if (client.email.isNotBlank()) {
                        FilledTonalIconButton(
                            onClick = onEmail,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(Icons.Default.Mail, contentDescription = "Email Client", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Client", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Client", tint = ErrorRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CompanyProfileSettingsView(
    profile: CompanyProfile,
    onSave: (CompanyProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var companyName by remember(profile) { mutableStateOf(profile.companyName) }
    var ownerName by remember(profile) { mutableStateOf(profile.ownerName) }
    var email by remember(profile) { mutableStateOf(profile.email) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var address by remember(profile) { mutableStateOf(profile.address) }
    var taxId by remember(profile) { mutableStateOf(profile.taxId) }
    var defaultCurrency by remember(profile) { mutableStateOf(profile.defaultCurrency) }
    var defaultTaxPercentStr by remember(profile) { mutableStateOf(profile.defaultTaxPercent.toString()) }
    var defaultPaymentTerms by remember(profile) { mutableStateOf(profile.defaultPaymentTerms) }
    var paymentInstructions by remember(profile) { mutableStateOf(profile.paymentInstructions) }

    val currencies = listOf("$", "€", "£", "¥", "₹", "C$", "A$")
    val termsList = listOf("Due on Receipt", "Net 15", "Net 30", "Net 60")

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Business Identity",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner / Representative") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = taxId,
                            onValueChange = { taxId = it },
                            label = { Text("Tax / VAT ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Business Email") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Registered Address") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Invoice Defaults & Bank Wire",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text("Default Currency:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(currencies) { c ->
                            FilterChip(
                                selected = defaultCurrency == c,
                                onClick = { defaultCurrency = c },
                                label = { Text(c) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = defaultTaxPercentStr,
                            onValueChange = { defaultTaxPercentStr = it },
                            label = { Text("Default Tax %") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Default Payment Terms:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(termsList) { t ->
                            FilterChip(
                                selected = defaultPaymentTerms == t,
                                onClick = { defaultPaymentTerms = t },
                                label = { Text(t) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = paymentInstructions,
                        onValueChange = { paymentInstructions = it },
                        label = { Text("Default Wire Transfer / Payment Details") },
                        placeholder = { Text("e.g. Bank of America, Routing #12345, Account #67890") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    val updated = profile.copy(
                        companyName = companyName.trim(),
                        ownerName = ownerName.trim(),
                        email = email.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        taxId = taxId.trim(),
                        defaultCurrency = defaultCurrency,
                        defaultTaxPercent = defaultTaxPercentStr.toDoubleOrNull() ?: 0.0,
                        defaultPaymentTerms = defaultPaymentTerms,
                        paymentInstructions = paymentInstructions.trim()
                    )
                    onSave(updated)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_company_profile_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Company Settings")
            }
        }
    }
}

@Composable
fun CreateEditClientDialog(
    client: Client?,
    onDismiss: () -> Unit,
    onSave: (Client) -> Unit
) {
    var name by remember { mutableStateOf(client?.name ?: "") }
    var company by remember { mutableStateOf(client?.company ?: "") }
    var email by remember { mutableStateOf(client?.email ?: "") }
    var phone by remember { mutableStateOf(client?.phone ?: "") }
    var address by remember { mutableStateOf(client?.address ?: "") }
    var paymentTerms by remember { mutableStateOf(client?.paymentTerms ?: "Net 30") }
    var notes by remember { mutableStateOf(client?.notes ?: "") }

    val termsList = listOf("Due on Receipt", "Net 15", "Net 30", "Net 60")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (client == null) "Add New Client" else "Edit Client",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company / Organization *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Contact Person Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Billing Address") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Payment Terms:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(termsList) { t ->
                            FilterChip(
                                selected = paymentTerms == t,
                                onClick = { paymentTerms = t },
                                label = { Text(t) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Internal CRM Notes") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (company.isNotBlank() || name.isNotBlank()) {
                        val saved = (client ?: Client(name = name)).copy(
                            name = name.trim(),
                            company = company.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            paymentTerms = paymentTerms,
                            notes = notes.trim()
                        )
                        onSave(saved)
                    }
                },
                enabled = company.isNotBlank() || name.isNotBlank()
            ) {
                Text("Save Client")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
