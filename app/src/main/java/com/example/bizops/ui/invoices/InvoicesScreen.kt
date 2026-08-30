package com.example.bizops.ui.invoices

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bizops.data.model.Client
import com.example.bizops.data.model.CompanyProfile
import com.example.bizops.data.model.Invoice
import com.example.bizops.data.model.InvoiceItem
import com.example.bizops.data.model.InvoiceStatus
import com.example.bizops.data.model.InvoiceTemplateStyle
import com.example.bizops.ui.components.DigitalSignatureCanvas
import com.example.bizops.ui.components.DigitalSignaturePad
import com.example.bizops.ui.components.EmptyStateView
import com.example.bizops.ui.components.LogoPickerDialog
import com.example.bizops.ui.components.SignaturePadDialog
import com.example.bizops.ui.components.StatusBadge
import com.example.bizops.ui.invoices.InvoiceStyleSelectionDialog
import com.example.bizops.ui.invoices.StyledLiveInvoicePreview
import com.example.bizops.ui.viewmodel.BizOpsViewModel
import com.example.bizops.util.InvoicePdfExporter
import com.example.bizops.util.LogoPresetManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    viewModel: BizOpsViewModel,
    invoices: List<Invoice>,
    clients: List<Client>,
    companyProfile: CompanyProfile,
    onOpenEmailPersonalizerWithInvoice: (Invoice) -> Unit,
    initialStatusFilter: InvoiceStatus? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedStatusFilter by remember(initialStatusFilter) { mutableStateOf<InvoiceStatus?>(initialStatusFilter) }
    var searchQuery by remember { mutableStateOf("") }

    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    var previewInvoice by remember { mutableStateOf<Invoice?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    var styleSelectionInvoice by remember { mutableStateOf<Invoice?>(null) }
    var showStyleSelectionDialog by remember { mutableStateOf(false) }

    val filteredInvoices = remember(invoices, selectedStatusFilter, searchQuery) {
        invoices.filter { inv ->
            val matchesStatus = selectedStatusFilter == null || inv.status == selectedStatusFilter
            val matchesQuery = searchQuery.isBlank() ||
                    inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.clientName.contains(searchQuery, ignoreCase = true) ||
                    inv.clientCompany.contains(searchQuery, ignoreCase = true) ||
                    inv.items.any { it.description.contains(searchQuery, ignoreCase = true) }
            matchesStatus && matchesQuery
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Invoice Maker & Ledger",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${invoices.size} Invoices • ${companyProfile.defaultCurrency}${String.format(Locale.US, "%,.2f", invoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.totalAmount })} collected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            invoiceToEdit = null
                            showEditDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("create_invoice_top_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create")
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
                    invoiceToEdit = null
                    showEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Invoice") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("new_invoice_fab")
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Search field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by #INV, client name, or item...") },
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

            // Filter Chips Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == null,
                            onClick = { selectedStatusFilter = null },
                            label = { Text("All (${invoices.size})") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    InvoiceStatus.values().forEach { st ->
                        val count = invoices.count { it.status == st }
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == st,
                                onClick = { selectedStatusFilter = st },
                                label = { Text("${st.name.lowercase().capitalize(Locale.ROOT)} ($count)") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Invoices List
            if (filteredInvoices.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.ReceiptLong,
                        title = if (searchQuery.isNotBlank()) "No Matching Invoices" else "No Invoices Yet",
                        description = if (searchQuery.isNotBlank()) "Try another search keyword" else "Create and send your first professional invoice with line items, tax, and terms.",
                        actionLabel = if (searchQuery.isBlank()) "+ Create First Invoice" else null,
                        onAction = {
                            invoiceToEdit = null
                            showEditDialog = true
                        }
                    )
                }
            } else {
                items(filteredInvoices, key = { it.id }) { invoice ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteInvoice(invoice)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Invoice ${invoice.invoiceNumber} deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.saveInvoice(invoice)
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
                                label = "swipe_delete_invoice_bg"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
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
                                        contentDescription = "Delete Invoice",
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
                        InvoiceCard(
                            invoice = invoice,
                            onPreview = {
                                previewInvoice = invoice
                                showPreviewDialog = true
                            },
                            onChooseStyle = {
                                styleSelectionInvoice = invoice
                                showStyleSelectionDialog = true
                            },
                            onEdit = {
                                invoiceToEdit = invoice
                                showEditDialog = true
                            },
                            onSendEmail = {
                                onOpenEmailPersonalizerWithInvoice(invoice)
                            },
                            onExportPdf = {
                                InvoicePdfExporter.sharePdf(context, invoice, companyProfile)
                            },
                            onSavePdf = {
                                InvoicePdfExporter.savePdfToDownloads(context, invoice, companyProfile)
                            },
                            onMarkStatus = { newStatus ->
                                viewModel.updateInvoiceStatus(invoice, newStatus)
                            },
                            onDuplicate = {
                                viewModel.duplicateInvoice(invoice)
                                Toast.makeText(context, "Invoice duplicated", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deleteInvoice(invoice)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Invoice ${invoice.invoiceNumber} deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.saveInvoice(invoice)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Create / Edit Invoice Dialog ---
    if (showEditDialog) {
        CreateEditInvoiceDialog(
            invoice = invoiceToEdit,
            clients = clients,
            companyProfile = companyProfile,
            onDismiss = { showEditDialog = false },
            onSave = { savedInvoice ->
                viewModel.saveInvoice(savedInvoice)
                showEditDialog = false
                Toast.makeText(context, "Invoice saved successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- Printable Invoice Preview Dialog ---
    if (showPreviewDialog && previewInvoice != null) {
        InvoicePreviewDialog(
            invoice = previewInvoice!!,
            companyProfile = companyProfile,
            onDismiss = { showPreviewDialog = false },
            onSendEmail = {
                showPreviewDialog = false
                onOpenEmailPersonalizerWithInvoice(previewInvoice!!)
            },
            onExportPdf = {
                InvoicePdfExporter.sharePdf(context, previewInvoice!!, companyProfile)
            },
            onSavePdf = {
                InvoicePdfExporter.savePdfToDownloads(context, previewInvoice!!, companyProfile)
            },
            onShare = {
                val shareText = viewModel.formatInvoiceAsShareableText(previewInvoice!!, companyProfile)
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share Invoice ${previewInvoice!!.invoiceNumber}")
                context.startActivity(shareIntent)
            },
            onMarkPaid = {
                viewModel.updateInvoiceStatus(previewInvoice!!, InvoiceStatus.PAID)
                previewInvoice = previewInvoice!!.copy(status = InvoiceStatus.PAID, paidDate = System.currentTimeMillis())
                Toast.makeText(context, "Marked as Paid!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- Pre-defined Invoice Style Selection Screen ---
    if (showStyleSelectionDialog && styleSelectionInvoice != null) {
        InvoiceStyleSelectionDialog(
            invoice = styleSelectionInvoice!!,
            companyProfile = companyProfile,
            onDismiss = { showStyleSelectionDialog = false },
            onStyleSelected = { selectedStyle ->
                val updated = styleSelectionInvoice!!.copy(templateStyle = selectedStyle.id)
                viewModel.saveInvoice(updated)
                styleSelectionInvoice = updated
                Toast.makeText(context, "Applied style: ${selectedStyle.title}", Toast.LENGTH_SHORT).show()
            },
            onGeneratePdf = { selectedStyle ->
                val updated = styleSelectionInvoice!!.copy(templateStyle = selectedStyle.id)
                viewModel.saveInvoice(updated)
                InvoicePdfExporter.sharePdf(context, updated, companyProfile)
                showStyleSelectionDialog = false
            }
        )
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    onPreview: () -> Unit,
    onChooseStyle: () -> Unit,
    onEdit: () -> Unit,
    onSendEmail: () -> Unit,
    onExportPdf: () -> Unit,
    onSavePdf: () -> Unit,
    onMarkStatus: (InvoiceStatus) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val currentStyle = remember(invoice.templateStyle) {
        InvoiceTemplateStyle.fromId(invoice.templateStyle)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPreview)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = currentStyle.primaryColor
                        )
                    )
                    Text(
                        text = "Issued: ${dateFormat.format(Date(invoice.issueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Style badge
                    Surface(
                        color = currentStyle.primaryColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onChooseStyle)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(currentStyle.primaryColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentStyle.tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = currentStyle.primaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(status = invoice.status)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View & Preview") },
                                leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onPreview()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Choose Layout & Style") },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = currentStyle.primaryColor) },
                                onClick = {
                                    showMenu = false
                                    onChooseStyle()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export & Share PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PrimaryBlue) },
                                onClick = {
                                    showMenu = false
                                    onExportPdf()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save PDF to Downloads") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onSavePdf()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Send via Email Studio") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onSendEmail()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Invoice") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                }
                            )
                            HorizontalDivider()
                            if (invoice.status != InvoiceStatus.PAID) {
                                DropdownMenuItem(
                                    text = { Text("Mark as Paid") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen) },
                                    onClick = {
                                        showMenu = false
                                        onMarkStatus(InvoiceStatus.PAID)
                                    }
                                )
                            }
                            if (invoice.status == InvoiceStatus.DRAFT) {
                                DropdownMenuItem(
                                    text = { Text("Mark as Sent") },
                                    leadingIcon = { Icon(Icons.Default.Send, contentDescription = null, tint = PrimaryBlue) },
                                    onClick = {
                                        showMenu = false
                                        onMarkStatus(InvoiceStatus.SENT)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = ErrorRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Client and Items summary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.clientCompany.ifBlank { invoice.clientName.ifBlank { "Unassigned Client" } },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${invoice.items.size} line items • Due ${dateFormat.format(Date(invoice.dueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = invoice.formattedTotal(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditInvoiceDialog(
    invoice: Invoice?,
    clients: List<Client>,
    companyProfile: CompanyProfile,
    onDismiss: () -> Unit,
    onSave: (Invoice) -> Unit
) {
    val context = LocalContext.current
    val defaultNumber = remember {
        "INV-" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()) + "-" + (100..999).random()
    }

    var invoiceNumber by remember { mutableStateOf(invoice?.invoiceNumber ?: defaultNumber) }
    var selectedClient by remember { mutableStateOf<Client?>(clients.find { it.id == invoice?.clientId }) }
    var clientName by remember { mutableStateOf(invoice?.clientName ?: "") }
    var clientCompany by remember { mutableStateOf(invoice?.clientCompany ?: "") }
    var clientEmail by remember { mutableStateOf(invoice?.clientEmail ?: "") }
    var clientAddress by remember { mutableStateOf(invoice?.clientAddress ?: "") }

    // Logos & Branding
    var companyLogo by remember { mutableStateOf(invoice?.companyLogo ?: "preset_apex") }
    var clientLogo by remember { mutableStateOf(invoice?.clientLogo ?: "preset_client_summit") }
    var showCompanyLogoPicker by remember { mutableStateOf(false) }
    var showClientLogoPicker by remember { mutableStateOf(false) }

    // Pre-defined Style & Layout
    var templateStyle by remember { mutableStateOf(invoice?.templateStyle ?: InvoiceTemplateStyle.MODERN_EXECUTIVE.id) }

    // Digital Signature
    var signatureData by remember { mutableStateOf(invoice?.signatureData) }
    var signatoryName by remember { mutableStateOf(invoice?.signatoryName ?: companyProfile.ownerName.ifBlank { "Authorized Officer" }) }
    var signatoryTitle by remember { mutableStateOf(invoice?.signatoryTitle ?: "Managing Director") }
    var showSignaturePad by remember { mutableStateOf(false) }

    // Financial & Meta
    var currency by remember { mutableStateOf(invoice?.currency ?: companyProfile.defaultCurrency) }
    var taxPercentStr by remember { mutableStateOf((invoice?.taxPercent ?: companyProfile.defaultTaxPercent).toString()) }
    var discountPercentStr by remember { mutableStateOf((invoice?.discountPercent ?: 0.0).toString()) }
    var shippingStr by remember { mutableStateOf((invoice?.shippingOrFee ?: 0.0).toString()) }
    var status by remember { mutableStateOf(invoice?.status ?: InvoiceStatus.DRAFT) }
    var notes by remember { mutableStateOf(invoice?.notes ?: "Thank you for your business! Payment is requested within terms.") }
    var paymentDetails by remember { mutableStateOf(invoice?.paymentDetails ?: companyProfile.paymentInstructions) }
    var terms by remember { mutableStateOf(invoice?.termsAndConditions ?: "Net ${companyProfile.defaultPaymentTerms} terms apply.") }

    var items by remember {
        mutableStateOf(
            if (invoice?.items?.isNotEmpty() == true) invoice.items
            else listOf(InvoiceItem(description = "Operations Consulting Services", quantity = 1.0, unitPrice = 500.0))
        )
    }

    var previewInvoiceModal by remember { mutableStateOf<Invoice?>(null) }

    val currencies = listOf("$", "€", "£", "¥", "₹", "C$", "A$")

    // Calculations
    val subtotal = items.sumOf { it.total }
    val taxP = taxPercentStr.toDoubleOrNull() ?: 0.0
    val discP = discountPercentStr.toDoubleOrNull() ?: 0.0
    val shipF = shippingStr.toDoubleOrNull() ?: 0.0
    val discountAmt = subtotal * (discP / 100.0)
    val taxable = (subtotal - discountAmt).coerceAtLeast(0.0)
    val taxAmt = taxable * (taxP / 100.0)
    val grandTotal = (taxable + taxAmt + shipF).coerceAtLeast(0.0)

    fun buildCurrentInvoice(): Invoice {
        return (invoice ?: Invoice(invoiceNumber = invoiceNumber)).copy(
            invoiceNumber = invoiceNumber.trim(),
            clientId = selectedClient?.id,
            clientName = clientName.trim(),
            clientCompany = clientCompany.trim(),
            clientEmail = clientEmail.trim(),
            clientAddress = clientAddress.trim(),
            companyLogo = companyLogo,
            clientLogo = clientLogo,
            templateStyle = templateStyle,
            signatureData = signatureData,
            signatoryName = signatoryName.trim(),
            signatoryTitle = signatoryTitle.trim(),
            currency = currency,
            items = items,
            taxPercent = taxP,
            discountPercent = discP,
            shippingOrFee = shipF,
            status = status,
            notes = notes.trim(),
            paymentDetails = paymentDetails.trim(),
            termsAndConditions = terms.trim()
        )
    }

    if (showCompanyLogoPicker) {
        LogoPickerDialog(
            title = "Select Company Brand Logo",
            currentLogo = companyLogo,
            isClientLogo = false,
            onDismiss = { showCompanyLogoPicker = false },
            onLogoSelected = {
                if (it != null) companyLogo = it
                showCompanyLogoPicker = false
            }
        )
    }

    if (showClientLogoPicker) {
        LogoPickerDialog(
            title = "Select Client Logo",
            currentLogo = clientLogo,
            isClientLogo = true,
            onDismiss = { showClientLogoPicker = false },
            onLogoSelected = {
                if (it != null) clientLogo = it
                showClientLogoPicker = false
            }
        )
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            initialSignatoryName = signatoryName,
            initialSignatoryTitle = signatoryTitle,
            currentSignature = signatureData,
            onDismiss = { showSignaturePad = false },
            onSignatureSaved = { newSignature, newSignerName, newSignerTitle ->
                signatureData = newSignature
                if (newSignerName.isNotBlank()) signatoryName = newSignerName
                if (newSignerTitle.isNotBlank()) signatoryTitle = newSignerTitle
                showSignaturePad = false
            }
        )
    }

    if (previewInvoiceModal != null) {
        val currentInv = previewInvoiceModal!!
        InvoicePreviewDialog(
            invoice = currentInv,
            companyProfile = companyProfile,
            onDismiss = { previewInvoiceModal = null },
            onSendEmail = {
                val emailIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(currentInv.clientEmail))
                    putExtra(Intent.EXTRA_SUBJECT, "Invoice ${currentInv.invoiceNumber}")
                    putExtra(Intent.EXTRA_TEXT, "Please find details for invoice ${currentInv.invoiceNumber}. Total due: ${currentInv.currency}${String.format(Locale.US, "%.2f", currentInv.totalAmount)}")
                }
                context.startActivity(Intent.createChooser(emailIntent, "Send Invoice via Email"))
            },
            onExportPdf = {
                InvoicePdfExporter.sharePdf(context, currentInv, companyProfile)
            },
            onSavePdf = {
                InvoicePdfExporter.savePdfToDownloads(context, currentInv, companyProfile)
            },
            onShare = {
                InvoicePdfExporter.sharePdf(context, currentInv, companyProfile)
            },
            onMarkPaid = {
                status = InvoiceStatus.PAID
                previewInvoiceModal = currentInv.copy(status = InvoiceStatus.PAID)
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (invoice == null) "Custom Invoice Maker" else "Edit Invoice",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Logo • Line Items • Signature • PDF Export",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    val current = buildCurrentInvoice()
                                    InvoicePdfExporter.sharePdf(context, current, companyProfile)
                                },
                                modifier = Modifier.testTag("quick_export_pdf_button")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                            }
                            Button(
                                onClick = {
                                    val saved = buildCurrentInvoice()
                                    onSave(saved)
                                },
                                shape = RoundedCornerShape(10.dp),
                                enabled = invoiceNumber.isNotBlank() && items.isNotEmpty() && (clientName.isNotBlank() || clientCompany.isNotBlank()),
                                modifier = Modifier.testTag("save_invoice_button")
                            ) {
                                Text("Save Invoice")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(12.dp)
                                .fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { previewInvoiceModal = buildCurrentInvoice() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("preview_invoice_btn")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Live Preview")
                            }
                            Button(
                                onClick = {
                                    val current = buildCurrentInvoice()
                                    InvoicePdfExporter.sharePdf(context, current, companyProfile)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_share_pdf_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export & Share")
                            }
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 12.dp,
                        bottom = innerPadding.calculateBottomPadding() + 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // --- 1. Brand Logos (Company & Client) ---
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Logos & Visual Branding",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Company Logo Card
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showCompanyLogoPicker = true }
                                            .testTag("select_company_logo_button")
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            val compBitmap = remember(companyLogo) {
                                                LogoPresetManager.getLogoBitmap(companyLogo, 80)
                                            }
                                            Image(
                                                bitmap = compBitmap.asImageBitmap(),
                                                contentDescription = "Company Logo",
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White)
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                    .padding(2.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Company Logo",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Tap to change",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Client Logo Card
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showClientLogoPicker = true }
                                            .testTag("select_client_logo_button")
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            val clientBitmap = remember(clientLogo) {
                                                LogoPresetManager.getLogoBitmap(clientLogo, 80)
                                            }
                                            Image(
                                                bitmap = clientBitmap.asImageBitmap(),
                                                contentDescription = "Client Logo",
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White)
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                    .padding(2.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Client Logo",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Tap to change",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- 1.1 Document Layout Style ---
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Pre-defined Document Layout",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    val activeStyleObj = InvoiceTemplateStyle.fromId(templateStyle)
                                    Surface(
                                        color = activeStyleObj.primaryColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = activeStyleObj.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = activeStyleObj.primaryColor,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Choose from 7 curated design templates for PDF generation and client sharing:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(InvoiceTemplateStyle.values()) { st ->
                                        val isSelected = st.id == templateStyle
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { templateStyle = st.id },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(st.primaryColor, CircleShape)
                                                )
                                            },
                                            label = { Text(st.title) },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- 2. Invoice Meta ---
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Invoice Details",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = invoiceNumber,
                                        onValueChange = { invoiceNumber = it },
                                        label = { Text("Invoice # *") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("invoice_number_input")
                                    )
                                    // Currency Picker
                                    Column(modifier = Modifier.weight(0.6f)) {
                                        Text(
                                            text = "Currency",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            items(currencies) { c ->
                                                FilterChip(
                                                    selected = currency == c,
                                                    onClick = { currency = c },
                                                    label = { Text(c) }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Status selector
                                Text(
                                    text = "Status",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    InvoiceStatus.values().forEach { st ->
                                        item {
                                            FilterChip(
                                                selected = status == st,
                                                onClick = { status = st },
                                                label = { Text(st.name) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- 3. Client Selection & Details ---
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Client (Billed To)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (clients.isNotEmpty()) {
                                    Text(
                                        text = "Autofill from CRM:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(clients) { c ->
                                            FilterChip(
                                                selected = selectedClient?.id == c.id,
                                                onClick = {
                                                    selectedClient = c
                                                    clientName = c.name
                                                    clientCompany = c.company
                                                    clientEmail = c.email
                                                    clientAddress = c.address
                                                },
                                                label = { Text(c.company.ifBlank { c.name }) }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = clientCompany,
                                    onValueChange = { clientCompany = it },
                                    label = { Text("Client Company Name") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("client_company_input")
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = clientName,
                                        onValueChange = { clientName = it },
                                        label = { Text("Contact Name *") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("client_name_input")
                                    )
                                    OutlinedTextField(
                                        value = clientEmail,
                                        onValueChange = { clientEmail = it },
                                        label = { Text("Client Email") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("client_email_input")
                                    )
                                }
                                OutlinedTextField(
                                    value = clientAddress,
                                    onValueChange = { clientAddress = it },
                                    label = { Text("Billing Address") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // --- 4. Line Items Editor (num. - item name - price - total) ---
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = "Line Items (${items.size})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Num • Item Name • Qty • Price • Total",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            items = items + InvoiceItem(description = "", quantity = 1.0, unitPrice = 0.0)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("add_item_row_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Item")
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick presets chips
                                Text(
                                    text = "Quick Presets:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val presets = listOf(
                                        "Software Development Sprint" to 1200.0,
                                        "Monthly Operations Retainer" to 850.0,
                                        "Cloud Architecture Audit" to 600.0,
                                        "UI/UX Design Review" to 450.0,
                                        "Security Assessment" to 750.0
                                    )
                                    items(presets) { (desc, rate) ->
                                        AssistChip(
                                            onClick = {
                                                items = items + InvoiceItem(description = desc, quantity = 1.0, unitPrice = rate)
                                            },
                                            label = { Text("$desc ($$rate)", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                items.forEachIndexed { index, item ->
                                    var desc by remember(item.id) { mutableStateOf(item.description) }
                                    var qtyStr by remember(item.id) { mutableStateOf(item.quantity.toString()) }
                                    var priceStr by remember(item.id) { mutableStateOf(item.unitPrice.toString()) }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Item #${index + 1}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }

                                                if (items.size > 1) {
                                                    IconButton(
                                                        onClick = { items = items.filterIndexed { i, _ -> i != index } },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = ErrorRed, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            OutlinedTextField(
                                                value = desc,
                                                onValueChange = {
                                                    desc = it
                                                    items = items.toMutableList().also { list ->
                                                        list[index] = item.copy(description = it)
                                                    }
                                                },
                                                label = { Text("Item Name / Description *") },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("item_desc_input_$index")
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                OutlinedTextField(
                                                    value = qtyStr,
                                                    onValueChange = {
                                                        qtyStr = it
                                                        val q = it.toDoubleOrNull() ?: 1.0
                                                        items = items.toMutableList().also { list ->
                                                            list[index] = item.copy(quantity = q)
                                                        }
                                                    },
                                                    label = { Text("Qty") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(0.9f)
                                                )

                                                OutlinedTextField(
                                                    value = priceStr,
                                                    onValueChange = {
                                                        priceStr = it
                                                        val p = it.toDoubleOrNull() ?: 0.0
                                                        items = items.toMutableList().also { list ->
                                                            list[index] = item.copy(unitPrice = p)
                                                        }
                                                    },
                                                    label = { Text("Unit Price ($currency)") },
                                                    singleLine = true,
                                                    modifier = Modifier
                                                        .weight(1.3f)
                                                        .testTag("item_price_input_$index")
                                                )

                                                Surface(
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                    modifier = Modifier.weight(1.2f)
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.End,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Total ($currency)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "$currency${String.format(Locale.US, "%.2f", item.total)}",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- 5. Digital Signature Section (Canvas-based drawing directly on screen) ---
                    item {
                        DigitalSignaturePad(
                            initialSignatoryName = signatoryName,
                            initialSignatoryTitle = signatoryTitle,
                            currentSignature = signatureData,
                            onSignatureChanged = { base64, name, title ->
                                signatureData = base64
                                if (name.isNotBlank()) signatoryName = name
                                if (title.isNotBlank()) signatoryTitle = title
                            },
                            showSignatoryInputs = true
                        )
                    }

                    // --- 6. Financial Calculations & Summary Ledger ---
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Tax, Discounts & Totals",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = discountPercentStr,
                                        onValueChange = { discountPercentStr = it },
                                        label = { Text("Discount %") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = taxPercentStr,
                                        onValueChange = { taxPercentStr = it },
                                        label = { Text("Tax Rate %") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = shippingStr,
                                        onValueChange = { shippingStr = it },
                                        label = { Text("Shipping/Fee") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
                                    Text("$currency${String.format(Locale.US, "%,.2f", subtotal)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                }

                                if (discP > 0) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Discount ($discP%):", style = MaterialTheme.typography.bodyMedium, color = SuccessGreen)
                                        Text("-$currency${String.format(Locale.US, "%,.2f", discountAmt)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = SuccessGreen)
                                    }
                                }

                                if (taxP > 0) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Tax ($taxP%):", style = MaterialTheme.typography.bodyMedium)
                                        Text("+$currency${String.format(Locale.US, "%,.2f", taxAmt)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }

                                if (shipF > 0) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Shipping / Extra Fee:", style = MaterialTheme.typography.bodyMedium)
                                        Text("+$currency${String.format(Locale.US, "%,.2f", shipF)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "Grand Total Due:",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "$currency${String.format(Locale.US, "%,.2f", grandTotal)}",
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- 7. Notes & Payment Details ---
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Payment Instructions & Notes",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedTextField(
                                    value = paymentDetails,
                                    onValueChange = { paymentDetails = it },
                                    label = { Text("Bank Wire / PayPal Payment Details") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Invoice Notes") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = terms,
                                    onValueChange = { terms = it },
                                    label = { Text("Terms & Conditions") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewDialog(
    invoice: Invoice,
    companyProfile: CompanyProfile,
    onDismiss: () -> Unit,
    onSendEmail: () -> Unit,
    onExportPdf: () -> Unit,
    onSavePdf: () -> Unit,
    onShare: () -> Unit,
    onMarkPaid: () -> Unit
) {
    val context = LocalContext.current
    var currentStyle by remember {
        mutableStateOf(InvoiceTemplateStyle.fromId(invoice.templateStyle))
    }
    val currentInvoice = remember(invoice, currentStyle) {
        invoice.copy(templateStyle = currentStyle.id)
    }
    var showFullStyleSelector by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Invoice Preview",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${currentInvoice.invoiceNumber} • ${currentStyle.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = currentStyle.primaryColor
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showFullStyleSelector = true }) {
                                Icon(Icons.Default.Palette, contentDescription = "Change Layout Style", tint = currentStyle.primaryColor)
                            }
                            IconButton(onClick = {
                                InvoicePdfExporter.sharePdf(context, currentInvoice, companyProfile)
                            }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export & Share PDF", tint = PrimaryBlue)
                            }
                            IconButton(onClick = {
                                InvoicePdfExporter.savePdfToDownloads(context, currentInvoice, companyProfile)
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Save PDF to Downloads")
                            }
                            IconButton(onClick = onShare) {
                                Icon(Icons.Default.Share, contentDescription = "Share text invoice")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        InvoicePdfExporter.sharePdf(context, currentInvoice, companyProfile)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = currentStyle.primaryColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export PDF")
                                }
                                OutlinedButton(
                                    onClick = {
                                        InvoicePdfExporter.savePdfToDownloads(context, currentInvoice, companyProfile)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save PDF")
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = onSendEmail,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send Email")
                                }
                                if (invoice.status != InvoiceStatus.PAID) {
                                    Button(
                                        onClick = onMarkPaid,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Mark Paid")
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Quick Style Selector Bar
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Template Layout Style:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = { showFullStyleSelector = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Compare All", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(InvoiceTemplateStyle.values()) { st ->
                                        val isSelected = st == currentStyle
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { currentStyle = st },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(st.primaryColor, CircleShape)
                                                )
                                            },
                                            label = { Text(st.title) },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Styled Document Sheet
                    item {
                        StyledLiveInvoicePreview(
                            invoice = currentInvoice,
                            style = currentStyle,
                            companyProfile = companyProfile
                        )
                    }
                }
            }
        }
    }

    if (showFullStyleSelector) {
        InvoiceStyleSelectionDialog(
            invoice = currentInvoice,
            companyProfile = companyProfile,
            onDismiss = { showFullStyleSelector = false },
            onStyleSelected = { selected ->
                currentStyle = selected
                showFullStyleSelector = false
            },
            onGeneratePdf = { selected ->
                currentStyle = selected
                InvoicePdfExporter.sharePdf(context, currentInvoice.copy(templateStyle = selected.id), companyProfile)
                showFullStyleSelector = false
            }
        )
    }
}
