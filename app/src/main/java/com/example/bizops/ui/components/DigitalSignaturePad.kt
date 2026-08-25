package com.example.bizops.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data structure representing a single point or stroke on the signature canvas.
 */
data class SignaturePoint(val x: Float, val y: Float, val timestamp: Long = System.currentTimeMillis())

data class SignatureStroke(
    val points: List<SignaturePoint>,
    val color: Color,
    val strokeWidth: Float,
    val isCalligraphic: Boolean = false
)

/**
 * Color presets for signature ink.
 */
val SignatureInkColors = listOf(
    Color(0xFF1D4ED8) to "Royal Blue",
    Color(0xFF0F172A) to "Midnight Black",
    Color(0xFF1E3A8A) to "Executive Navy",
    Color(0xFF334155) to "Dark Slate",
    Color(0xFF881337) to "Burgundy",
    Color(0xFF065F46) to "Forest Green"
)

enum class PenThickness(val label: String, val widthPx: Float) {
    FINE("Fine", 3.0f),
    MEDIUM("Medium", 5.0f),
    BOLD("Bold", 8.0f)
}

/**
 * Core Canvas-Based Digital Signature Drawing Pad.
 * Allows drawing smoothly with Bézier curves, undo/redo, ink color selection, stroke thickness,
 * and direct Base64 PNG export.
 */
