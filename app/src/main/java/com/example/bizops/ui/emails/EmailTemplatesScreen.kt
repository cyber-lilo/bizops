package com.example.bizops.ui.emails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bizops.data.model.Client
import com.example.bizops.data.model.CompanyProfile
import com.example.bizops.data.model.EmailCategory
import com.example.bizops.data.model.EmailTemplate
import com.example.bizops.data.model.Invoice
import com.example.bizops.ui.components.EmptyStateView
import com.example.bizops.ui.viewmodel.BizOpsViewModel
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailTemplatesScreen(
    viewModel: BizOpsViewModel,
    templates: List<EmailTemplate>,
    clients: List<Client>,
    invoices: List<Invoice>,
    companyProfile: CompanyProfile,
    initialPreselectedInvoice: Invoice? = null,
    onClearPreselectedInvoice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(EmailCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    var templateToPersonalize by remember { mutableStateOf<EmailTemplate?>(null) }
    var showPersonalizer by remember { mutableStateOf(false) }

    var showAiGeneratorDialog by remember { mutableStateOf(false) }
    var showCreateCustomTemplateDialog by remember { mutableStateOf(false) }
    var templateToEdit by remember { mutableStateOf<EmailTemplate?>(null) }

    // If navigated with a preselected invoice, auto-open the invoice template personalizer
    LaunchedEffect(initialPreselectedInvoice) {
        if (initialPreselectedInvoice != null) {
            val invoiceTemplate = templates.find { it.category == EmailCategory.INVOICE_BILLING }
                ?: templates.firstOrNull()
            templateToPersonalize = invoiceTemplate
            showPersonalizer = true
            onClearPreselectedInvoice()
        }
    }

    val filteredTemplates = remember(templates, selectedCategory, searchQuery) {
        templates.filter { t ->
            val matchesCategory = selectedCategory == EmailCategory.ALL ||
                    (selectedCategory == EmailCategory.CUSTOM && t.isCustom) ||
                    t.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                    t.title.contains(searchQuery, ignoreCase = true) ||
                    t.subject.contains(searchQuery, ignoreCase = true) ||
                    t.body.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Email Generator",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Dynamic templates for billing, ops & outreach",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showAiGeneratorDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = InfoPurple.copy(alpha = 0.15f),
                            contentColor = InfoPurple
                        ),
                        modifier = Modifier.testTag("ai_draft_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Draft")
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
                    templateToEdit = null
                    showCreateCustomTemplateDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Template") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("new_template_fab")
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
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search email templates...") },
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

            // Category Tabs
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EmailCategory.values().forEach { cat ->
                        item {
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Templates List
            if (filteredTemplates.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.Email,
                        title = if (searchQuery.isNotBlank()) "No Matching Templates" else "No Templates in Category",
                        description = if (searchQuery.isNotBlank()) "Try refining your search keyword" else "Create a custom template or generate one with AI.",
                        actionLabel = "Draft with AI",
                        onAction = { showAiGeneratorDialog = true }
                    )
                }
            } else {
                items(filteredTemplates, key = { it.id }) { template ->
                    EmailTemplateCard(
                        template = template,
                        onUse = {
                            templateToPersonalize = template
                            showPersonalizer = true
                        },
                        onEdit = {
                            templateToEdit = template
                            showCreateCustomTemplateDialog = true
                        },
                        onDelete = {
                            viewModel.deleteTemplate(template)
                            Toast.makeText(context, "Template deleted", Toast.LENGTH_SHORT).show()
                        },
                        onQuickCopy = {
                            val (subj, body) = viewModel.populateTemplateVariables(
                                templateSubject = template.subject,
                                templateBody = template.body,
                                company = companyProfile
                            )
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Email Template", "$subj\n\n$body")
                            clipboard.setPrimaryClip(clip)
                            viewModel.recordTemplateUsage(template.id)
                            Toast.makeText(context, "Copied template to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // --- Personalize & Send Modal ---
    if (showPersonalizer && templateToPersonalize != null) {
        EmailPersonalizerDialog(
            template = templateToPersonalize!!,
            clients = clients,
            invoices = invoices,
            companyProfile = companyProfile,
            viewModel = viewModel,
            onDismiss = { showPersonalizer = false },
            onSendIntent = { recipientEmail, subject, body ->
                viewModel.recordTemplateUsage(templateToPersonalize!!.id)
                showPersonalizer = false
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${Uri.encode(recipientEmail)}")
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                try {
                    context.startActivity(Intent.createChooser(intent, "Send Email via:"))
                } catch (e: Exception) {
                    Toast.makeText(context, "No email client found. Text copied to clipboard!", Toast.LENGTH_LONG).show()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Email", "$subject\n\n$body"))
                }
            },
            onSaveAsCustomTemplate = { newTpl ->
                viewModel.saveTemplate(newTpl)
                Toast.makeText(context, "Saved to Custom Templates!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- AI Generator Modal ---
    if (showAiGeneratorDialog) {
        AiEmailGeneratorDialog(
            viewModel = viewModel,
            clients = clients,
            companyProfile = companyProfile,
            onDismiss = { showAiGeneratorDialog = false },
            onApply = { subject, body ->
                showAiGeneratorDialog = false
                templateToPersonalize = EmailTemplate(
                    title = "AI Draft - ${subject.take(30)}",
                    category = EmailCategory.CUSTOM,
                    subject = subject,
                    body = body,
                    isCustom = true
                )
                showPersonalizer = true
            }
        )
    }

    // --- Create / Edit Custom Template Dialog ---
    if (showCreateCustomTemplateDialog) {
        CreateEditTemplateDialog(
            template = templateToEdit,
            onDismiss = { showCreateCustomTemplateDialog = false },
            onSave = { saved ->
                viewModel.saveTemplate(saved)
                showCreateCustomTemplateDialog = false
                Toast.makeText(context, "Template saved", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun EmailTemplateCard(
    template: EmailTemplate,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuickCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onUse)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = template.category.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (template.usageCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Used ${template.usageCount}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (template.isCustom) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = template.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Subject: ${template.subject}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = template.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onQuickCopy,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }
                Spacer(modifier = Modifier.width(6.dp))
                FilledTonalButton(
                    onClick = onUse,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Use Template")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailPersonalizerDialog(
    template: EmailTemplate,
    clients: List<Client>,
    invoices: List<Invoice>,
    companyProfile: CompanyProfile,
    viewModel: BizOpsViewModel,
    onDismiss: () -> Unit,
    onSendIntent: (recipientEmail: String, subject: String, body: String) -> Unit,
    onSaveAsCustomTemplate: (EmailTemplate) -> Unit
) {
    val context = LocalContext.current
    var selectedClient by remember { mutableStateOf<Client?>(clients.firstOrNull()) }
    var selectedInvoice by remember { mutableStateOf<Invoice?>(invoices.firstOrNull()) }

    var recipientEmail by remember { mutableStateOf(selectedClient?.email ?: "") }
    var subjectText by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf("") }

    var showEnhanceDialog by remember { mutableStateOf(false) }

    // Function to apply placeholders
    fun refreshPlaceholders() {
        val (s, b) = viewModel.populateTemplateVariables(
            templateSubject = template.subject,
            templateBody = template.body,
            client = selectedClient,
            invoice = selectedInvoice,
            company = companyProfile
        )
        subjectText = s
        bodyText = b
        if (selectedClient != null && selectedClient!!.email.isNotBlank()) {
            recipientEmail = selectedClient!!.email
        }
    }

    LaunchedEffect(template, selectedClient, selectedInvoice) {
        refreshPlaceholders()
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
                            Text(
                                text = "Personalize & Send Email",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showEnhanceDialog = true },
                                modifier = Modifier.testTag("ai_enhance_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Enhance",
                                    tint = InfoPurple
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                .padding(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Email", "$subjectText\n\n$bodyText"))
                                    Toast.makeText(context, "Copied email text to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Text")
                            }

                            Button(
                                onClick = {
                                    onSendIntent(recipientEmail, subjectText, bodyText)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("open_email_app_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Email App")
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
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // --- CRM / Invoice Data Linking Pill Selectors ---
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "1. Autofill Context",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (clients.isNotEmpty()) {
                                    Text("Select Client:", style = MaterialTheme.typography.labelSmall)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(clients) { c ->
                                            FilterChip(
                                                selected = selectedClient?.id == c.id,
                                                onClick = {
                                                    selectedClient = c
                                                    recipientEmail = c.email
                                                },
                                                label = { Text(c.company.ifBlank { c.name }) }
                                            )
                                        }
                                    }
                                }

                                if (invoices.isNotEmpty()) {
                                    Text("Link Invoice (Amount/Due Date):", style = MaterialTheme.typography.labelSmall)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(invoices) { inv ->
                                            FilterChip(
                                                selected = selectedInvoice?.id == inv.id,
                                                onClick = { selectedInvoice = inv },
                                                label = { Text("${inv.invoiceNumber} (${inv.formattedTotal()})") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Recipient Email & Subject Fields ---
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = recipientEmail,
                                    onValueChange = { recipientEmail = it },
                                    label = { Text("To: Recipient Email") },
                                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = subjectText,
                                    onValueChange = { subjectText = it },
                                    label = { Text("Subject Line") },
                                    leadingIcon = { Icon(Icons.Default.Subject, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // --- Email Body Editor ---
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Email Content",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = { showEnhanceDialog = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = InfoPurple, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("AI Polish", color = InfoPurple)
                                    }
                                }

                                OutlinedTextField(
                                    value = bodyText,
                                    onValueChange = { bodyText = it },
                                    label = { Text("Message Body") },
                                    minLines = 8,
                                    maxLines = 16,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                TextButton(
                                    onClick = {
                                        val customTpl = EmailTemplate(
                                            title = "Custom: ${subjectText.take(25)}",
                                            category = EmailCategory.CUSTOM,
                                            subject = subjectText,
                                            body = bodyText,
                                            isCustom = true
                                        )
                                        onSaveAsCustomTemplate(customTpl)
                                    }
                                ) {
                                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save as New Custom Template")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- AI Polish Tone Dialog ---
    if (showEnhanceDialog) {
        var selectedTone by remember { mutableStateOf("Professional") }
        var customInstruction by remember { mutableStateOf("") }
        val isGenerating by viewModel.isGeneratingAi.collectAsState()

        AlertDialog(
            onDismissRequest = { if (!isGenerating) showEnhanceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = InfoPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Polish & Rephrase")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select desired tone or instructions:")
                    val tones = listOf("Professional", "Friendly", "Urgent", "Concise", "Executive")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tones) { t ->
                            FilterChip(
                                selected = selectedTone == t,
                                onClick = { selectedTone = t },
                                label = { Text(t) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customInstruction,
                        onValueChange = { customInstruction = it },
                        label = { Text("Additional instructions (optional)") },
                        placeholder = { Text("e.g., Emphasize our 5-day SLA guarantee") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.enhanceAiEmail(
                            currentSubject = subjectText,
                            currentBody = bodyText,
                            tone = selectedTone,
                            instruction = customInstruction
                        ) { newSubj, newBody ->
                            subjectText = newSubj
                            bodyText = newBody
                            showEnhanceDialog = false
                            Toast.makeText(context, "Email enhanced with AI!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isGenerating
                ) {
                    Text(if (isGenerating) "Polishing..." else "Apply AI Polish")
                }
            },
            dismissButton = {
                if (!isGenerating) {
                    TextButton(onClick = { showEnhanceDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiEmailGeneratorDialog(
    viewModel: BizOpsViewModel,
    clients: List<Client>,
    companyProfile: CompanyProfile,
    onDismiss: () -> Unit,
    onApply: (subject: String, body: String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Professional") }
    var selectedClient by remember { mutableStateOf<Client?>(clients.firstOrNull()) }
    val isGenerating by viewModel.isGeneratingAi.collectAsState()

    val quickPrompts = listOf(
        "Late payment follow-up with 3-day grace period",
        "Quarterly project milestone review invitation",
        "New operational service retainer proposal",
        "Daily standup recap for executives"
    )

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = InfoPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Email Composer")
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        text = "Describe what you want to communicate:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("e.g., Remind client that invoice is overdue by 5 days and ask if they need wire info again...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Quick Prompts:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickPrompts) { qp ->
                            AssistChip(
                                onClick = { prompt = qp },
                                label = { Text(qp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                item {
                    Text("Tone:", style = MaterialTheme.typography.labelSmall)
                    val tones = listOf("Professional", "Friendly", "Urgent", "Concise", "Persuasive")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tones) { t ->
                            FilterChip(
                                selected = selectedTone == t,
                                onClick = { selectedTone = t },
                                label = { Text(t) }
                            )
                        }
                    }
                }

                if (isGenerating) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        viewModel.generateAiEmail(
                            prompt = prompt,
                            tone = selectedTone,
                            recipientName = selectedClient?.name ?: "Valued Client",
                            companyName = companyProfile.companyName
                        ) { subj, body ->
                            onApply(subj, body)
                        }
                    }
                },
                enabled = prompt.isNotBlank() && !isGenerating
            ) {
                Text(if (isGenerating) "Generating..." else "Generate Email")
            }
        },
        dismissButton = {
            if (!isGenerating) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun CreateEditTemplateDialog(
    template: EmailTemplate?,
    onDismiss: () -> Unit,
    onSave: (EmailTemplate) -> Unit
) {
    var title by remember { mutableStateOf(template?.title ?: "") }
    var category by remember { mutableStateOf(template?.category ?: EmailCategory.CUSTOM) }
    var subject by remember { mutableStateOf(template?.subject ?: "") }
    var body by remember { mutableStateOf(template?.body ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (template == null) "Create Custom Template" else "Edit Template",
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
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Template Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Default Subject *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Template Body (Use {{client_name}}, {{amount}}, etc.) *") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && subject.isNotBlank() && body.isNotBlank()) {
                        val saved = (template ?: EmailTemplate(
                            title = title,
                            category = category,
                            subject = subject,
                            body = body,
                            isCustom = true
                        )).copy(
                            title = title.trim(),
                            category = category,
                            subject = subject.trim(),
                            body = body.trim(),
                            isCustom = true
                        )
                        onSave(saved)
                    }
                },
                enabled = title.isNotBlank() && subject.isNotBlank() && body.isNotBlank()
            ) {
                Text("Save Template")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
