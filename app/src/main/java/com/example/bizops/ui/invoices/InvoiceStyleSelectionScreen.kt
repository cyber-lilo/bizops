package com.example.bizops.ui.invoices

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bizops.data.model.CompanyProfile
import com.example.bizops.data.model.Invoice
import com.example.bizops.data.model.InvoiceStatus
import com.example.bizops.data.model.InvoiceTemplateStyle
import com.example.bizops.ui.components.StatusBadge
import com.example.bizops.util.InvoicePdfExporter
import com.example.bizops.util.LogoPresetManager
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceStyleSelectionDialog(
    invoice: Invoice,
    companyProfile: CompanyProfile,
    onDismiss: () -> Unit,
    onStyleSelected: (InvoiceTemplateStyle) -> Unit,
    onGeneratePdf: (InvoiceTemplateStyle) -> Unit
) {
    val context = LocalContext.current
    var currentStyle by remember {
        mutableStateOf(InvoiceTemplateStyle.fromId(invoice.templateStyle))
    }
    val updatedInvoice = remember(invoice, currentStyle) {
        invoice.copy(templateStyle = currentStyle.id)
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
                                    text = "Choose Invoice Style",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${invoice.invoiceNumber} • ${invoice.clientName.ifBlank { "Client" }}",
                                    style = MaterialTheme.typography.bodySmall,
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
                            FilledTonalButton(
                                onClick = {
                                    onStyleSelected(currentStyle)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apply")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 8.dp,
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
                                        onStyleSelected(currentStyle)
                                        InvoicePdfExporter.sharePdf(context, updatedInvoice, companyProfile)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = currentStyle.primaryColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("generate_pdf_with_style_button")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export PDF (${currentStyle.title})", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onStyleSelected(currentStyle)
                                        InvoicePdfExporter.savePdfToDownloads(context, updatedInvoice, companyProfile)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(0.7f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save PDF")
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
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Description
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(currentStyle.primaryColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = currentStyle.primaryColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Pre-defined Document Layouts",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Select a layout below to preview real-time typography, palettes, and PDF rendering.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Style Cards Carousel / Row
                    item {
                        Text(
                            text = "AVAILABLE STYLES & LAYOUTS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(InvoiceTemplateStyle.values()) { style ->
                                val isSelected = style == currentStyle
                                StylePresetThumbnailCard(
                                    style = style,
                                    isSelected = isSelected,
                                    onClick = { currentStyle = style }
                                )
                            }
                        }
                    }

                    // Style Info Callout
                    item {
                        Surface(
                            color = currentStyle.primaryColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, currentStyle.primaryColor.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentStyle.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = currentStyle.primaryColor
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = currentStyle.primaryColor,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = currentStyle.tag,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = currentStyle.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Typography: ${currentStyle.fontStyle} • Header: ${if (currentStyle.isDarkHero) "Dark Hero Banner" else "Modern Banner"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Color Palette Swatches
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(currentStyle.primaryColor, CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(currentStyle.secondaryColor, CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(currentStyle.accentColor, CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    // Live Document Preview Container
                    item {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "LIVE DOCUMENT PREVIEW",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "A4 PDF Rendering",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Styled Live Invoice Preview Sheet
                        StyledLiveInvoicePreview(
                            invoice = updatedInvoice,
                            style = currentStyle,
                            companyProfile = companyProfile
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StylePresetThumbnailCard(
    style: InvoiceTemplateStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) style.primaryColor else MaterialTheme.colorScheme.outlineVariant,
        label = "borderColor"
    )
    val borderWidth = if (isSelected) 2.5.dp else 1.dp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
            .testTag("style_card_${style.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Miniature Document Mock Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (style.isDarkHero) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (style.isDarkHero) 24.dp else 6.dp)
                            .background(style.primaryColor)
                    ) {
                        if (style.isDarkHero) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(Color.White, RoundedCornerShape(2.dp)))
                                Text("INV", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Mini Body Lines
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.width(36.dp).height(4.dp).background(if (style.isDarkHero) Color(0xFF38BDF8) else style.primaryColor, RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.width(20.dp).height(4.dp).background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp)))
                        }
                        Box(modifier = Modifier.width(50.dp).height(3.dp).background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp)))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.width(30.dp).height(4.dp).background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.width(18.dp).height(4.dp).background(style.primaryColor, RoundedCornerShape(2.dp)))
                        }
                    }
                }

                // Selection checkmark badge
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .background(style.primaryColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = style.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = style.tag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = style.primaryColor
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Palette Swatches
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(10.dp).background(style.primaryColor, CircleShape))
                Box(modifier = Modifier.size(10.dp).background(style.secondaryColor, CircleShape))
                Box(modifier = Modifier.size(10.dp).background(style.accentColor, CircleShape))
            }
        }
    }
}