@Composable
fun DigitalSignatureCanvas(
    modifier: Modifier = Modifier,
    canvasHeight: Dp = 180.dp,
    selectedColor: Color = Color(0xFF1D4ED8),
    selectedThickness: PenThickness = PenThickness.MEDIUM,
    isCalligraphic: Boolean = false,
    showGuideLines: Boolean = true,
    watermarkText: String? = null,
    strokes: List<SignatureStroke>,
    currentStroke: List<SignaturePoint>,
    onStrokeStarted: (SignaturePoint) -> Unit,
    onStrokeDragged: (SignaturePoint) -> Unit,
    onStrokeCompleted: (SignatureStroke) -> Unit,
    onDotAdded: (SignaturePoint, Color, Float) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFAFBFD))
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .testTag("digital_signature_canvas_box")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedColor, selectedThickness, isCalligraphic) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onStrokeStarted(SignaturePoint(offset.x, offset.y))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            onStrokeDragged(SignaturePoint(change.position.x, change.position.y))
                        },
                        onDragEnd = {
                            if (currentStroke.isNotEmpty()) {
                                onStrokeCompleted(
                                    SignatureStroke(
                                        points = currentStroke,
                                        color = selectedColor,
                                        strokeWidth = selectedThickness.widthPx,
                                        isCalligraphic = isCalligraphic
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            if (currentStroke.isNotEmpty()) {
                                onStrokeCompleted(
                                    SignatureStroke(
                                        points = currentStroke,
                                        color = selectedColor,
                                        strokeWidth = selectedThickness.widthPx,
                                        isCalligraphic = isCalligraphic
                                    )
                                )
                            }
                        }
                    )
                }
                .pointerInput(selectedColor, selectedThickness) {
                    detectTapGestures { offset ->
                        onDotAdded(SignaturePoint(offset.x, offset.y), selectedColor, selectedThickness.widthPx)
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // 1. Subtle Background Grid / Guides
            if (showGuideLines) {
                val baselineY = height * 0.72f
                // Signature base line
                drawLine(
                    color = Color(0xFF94A3B8).copy(alpha = 0.5f),
                    start = Offset(24f, baselineY),
                    end = Offset(width - 24f, baselineY),
                    strokeWidth = 1.5f
                )
                // "X" indicator
                val xLeft = 32f
                val xY = baselineY - 4f
                val xSize = 10f
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(xLeft - xSize, xY - xSize),
                    end = Offset(xLeft + xSize, xY + xSize),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(xLeft - xSize, xY + xSize),
                    end = Offset(xLeft + xSize, xY - xSize),
                    strokeWidth = 2f
                )
            }

            // 2. Render Completed Strokes with Smooth Quadratic Bézier Curves
            strokes.forEach { stroke ->
                drawSmoothStroke(stroke)
            }

            // 3. Render In-Progress Stroke
            if (currentStroke.isNotEmpty()) {
                val activeStroke = SignatureStroke(
                    points = currentStroke,
                    color = selectedColor,
                    strokeWidth = selectedThickness.widthPx,
                    isCalligraphic = isCalligraphic
                )
                drawSmoothStroke(activeStroke)
            }
        }

        // Placeholder watermark or empty hint
        if (strokes.isEmpty() && currentStroke.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Draw,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sign with finger or stylus directly on screen",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "Draw your official business signature above the line",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // Timestamp watermark
        if (watermarkText != null && strokes.isNotEmpty()) {
            Text(
                text = watermarkText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8).copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }
}

/**
 * Helper extension to draw smooth Bézier curves between sampled points on Compose Canvas.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothStroke(stroke: SignatureStroke) {
    val pts = stroke.points
    if (pts.isEmpty()) return

    if (pts.size == 1) {
        // Single tap creates a clean dot
        drawCircle(
            color = stroke.color,
            radius = stroke.strokeWidth * 0.9f,
            center = Offset(pts[0].x, pts[0].y)
        )
        return
    }

    val composePath = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        if (pts.size == 2) {
            lineTo(pts[1].x, pts[1].y)
        } else {
            // Smooth quadratic Bézier interpolation between midpoints
            for (i in 1 until pts.size - 1) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val midX = (p0.x + p1.x) / 2f
                val midY = (p0.y + p1.y) / 2f
                quadraticTo(p0.x, p0.y, midX, midY)
            }
            val last = pts.last()
            lineTo(last.x, last.y)
        }
    }

    drawPath(
        path = composePath,
        color = stroke.color,
        style = Stroke(
            width = stroke.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * Full interactive Canvas Signature Component with toolbar (palette, thickness, undo/redo, clear)
 * designed for direct inclusion inside form screens or modal dialogs.
 */
@Composable
fun DigitalSignaturePad(
    modifier: Modifier = Modifier,
    initialSignatoryName: String = "",
    initialSignatoryTitle: String = "",
    currentSignature: String? = null,
    onSignatureChanged: (base64Png: String?, signerName: String, signerTitle: String) -> Unit,
    showSignatoryInputs: Boolean = true,
    compactToolbar: Boolean = false
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Canvas Draw, 1 = Cursive Script
    var signatoryName by remember { mutableStateOf(initialSignatoryName.ifBlank { "Authorized Officer" }) }
    var signatoryTitle by remember { mutableStateOf(initialSignatoryTitle.ifBlank { "Managing Director" }) }

    // Drawing state
    val strokes = remember { mutableStateListOf<SignatureStroke>() }
    val redoStack = remember { mutableStateListOf<SignatureStroke>() }
    var currentStrokePoints by remember { mutableStateOf<List<SignaturePoint>>(emptyList()) }

    var selectedInkColor by remember { mutableStateOf(Color(0xFF1D4ED8)) }
    var selectedThickness by remember { mutableStateOf(PenThickness.MEDIUM) }
    var isCalligraphic by remember { mutableStateOf(false) }

    val formattedDate = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    }

    fun notifyUpdate() {
        val base64 = if (selectedTab == 0 && strokes.isNotEmpty()) {
            exportStrokesToPngBase64(strokes, 450, 180)
        } else {
            null
        }
        onSignatureChanged(base64, signatoryName.trim(), signatoryTitle.trim())
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title & Tab Switcher
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Draw,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Canvas Digital Signature",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Mode Tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(2.dp)
                ) {
                    Surface(
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable {
                                selectedTab = 0
                                notifyUpdate()
                            }
                            .testTag("tab_draw_signature")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Draw", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Surface(
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable {
                                selectedTab = 1
                                notifyUpdate()
                            }
                            .testTag("tab_type_signature")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Type", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // Toolbar: Ink Palette, Pen Size, Undo, Redo, Clear
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Color Swatches
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(SignatureInkColors) { (color, name) ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedInkColor == color) 2.5.dp else 1.dp,
                                            color = if (selectedInkColor == color) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.7f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedInkColor = color }
                                )
                            }
                        }

                        // Pen Thickness Selector
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PenThickness.values().forEach { thickness ->
                                FilterChip(
                                    selected = selectedThickness == thickness,
                                    onClick = { selectedThickness = thickness },
                                    label = { Text(thickness.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }

                    // Undo / Redo / Clear Row
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalIconButton(
                                onClick = {
                                    if (strokes.isNotEmpty()) {
                                        val last = strokes.removeAt(strokes.lastIndex)
                                        redoStack.add(last)
                                        notifyUpdate()
                                    }
                                },
                                enabled = strokes.isNotEmpty(),
                                modifier = Modifier.size(32.dp).testTag("signature_undo_btn")
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    if (redoStack.isNotEmpty()) {
                                        val restored = redoStack.removeAt(redoStack.lastIndex)
                                        strokes.add(restored)
                                        notifyUpdate()
                                    }
                                },
                                enabled = redoStack.isNotEmpty(),
                                modifier = Modifier.size(32.dp).testTag("signature_redo_btn")
                            ) {
                                Icon(Icons.Default.Redo, contentDescription = "Redo", modifier = Modifier.size(16.dp))
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    strokes.clear()
                                    redoStack.clear()
                                    currentStrokePoints = emptyList()
                                    notifyUpdate()
                                },
                                enabled = strokes.isNotEmpty() || currentStrokePoints.isNotEmpty(),
                                modifier = Modifier.size(32.dp).testTag("signature_clear_btn")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = if (strokes.isEmpty()) "Canvas ready" else "${strokes.size} stroke(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // THE INTERACTIVE CANVAS
                    DigitalSignatureCanvas(
                        canvasHeight = 160.dp,
                        selectedColor = selectedInkColor,
                        selectedThickness = selectedThickness,
                        isCalligraphic = isCalligraphic,
                        showGuideLines = true,
                        watermarkText = "Digitally captured $formattedDate",
                        strokes = strokes,
                        currentStroke = currentStrokePoints,
                        onStrokeStarted = { pt ->
                            currentStrokePoints = listOf(pt)
                            redoStack.clear()
                        },
                        onStrokeDragged = { pt ->
                            currentStrokePoints = currentStrokePoints + pt
                        },
                        onStrokeCompleted = { newStroke ->
                            strokes.add(newStroke)
                            currentStrokePoints = emptyList()
                            notifyUpdate()
                        },
                        onDotAdded = { pt, color, width ->
                            strokes.add(
                                SignatureStroke(
                                    points = listOf(pt),
                                    color = color,
                                    strokeWidth = width,
                                    isCalligraphic = false
                                )
                            )
                            notifyUpdate()
                        }
                    )
                }
            } else {
                // Cursive Script Fallback Mode
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFAFBFD))
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = signatoryName.ifBlank { "Authorized Officer" },
                        fontSize = 28.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1D4ED8),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(1.5.dp)
                            .background(Color(0xFFCBD5E1))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Formal Digital Seal • $signatoryTitle",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Signatory Meta Inputs
            if (showSignatoryInputs) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = signatoryName,
                        onValueChange = {
                            signatoryName = it
                            notifyUpdate()
                        },
                        label = { Text("Signatory Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("signatory_name_field")
                    )
                    OutlinedTextField(
                        value = signatoryTitle,
                        onValueChange = {
                            signatoryTitle = it
                            notifyUpdate()
                        },
                        label = { Text("Job Title / Designation") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("signatory_title_field")
                    )
                }
            }
        }
    }
}

