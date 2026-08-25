package com.example.bizops.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class LogoPreset(
    val id: String,
    val name: String,
    val primaryColor: Int,
    val secondaryColor: Int,
    val initials: String,
    val shape: LogoShape
)

enum class LogoShape {
    HEXAGON, CIRCLE, DIAMOND, SHIELD, ROUNDED_SQUARE
}

object LogoPresetManager {

    val COMPANY_PRESETS = listOf(
        LogoPreset("preset:apex", "Apex Ops", Color.rgb(37, 99, 235), Color.rgb(30, 41, 59), "AX", LogoShape.HEXAGON),
        LogoPreset("preset:tech_flow", "TechFlow", Color.rgb(13, 148, 136), Color.rgb(15, 23, 42), "TF", LogoShape.ROUNDED_SQUARE),
        LogoPreset("preset:global_corp", "Global Enterprise", Color.rgb(79, 70, 229), Color.rgb(30, 58, 138), "GE", LogoShape.SHIELD),
        LogoPreset("preset:shield_ops", "Vanguard Defense", Color.rgb(225, 29, 72), Color.rgb(159, 18, 57), "VD", LogoShape.DIAMOND),
        LogoPreset("preset:geometric_star", "Nova Labs", Color.rgb(217, 119, 6), Color.rgb(180, 83, 9), "NL", LogoShape.CIRCLE)
    )

    val CLIENT_PRESETS = listOf(
        LogoPreset("preset:client_corp", "Nexus Dynamics", Color.rgb(14, 116, 144), Color.rgb(21, 94, 117), "ND", LogoShape.ROUNDED_SQUARE),
        LogoPreset("preset:client_cube", "Vanguard Logistics", Color.rgb(101, 163, 13), Color.rgb(63, 98, 18), "VL", LogoShape.HEXAGON),
        LogoPreset("preset:client_sphere", "Solaria Bio", Color.rgb(147, 51, 234), Color.rgb(107, 33, 168), "SB", LogoShape.CIRCLE),
        LogoPreset("preset:client_diamond", "Aura Fintech", Color.rgb(2, 132, 199), Color.rgb(3, 105, 161), "AF", LogoShape.DIAMOND),
        LogoPreset("preset:client_star", "Summit Holdings", Color.rgb(234, 88, 12), Color.rgb(194, 65, 12), "SH", LogoShape.SHIELD)
    )

    /**
     * Converts a Uri into a Base64 encoded PNG/JPEG string.
     */
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Resize if too large to prevent DB bloat
                val scaled = scaleBitmapDown(bitmap, 400)
                val outputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                val bytes = outputStream.toByteArray()
                "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes a logo string (preset or base64) into an Android Bitmap.
     */
    fun getLogoBitmap(logoString: String?, size: Int = 160): Bitmap {
        if (logoString.isNullOrBlank()) {
            return generatePresetBitmap(COMPANY_PRESETS.first(), size)
        }

        if (logoString.startsWith("data:image") || logoString.length > 100) {
            try {
                val cleanBase64 = if (logoString.contains(",")) logoString.substringAfter(",") else logoString
                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (decoded != null) {
                    return scaleBitmapDown(decoded, size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Check company presets
        val compPreset = COMPANY_PRESETS.find { it.id == logoString }
        if (compPreset != null) {
            return generatePresetBitmap(compPreset, size)
        }

        // Check client presets
        val clientPreset = CLIENT_PRESETS.find { it.id == logoString }
        if (clientPreset != null) {
            return generatePresetBitmap(clientPreset, size)
        }

        // Fallback default
        return generatePresetBitmap(COMPANY_PRESETS.first(), size)
    }

    /**
     * Generates a sleek, high-resolution vector emblem bitmap for a preset.
     */
    fun generatePresetBitmap(preset: LogoPreset, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val padding = size * 0.08f
        val rect = RectF(padding, padding, size - padding, size - padding)

        // Draw shape background
        paint.color = preset.primaryColor
        paint.style = Paint.Style.FILL

        when (preset.shape) {
            LogoShape.CIRCLE -> {
                canvas.drawCircle(size / 2f, size / 2f, (size - 2 * padding) / 2f, paint)
            }
            LogoShape.ROUNDED_SQUARE -> {
                canvas.drawRoundRect(rect, size * 0.22f, size * 0.22f, paint)
            }
            LogoShape.HEXAGON -> {
                val path = Path()
                val cx = size / 2f
                val cy = size / 2f
                val r = (size - 2 * padding) / 2f
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60 * i - 30).toDouble())
                    val x = (cx + r * Math.cos(angle)).toFloat()
                    val y = (cy + r * Math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            LogoShape.DIAMOND -> {
                val path = Path()
                path.moveTo(size / 2f, padding)
                path.lineTo(size - padding, size / 2f)
                path.lineTo(size / 2f, size - padding)
                path.lineTo(padding, size / 2f)
                path.close()
                canvas.drawPath(path, paint)
            }
            LogoShape.SHIELD -> {
                val path = Path()
                path.moveTo(padding, padding + size * 0.1f)
                path.lineTo(size - padding, padding + size * 0.1f)
                path.lineTo(size - padding, size * 0.55f)
                path.quadTo(size - padding, size - padding, size / 2f, size - padding)
                path.quadTo(padding, size - padding, padding, size * 0.55f)
                path.close()
                canvas.drawPath(path, paint)
            }
        }

        // Inner accent border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.04f
        paint.color = Color.argb(80, 255, 255, 255)
        canvas.drawCircle(size / 2f, size / 2f, (size - 2 * padding) * 0.38f, paint)

        // Initials Text
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = size * 0.36f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER

        val textBounds = android.graphics.Rect()
        paint.getTextBounds(preset.initials, 0, preset.initials.length, textBounds)
        val textY = (size / 2f) + (textBounds.height() / 2f) - 2f
        canvas.drawText(preset.initials, size / 2f, textY, paint)

        return bitmap
    }

    /**
     * Decodes signature base64 or generates a cursive signature bitmap.
     */
    fun getSignatureBitmap(signatureData: String?, signatoryName: String, width: Int = 300, height: Int = 100): Bitmap {
        if (!signatureData.isNullOrBlank() && (signatureData.startsWith("data:image") || signatureData.length > 50)) {
            try {
                val cleanBase64 = if (signatureData.contains(",")) signatureData.substringAfter(",") else signatureData
                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (decoded != null) {
                    return Bitmap.createScaledBitmap(decoded, width, height, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Generate cursive signature bitmap from name
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val name = signatoryName.ifBlank { "Jordan Vance" }
        paint.color = Color.rgb(29, 78, 216) // Blue ink
        paint.textSize = height * 0.42f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        paint.textAlign = Paint.Align.CENTER

        canvas.drawText(name, width / 2f, height * 0.55f, paint)

        // Draw cursive flourish stroke
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(29, 78, 216)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val path = Path().apply {
            moveTo(width * 0.15f, height * 0.72f)
            quadTo(width * 0.45f, height * 0.85f, width * 0.85f, height * 0.68f)
        }
        canvas.drawPath(path, strokePaint)

        return bitmap
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight = (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true)
    }
}