@Composable
fun StyledLiveInvoicePreview(
    invoice: Invoice,
    style: InvoiceTemplateStyle,
    companyProfile: CompanyProfile,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val fontFamily = when (style.fontStyle) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    val cardBg = when (style) {
        InvoiceTemplateStyle.EMERALD_GROWTH -> Color(0xFFF0FDF4)
        InvoiceTemplateStyle.CREATIVE_CORAL -> Color(0xFFFFF7ED)
        InvoiceTemplateStyle.ROYAL_ENTERPRISE -> Color(0xFFF5F3FF)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            if (style == InvoiceTemplateStyle.CLASSIC_CORPORATE) 2.dp else 1.dp,
            if (style == InvoiceTemplateStyle.CLASSIC_CORPORATE) style.primaryColor else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // 1. Top Bar / Dark Hero Banner
            if (style.isDarkHero) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val compLogoBitmap = remember(invoice.companyLogo) {
                                    LogoPresetManager.getLogoBitmap(invoice.companyLogo, 80)
                                }
                                Image(
                                    bitmap = compLogoBitmap.asImageBitmap(),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                        .padding(2.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = companyProfile.companyName.ifBlank { invoice.senderCompany.ifBlank { "BizOps Enterprise" } },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = fontFamily
                                        )
                                    )
                                    Text(
                                        text = "OPERATIONS & BILLING",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = style.secondaryColor,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = fontFamily
                                        )
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "INVOICE",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = fontFamily,
                                        letterSpacing = 2.sp
                                    )
                                )
                                Text(
                                    text = invoice.invoiceNumber,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = style.secondaryColor,
                                        fontFamily = fontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
                // Cyan accent divider line
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(style.accentColor))
            } else {
                // Top decorative color strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (style == InvoiceTemplateStyle.MINIMALIST_CLEAN) 3.dp else 8.dp)
                        .background(style.primaryColor)
                )
            }

            // 2. Main Content Body
            Column(modifier = Modifier.padding(18.dp)) {
                if (!style.isDarkHero) {
                    // Standard header
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val compLogoBitmap = remember(invoice.companyLogo) {
                                LogoPresetManager.getLogoBitmap(invoice.companyLogo, 80)
                            }
                            Image(
                                bitmap = compLogoBitmap.asImageBitmap(),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .padding(2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = companyProfile.companyName.ifBlank { invoice.senderCompany.ifBlank { "BizOps Systems" } },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = style.primaryColor,
                                        fontFamily = fontFamily
                                    )
                                )
                                val compAddress = companyProfile.address.ifBlank { invoice.senderAddress }
                                if (compAddress.isNotBlank()) {
                                    Text(
                                        text = compAddress.lines().firstOrNull() ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "INVOICE",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    fontFamily = fontFamily
                                ),
                                color = if (style == InvoiceTemplateStyle.CLASSIC_CORPORATE) style.primaryColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = invoice.invoiceNumber,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontFamily
                                )
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Metadata: Billed To Card & Dates
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Billed To Box
                    Surface(
                        color = if (style == InvoiceTemplateStyle.MINIMALIST_CLEAN) MaterialTheme.colorScheme.surface else style.primaryColor.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, style.primaryColor.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val clientLogoBitmap = remember(invoice.clientLogo) {
                                LogoPresetManager.getLogoBitmap(invoice.clientLogo, 70)
                            }
                            Image(
                                bitmap = clientLogoBitmap.asImageBitmap(),
                                contentDescription = "Client Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .padding(2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "BILLED TO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = style.primaryColor,
                                        fontFamily = fontFamily
                                    )
                                )
                                Text(
                                    text = invoice.clientCompany.ifBlank { invoice.clientName.ifBlank { "Valued Client" } },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = fontFamily
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (invoice.clientName.isNotBlank() && invoice.clientCompany.isNotBlank()) {
                                    Text(
                                        text = "Attn: ${invoice.clientName}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Dates & Status
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(0.9f)
                    ) {
                        StatusBadge(status = invoice.status)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Issued: ${dateFormat.format(Date(invoice.issueDate))}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Due: ${dateFormat.format(Date(invoice.dueDate))}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Table Header
                val headerBg = when (style) {
                    InvoiceTemplateStyle.TECH_DARK_HERO -> Color(0xFF0F172A)
                    InvoiceTemplateStyle.MINIMALIST_CLEAN -> Color(0xFFF1F5F9)
                    InvoiceTemplateStyle.CLASSIC_CORPORATE -> style.primaryColor
                    else -> style.primaryColor
                }
                val headerTextColor = if (style == InvoiceTemplateStyle.MINIMALIST_CLEAN) Color(0xFF0F172A) else Color.White

                Surface(
                    color = headerBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text("#", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = headerTextColor, fontFamily = fontFamily), modifier = Modifier.weight(0.4f))
                        Text("DESCRIPTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = headerTextColor, fontFamily = fontFamily), modifier = Modifier.weight(2f))
                        Text("QTY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = headerTextColor, fontFamily = fontFamily), textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                        Text("PRICE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = headerTextColor, fontFamily = fontFamily), textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                        Text("TOTAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = headerTextColor, fontFamily = fontFamily), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }
                }

                // Table Items
                invoice.items.forEachIndexed { index, item ->
                    val rowBg = if (index % 2 == 1) style.primaryColor.copy(alpha = 0.04f) else Color.Transparent
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontFamily = fontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.4f)
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = fontFamily),
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "${item.quantity}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.6f)
                        )
                        Text(
                            text = "${invoice.currency}${String.format(Locale.US, "%.2f", item.unitPrice)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.9f)
                        )
                        Text(
                            text = "${invoice.currency}${String.format(Locale.US, "%.2f", item.total)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = fontFamily),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Totals Block
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.width(200.dp)
                    ) {
                        Text("Subtotal:", style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily))
                        Text(invoice.formattedSubtotal(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = fontFamily))
                    }
                    if (invoice.discountPercent > 0) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Discount (${invoice.discountPercent}%):", style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily), color = SuccessGreen)
                            Text("-${invoice.currency}${String.format(Locale.US, "%.2f", invoice.discountAmount)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = fontFamily), color = SuccessGreen)
                        }
                    }
                    if (invoice.taxPercent > 0) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Tax (${invoice.taxPercent}%):", style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily))
                            Text("+${invoice.currency}${String.format(Locale.US, "%.2f", invoice.taxAmount)}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Grand Total Highlight
                    Surface(
                        color = if (style == InvoiceTemplateStyle.CLASSIC_CORPORATE) style.secondaryColor else style.primaryColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(220.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "TOTAL DUE:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = fontFamily
                                )
                            )
                            Text(
                                invoice.formattedTotal(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = fontFamily
                                )
                            )
                        }
                    }
                }

                // Signature & Footer Note
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Style Template: ${style.title}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = style.primaryColor,
                                fontFamily = fontFamily
                            )
                        )
                        Text(
                            text = "Thank you for your partnership!",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Signature
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(140.dp)
                    ) {
                        val previewSigBitmap = remember(invoice.signatureData, invoice.signatoryName) {
                            LogoPresetManager.getSignatureBitmap(invoice.signatureData, invoice.signatoryName, width = 240, height = 70)
                        }
                        Image(
                            bitmap = previewSigBitmap.asImageBitmap(),
                            contentDescription = "Signature",
                            modifier = Modifier.height(36.dp).fillMaxWidth()
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = invoice.signatoryName.ifBlank { "Authorized Signature" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily
                            )
                        )
                    }
                }
            }
        }
    }
}