/**
 * Enhanced Signature Pad Dialog with full canvas drawing capabilities and instant preview.
 */
@Composable
fun SignaturePadDialog(
    initialSignatoryName: String = "",
    initialSignatoryTitle: String = "",
    currentSignature: String? = null,
    onDismiss: () -> Unit,
    onSignatureSaved: (signatureBase64: String?, signatoryName: String, signatoryTitle: String) -> Unit
) {
    var savedSignatureBase64 by remember { mutableStateOf<String?>(currentSignature) }
    var currentSignerName by remember { mutableStateOf(initialSignatoryName) }
    var currentSignerTitle by remember { mutableStateOf(initialSignatoryTitle) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Modal Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Draw,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Digital Signature Canvas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Draw your official signature directly on screen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_signature_modal")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Embedded Digital Signature Pad
                DigitalSignaturePad(
                    initialSignatoryName = initialSignatoryName,
                    initialSignatoryTitle = initialSignatoryTitle,
                    currentSignature = currentSignature,
                    onSignatureChanged = { base64, name, title ->
                        savedSignatureBase64 = base64
                        currentSignerName = name
                        currentSignerTitle = title
                    },
                    showSignatoryInputs = true
                )

                // Dialog Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSignatureSaved(
                                savedSignatureBase64,
                                currentSignerName.trim(),
                                currentSignerTitle.trim()
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("apply_signature_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply to Invoice")
                    }
                }
            }
        }
    }
}

/**
 * Smooth quadratic Bézier rasterization to transparent PNG and Base64 encoding.
 */
fun exportStrokesToPngBase64(strokes: List<SignatureStroke>, width: Int, height: Int): String {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
    }

    strokes.forEach { stroke ->
        paint.color = android.graphics.Color.argb(
            (stroke.color.alpha * 255).toInt(),
            (stroke.color.red * 255).toInt(),
            (stroke.color.green * 255).toInt(),
            (stroke.color.blue * 255).toInt()
        )
        paint.strokeWidth = stroke.strokeWidth * 1.5f

        val pts = stroke.points
        if (pts.size == 1) {
            canvas.drawCircle(pts[0].x, pts[0].y, stroke.strokeWidth, paint)
        } else if (pts.size == 2) {
            canvas.drawLine(pts[0].x, pts[0].y, pts[1].x, pts[1].y, paint)
        } else if (pts.size > 2) {
            val path = AndroidPath()
            path.moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size - 1) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val midX = (p0.x + p1.x) / 2f
                val midY = (p0.y + p1.y) / 2f
                path.quadTo(p0.x, p0.y, midX, midY)
            }
            path.lineTo(pts.last().x, pts.last().y)
            canvas.drawPath(path, paint)
        }
    }

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
    val bytes = stream.toByteArray()
    return "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}
