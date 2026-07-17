package com.trackfinz.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.trackfinz.app.data.model.TransactionCategory
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ReceiptData(
    val merchantName: String,
    val totalAmount: Double,
    val date: Long,
    val items: List<String>,
    val suggestedCategory: TransactionCategory,
    val rawText: String
)

object ReceiptScanner {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Scan receipt image and extract data using ML Kit OCR
     */
    suspend fun scanReceipt(context: Context, imageUri: Uri): ReceiptData? {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            val visionText = textRecognizer.process(image).await()
            val rawText = visionText.text

            if (rawText.isBlank()) return null

            // Extract data from OCR text
            val merchantName = extractMerchantName(rawText)
            val totalAmount = extractTotalAmount(rawText)
            val date = extractDate(rawText)
            val items = extractItems(rawText)
            
            // Use SmartCategorizer for AI-powered category suggestion
            val suggestedCategory = SmartCategorizer.suggestCategory(
                context = context,
                title = merchantName,
                amount = totalAmount,
                type = com.trackfinz.app.data.model.TransactionType.EXPENSE
            )

            ReceiptData(
                merchantName = merchantName,
                totalAmount = totalAmount,
                date = date,
                items = items,
                suggestedCategory = suggestedCategory,
                rawText = rawText
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Save bitmap to app's internal storage
     */
    suspend fun saveReceiptImage(context: Context, bitmap: Bitmap): String? {
        return suspendCoroutine { continuation ->
            try {
                val timestamp = System.currentTimeMillis()
                val fileName = "receipt_$timestamp.jpg"
                val file = File(context.filesDir, "receipts").apply { mkdirs() }
                val receiptFile = File(file, fileName)

                FileOutputStream(receiptFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                continuation.resume(receiptFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume(null)
            }
        }
    }

    // ── Private extraction methods ────────────────────────────────────────────

    private fun extractMerchantName(text: String): String {
        // Try to find merchant name in first few lines
        val lines = text.lines().filter { it.trim().isNotEmpty() }
        if (lines.isEmpty()) return "Unknown Merchant"

        // First non-empty line is usually the merchant name
        val firstLine = lines.first().trim()
        
        // Clean up common receipt header patterns
        val cleaned = firstLine
            .replace(Regex("receipt|invoice|bill", RegexOption.IGNORE_CASE), "")
            .trim()

        return if (cleaned.length > 3) cleaned.take(50) else "Unknown Merchant"
    }

    private fun extractTotalAmount(text: String): Double {
        // Look for patterns like "Total: $50.00", "TOTAL 50.00", "Amount: 50"
        val patterns = listOf(
            Regex("""total[:\s]*\$?(\d+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""amount[:\s]*\$?(\d+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""sum[:\s]*\$?(\d+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""\$(\d+\.?\d*)"""),
            Regex("""(\d+\.\d{2})""") // Any decimal with 2 places
        )

        val amounts = mutableListOf<Double>()
        
        for (pattern in patterns) {
            pattern.findAll(text).forEach { match ->
                match.groupValues.getOrNull(1)?.toDoubleOrNull()?.let { amounts.add(it) }
            }
        }

        // Return the largest amount found (likely the total)
        return amounts.maxOrNull() ?: 0.0
    }

    private fun extractDate(text: String): Long {
        // Try to find date patterns
        val datePatterns = listOf(
            SimpleDateFormat("MM/dd/yyyy", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("MMM dd, yyyy", Locale.US),
            SimpleDateFormat("dd MMM yyyy", Locale.US)
        )

        for (pattern in datePatterns) {
            try {
                val regex = when (pattern.toPattern()) {
                    "MM/dd/yyyy" -> Regex("""\d{2}/\d{2}/\d{4}""")
                    "dd/MM/yyyy" -> Regex("""\d{2}/\d{2}/\d{4}""")
                    "yyyy-MM-dd" -> Regex("""\d{4}-\d{2}-\d{2}""")
                    else -> continue
                }

                regex.find(text)?.value?.let { dateStr ->
                    pattern.parse(dateStr)?.let { date ->
                        return date.time
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }

        // Default to current date if not found
        return System.currentTimeMillis()
    }

    private fun extractItems(text: String): List<String> {
        val lines = text.lines()
        val items = mutableListOf<String>()

        // Look for lines that might be items (have price patterns)
        val itemPattern = Regex("""(.+?)\s+\$?(\d+\.?\d*)""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Skip lines that look like headers or totals
            if (trimmed.matches(Regex(""".*total.*|.*subtotal.*|.*tax.*|.*amount.*""", RegexOption.IGNORE_CASE))) {
                continue
            }

            itemPattern.find(trimmed)?.let { match ->
                val itemName = match.groupValues[1].trim()
                if (itemName.length > 2 && itemName.length < 50) {
                    items.add(itemName)
                }
            }
        }

        return items.take(10) // Limit to 10 items
    }

}
