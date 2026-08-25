package com.example.bizops.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bizops.data.model.Invoice
import com.example.bizops.data.model.InvoiceStatus
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class MonthlyInvoiceBucket(
    val monthShort: String,       // "Jan", "Feb"
    val yearMonthKey: String,     // "2026-01"
    val fullTitle: String,        // "January 2026"
    val paidAmount: Double,
    val pendingAmount: Double,
    val overdueAmount: Double,
    val draftAmount: Double,
    val totalAmount: Double,
    val invoiceCount: Int,
    val timestamp: Long
)

enum class ChartTimeframe(val label: String, val monthsCount: Int) {
    LAST_6_MONTHS("6 Months", 6),
    LAST_12_MONTHS("1 Year", 12)
}

enum class ChartVisualMode(val label: String) {
    STACKED_STATUS("By Status"),
    TOTAL_TREND("Total Volume")
}

@Composable
fun MonthlyInvoiceChartCard(
    invoices: List<Invoice>,
    currencySymbol: String = "$",
    onNavigateToInvoices: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.LAST_6_MONTHS) }
    var visualMode by remember { mutableStateOf(ChartVisualMode.STACKED_STATUS) }
    var selectedBucketIndex by remember { mutableStateOf<Int?>(null) }

    // Aggregate monthly buckets based on selected timeframe
    val monthlyBuckets = remember(invoices, selectedTimeframe) {
        computeMonthlyBuckets(invoices, selectedTimeframe.monthsCount)
    }

    // Auto-select latest month with data if nothing selected
    LaunchedEffect(monthlyBuckets) {
        if (selectedBucketIndex == null || selectedBucketIndex!! >= monthlyBuckets.size) {
            val lastNonZeroIndex = monthlyBuckets.indexOfLast { it.totalAmount > 0 }
            selectedBucketIndex = if (lastNonZeroIndex != -1) lastNonZeroIndex else (monthlyBuckets.size - 1).coerceAtLeast(0)
        }
    }

    val selectedBucket = selectedBucketIndex?.let { idx ->
        if (idx in monthlyBuckets.indices) monthlyBuckets[idx] else null
    }

    val totalPeriodVolume = remember(monthlyBuckets) { monthlyBuckets.sumOf { it.totalAmount } }
    val totalPaidVolume = remember(monthlyBuckets) { monthlyBuckets.sumOf { it.paidAmount } }
    val totalInvoicesCount = remember(monthlyBuckets) { monthlyBuckets.sumOf { it.invoiceCount } }
    val maxMonthlyVolume = remember(monthlyBuckets) {
        val maxVal = monthlyBuckets.maxOfOrNull { it.totalAmount } ?: 0.0
        if (maxVal > 0) maxVal else 5000.0
    }
    val peakMonth = remember(monthlyBuckets) {
        monthlyBuckets.maxByOrNull { it.totalAmount }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_invoice_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title, Total, & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Monthly Invoice Velocity",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${currencySymbol}${String.format(Locale.US, "%,.0f", totalPeriodVolume)} total in ${selectedTimeframe.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Timeframe Segmented Switcher
                SingleChoiceSegmentedButtonRow(modifier = Modifier.height(32.dp)) {
                    ChartTimeframe.values().forEachIndexed { index, timeframe ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ChartTimeframe.values().size),
                            onClick = {
                                selectedTimeframe = timeframe
                                selectedBucketIndex = null
                            },
                            selected = selectedTimeframe == timeframe,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                activeContentColor = PrimaryBlue,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = if (timeframe == ChartTimeframe.LAST_6_MONTHS) "6M" else "1Y",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-header metric summary cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "Settled",
                    value = "${currencySymbol}${String.format(Locale.US, "%,.0f", totalPaidVolume)}",
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Avg/Month",
                    value = "${currencySymbol}${String.format(Locale.US, "%,.0f", totalPeriodVolume / selectedTimeframe.monthsCount)}",
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                if (peakMonth != null && peakMonth.totalAmount > 0) {
                    MetricPill(
                        label = "Peak (${peakMonth.monthShort})",
                        value = "${currencySymbol}${String.format(Locale.US, "%,.0f", peakMonth.totalAmount)}",
                        color = InfoPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector & Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = SuccessGreen, label = "Paid")
                    LegendItem(color = PrimaryBlue, label = "Pending")
                    LegendItem(color = ErrorRed, label = "Overdue")
                }

                // View Mode Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            visualMode = if (visualMode == ChartVisualMode.STACKED_STATUS) {
                                ChartVisualMode.TOTAL_TREND
                            } else {
                                ChartVisualMode.STACKED_STATUS
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (visualMode == ChartVisualMode.STACKED_STATUS) Icons.Outlined.ShowChart else Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = visualMode.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The Interactive Compose Chart Canvas
            val textMeasurer = rememberTextMeasurer()
            val primaryColor = MaterialTheme.colorScheme.primary
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant

            // Animation state
            var animationTrigger by remember { mutableFloatStateOf(0f) }
            LaunchedEffect(monthlyBuckets, visualMode) {
                animationTrigger = 0f
                animationTrigger = 1f
            }
            val animatedProgress by animateFloatAsState(
                targetValue = animationTrigger,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                label = "chart_bars_anim"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .testTag("chart_canvas_container")
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(monthlyBuckets) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val leftPadding = 50f
                                val rightPadding = 16f
                                val availableWidth = width - leftPadding - rightPadding
                                val barSpacing = availableWidth / monthlyBuckets.size

                                if (offset.x >= leftPadding && offset.x <= width - rightPadding) {
                                    val index = ((offset.x - leftPadding) / barSpacing).toInt()
                                    if (index in monthlyBuckets.indices) {
                                        selectedBucketIndex = index
                                    }
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val leftPadding = 48f
                    val rightPadding = 12f
                    val topPadding = 16f
                    val bottomPadding = 28f

                    val chartHeight = height - topPadding - bottomPadding
                    val chartWidth = width - leftPadding - rightPadding
                    val count = monthlyBuckets.size
                    val slotWidth = chartWidth / count
                    val barWidth = (slotWidth * 0.52f).coerceIn(12f, 32f)

                    // Compute clean Y-axis max scale (rounded up)
                    val yMax = computeYAxisMax(maxMonthlyVolume)
                    val ySteps = 3

                    // 1. Draw horizontal gridlines and Y-axis text labels
                    for (i in 0..ySteps) {
                        val fraction = i.toFloat() / ySteps
                        val yPos = topPadding + chartHeight * (1f - fraction)
                        val value = yMax * fraction

                        // Grid line
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, yPos),
                            end = Offset(width - rightPadding, yPos),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        // Y-axis label (e.g. $0, $5k, $10k)
                        val formattedLabel = formatAxisCurrency(value, currencySymbol)
                        val textLayout = textMeasurer.measure(
                            text = formattedLabel,
                            style = TextStyle(
                                fontSize = 9.sp,
                                color = labelTextColor,
                                fontWeight = FontWeight.Normal
                            )
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                x = (leftPadding - textLayout.size.width - 6f).coerceAtLeast(0f),
                                y = yPos - textLayout.size.height / 2f
                            )
                        )
                    }

                    // 2. Draw Bars / Columns for each month
                    val trendPoints = mutableListOf<Offset>()

                    monthlyBuckets.forEachIndexed { index, bucket ->
                        val slotCenterX = leftPadding + (index * slotWidth) + (slotWidth / 2f)
                        val barLeft = slotCenterX - (barWidth / 2f)
                        val isSelected = index == selectedBucketIndex

                        val paidHeight = (bucket.paidAmount / yMax).toFloat() * chartHeight * animatedProgress
                        val pendingHeight = (bucket.pendingAmount / yMax).toFloat() * chartHeight * animatedProgress
                        val overdueHeight = (bucket.overdueAmount / yMax).toFloat() * chartHeight * animatedProgress
                        val totalHeight = (bucket.totalAmount / yMax).toFloat() * chartHeight * animatedProgress

                        val baselineY = topPadding + chartHeight

                        if (visualMode == ChartVisualMode.STACKED_STATUS) {
                            var currentBottom = baselineY

                            // 1. Paid Segment (Bottom)
                            if (paidHeight > 0) {
                                val topY = currentBottom - paidHeight
                                drawRoundRect(
                                    color = SuccessGreen,
                                    topLeft = Offset(barLeft, topY),
                                    size = Size(barWidth, paidHeight),
                                    cornerRadius = CornerRadius(if (pendingHeight == 0f && overdueHeight == 0f) 6f else 2f)
                                )
                                currentBottom = topY
                            }

                            // 2. Pending Segment (Middle)
                            if (pendingHeight > 0) {
                                val topY = currentBottom - pendingHeight
                                drawRoundRect(
                                    color = PrimaryBlue,
                                    topLeft = Offset(barLeft, topY),
                                    size = Size(barWidth, pendingHeight),
                                    cornerRadius = CornerRadius(if (overdueHeight == 0f) 6f else 2f)
                                )
                                currentBottom = topY
                            }

                            // 3. Overdue Segment (Top)
                            if (overdueHeight > 0) {
                                val topY = currentBottom - overdueHeight
                                drawRoundRect(
                                    color = ErrorRed,
                                    topLeft = Offset(barLeft, topY),
                                    size = Size(barWidth, overdueHeight),
                                    cornerRadius = CornerRadius(6f)
                                )
                            }

                            // If total is 0, draw empty ghost dot/pill
                            if (totalHeight <= 0) {
                                drawRoundRect(
                                    color = gridColor,
                                    topLeft = Offset(barLeft, baselineY - 4f),
                                    size = Size(barWidth, 4f),
                                    cornerRadius = CornerRadius(2f)
                                )
                            }
                        } else {
                            // Total Trend Mode: Solid gradient bar
                            if (totalHeight > 0) {
                                val topY = baselineY - totalHeight
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(PrimaryBlue, AccentTeal)
                                    ),
                                    topLeft = Offset(barLeft, topY),
                                    size = Size(barWidth, totalHeight),
                                    cornerRadius = CornerRadius(6f)
                                )
                            } else {
                                drawRoundRect(
                                    color = gridColor,
                                    topLeft = Offset(barLeft, baselineY - 4f),
                                    size = Size(barWidth, 4f),
                                    cornerRadius = CornerRadius(2f)
                                )
                            }
                        }

                        // Collect point for trend line overlay
                        val topPeakY = (baselineY - totalHeight).coerceAtLeast(topPadding)
                        trendPoints.add(Offset(slotCenterX, topPeakY))

                        // Selection indicator halo / highlight outline
                        if (isSelected) {
                            val activeHeight = max(totalHeight, 4f)
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(barLeft - 3f, baselineY - activeHeight - 3f),
                                size = Size(barWidth + 6f, activeHeight + 6f),
                                cornerRadius = CornerRadius(8f),
                                style = Stroke(width = 2f)
                            )
                        }

                        // X-axis Month Label
                        val monthLabel = bucket.monthShort
                        val textLayout = textMeasurer.measure(
                            text = monthLabel,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = if (isSelected) primaryColor else labelTextColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                x = slotCenterX - textLayout.size.width / 2f,
                                y = baselineY + 6f
                            )
                        )
                    }

                    // 3. Optional Smooth Trend Curve in Total Trend mode
                    if (visualMode == ChartVisualMode.TOTAL_TREND && trendPoints.size > 1 && animatedProgress > 0.5f) {
                        val path = Path()
                        path.moveTo(trendPoints.first().x, trendPoints.first().y)
                        for (i in 1 until trendPoints.size) {
                            val prev = trendPoints[i - 1]
                            val curr = trendPoints[i]
                            val cPoint1 = Offset((prev.x + curr.x) / 2f, prev.y)
                            val cPoint2 = Offset((prev.x + curr.x) / 2f, curr.y)
                            path.cubicTo(cPoint1.x, cPoint1.y, cPoint2.x, cPoint2.y, curr.x, curr.y)
                        }
                        drawPath(
                            path = path,
                            color = PrimaryBlue.copy(alpha = 0.8f),
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                        )

                        // Draw point circles
                        trendPoints.forEachIndexed { idx, point ->
                            val isSel = idx == selectedBucketIndex
                            drawCircle(
                                color = if (isSel) PrimaryBlue else AccentTeal,
                                radius = if (isSel) 5f else 3.5f,
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = if (isSel) 2.5f else 1.5f,
                                center = point
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Month Detailed Interactive Breakdown Box
            AnimatedVisibility(
                visible = selectedBucket != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                if (selectedBucket != null) {
                    SelectedMonthDetailBox(
                        bucket = selectedBucket,
                        currencySymbol = currencySymbol,
                        onViewInvoices = onNavigateToInvoices
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedMonthDetailBox(
    bucket: MonthlyInvoiceBucket,
    currencySymbol: String,
    onViewInvoices: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_month_detail_box")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = bucket.fullTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${bucket.invoiceCount} ${if (bucket.invoiceCount == 1) "invoice" else "invoices"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "${currencySymbol}${String.format(Locale.US, "%,.2f", bucket.totalAmount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-breakdown Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBreakdownChip(
                    label = "Settled",
                    amount = "${currencySymbol}${String.format(Locale.US, "%,.0f", bucket.paidAmount)}",
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                StatusBreakdownChip(
                    label = "Pending",
                    amount = "${currencySymbol}${String.format(Locale.US, "%,.0f", bucket.pendingAmount)}",
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                if (bucket.overdueAmount > 0) {
                    StatusBreakdownChip(
                        label = "Overdue",
                        amount = "${currencySymbol}${String.format(Locale.US, "%,.0f", bucket.overdueAmount)}",
                        color = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBreakdownChip(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Computes consecutive monthly buckets spanning back from the current date.
 */
fun computeMonthlyBuckets(invoices: List<Invoice>, monthsCount: Int): List<MonthlyInvoiceBucket> {
    val calendar = Calendar.getInstance()
    val monthShortFormat = SimpleDateFormat("MMM", Locale.US)
    val yearMonthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    val fullTitleFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

    // Build the ordered list of months from (current - monthsCount + 1) to current
    val buckets = mutableListOf<MonthlyInvoiceBucket>()

    for (i in (monthsCount - 1) downTo 0) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -i)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startOfMonth = cal.timeInMillis
        val calEnd = (cal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }
        val endOfMonth = calEnd.timeInMillis

        val monthShort = monthShortFormat.format(cal.time)
        val yearMonthKey = yearMonthKeyFormat.format(cal.time)
        val fullTitle = fullTitleFormat.format(cal.time)

        // Filter invoices matching this month's issueDate
        val monthInvoices = invoices.filter { inv ->
            inv.issueDate in startOfMonth..endOfMonth
        }

        val paidAmt = monthInvoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.totalAmount }
        val pendingAmt = monthInvoices.filter { it.status == InvoiceStatus.SENT }.sumOf { it.totalAmount }
        val overdueAmt = monthInvoices.filter { it.status == InvoiceStatus.OVERDUE }.sumOf { it.totalAmount }
        val draftAmt = monthInvoices.filter { it.status == InvoiceStatus.DRAFT }.sumOf { it.totalAmount }
        val totalAmt = monthInvoices.sumOf { it.totalAmount }

        buckets.add(
            MonthlyInvoiceBucket(
                monthShort = monthShort,
                yearMonthKey = yearMonthKey,
                fullTitle = fullTitle,
                paidAmount = paidAmt,
                pendingAmount = pendingAmt,
                overdueAmount = overdueAmt,
                draftAmount = draftAmt,
                totalAmount = totalAmt,
                invoiceCount = monthInvoices.size,
                timestamp = startOfMonth
            )
        )
    }

    return buckets
}

private fun computeYAxisMax(peakAmount: Double): Double {
    if (peakAmount <= 0) return 5000.0
    // Round to nearest clean number (e.g. 5000, 10000, 20000)
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(peakAmount)))
    val normalized = peakAmount / magnitude
    val ceilNormalized = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 2.5 -> 2.5
        normalized <= 5.0 -> 5.0
        normalized <= 7.5 -> 7.5
        else -> 10.0
    }
    val ceiling = ceilNormalized * magnitude
    return if (ceiling <= peakAmount) ceiling * 1.25 else ceiling
}

private fun formatAxisCurrency(amount: Double, currencySymbol: String): String {
    return when {
        amount >= 1_000_000 -> "${currencySymbol}${String.format(Locale.US, "%.1fM", amount / 1_000_000)}"
        amount >= 1_000 -> "${currencySymbol}${String.format(Locale.US, "%.0fk", amount / 1_000)}"
        amount == 0.0 -> "${currencySymbol}0"
        else -> "${currencySymbol}${amount.toInt()}"
    }
}
