package com.sedexpensetracker.expensetracker.export

import android.content.Context
import android.net.Uri
import com.sedexpensetracker.expensetracker.data.models.TransactionModel
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun generateCsvContent(transactions: List<TransactionModel>, currencySymbol: String = "$", dateFormatPattern: String = "MMM dd, yyyy"): String {
        val sb = StringBuilder()
        sb.append("Date,Amount,Title,notes,categoryName,type\n")

        for (tx in transactions) {
            val date = CurrencyFormatter.formatDate(tx.dateTimeEpochMillis, dateFormatPattern)
            val amountStr = String.format(Locale.US, "%.2f", tx.amountMinorUnits / 100.0)
            val escapedTitle = tx.title.replace("\"", "\"\"")
            val escapedNotes = (tx.notes ?: "").replace("\"", "\"\"")

            sb.append("\"$date\",")
            sb.append("$amountStr,")
            sb.append("\"$escapedTitle\",")
            sb.append("\"$escapedNotes\",")
            sb.append("\"${tx.categoryName}\",")
            sb.append("\"${tx.type}\"\n")
        }

        return sb.toString()
    }

    fun writeCsvToUri(context: Context, uri: Uri, transactions: List<TransactionModel>, currencySymbol: String, dateFormatPattern: String = "MMM dd, yyyy"): Boolean {
        return try {
            val csvText = generateCsvContent(transactions, currencySymbol, dateFormatPattern)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(csvText.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun parseCsvToTransactions(csvContent: String): List<TransactionModel> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()

        val result = mutableListOf<TransactionModel>()
        val header = lines.first().split(",").map { it.trim().lowercase().replace("\"", "") }

        var dateIdx = header.indexOfFirst { it.contains("date") }
        var amountIdx = header.indexOfFirst { it.contains("amount") }
        var titleIdx = header.indexOfFirst { it.contains("title") }
        var notesIdx = header.indexOfFirst { it.contains("note") }
        var catIdx = header.indexOfFirst { it.contains("category") }
        var typeIdx = header.indexOfFirst { it.contains("type") }

        if (dateIdx == -1) dateIdx = 0
        if (amountIdx == -1) amountIdx = 1
        if (titleIdx == -1) titleIdx = 2
        if (notesIdx == -1) notesIdx = 3
        if (catIdx == -1) catIdx = 4
        if (typeIdx == -1) typeIdx = 5

        val datePatterns = listOf("MMM dd, yyyy", "MMM dd yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy")

        for (line in lines.drop(1)) {
            val columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().replace("\"", "") }
            if (columns.size <= maxOf(dateIdx, amountIdx, titleIdx)) continue

            val dateStr = columns.getOrNull(dateIdx) ?: ""
            val amountStr = columns.getOrNull(amountIdx) ?: "0"
            val titleStr = columns.getOrNull(titleIdx) ?: "Imported Transaction"
            val notesStr = columns.getOrNull(notesIdx)
            val catStr = columns.getOrNull(catIdx) ?: "Other Expense"
            val typeStr = (columns.getOrNull(typeIdx) ?: "EXPENSE").uppercase()

            var parsedMillis = System.currentTimeMillis()
            for (pattern in datePatterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                    val parsedDate = sdf.parse(dateStr)
                    if (parsedDate != null) {
                        parsedMillis = parsedDate.time
                        break
                    }
                } catch (e: Exception) {}
            }

            val amountMinor = CurrencyFormatter.parseToMinorUnits(amountStr)

            result.add(
                TransactionModel(
                    title = titleStr,
                    amountMinorUnits = amountMinor,
                    type = if (typeStr.contains("INCOME")) "INCOME" else "EXPENSE",
                    categoryId = "imported",
                    categoryName = catStr,
                    categoryIcon = if (typeStr.contains("INCOME")) "Payments" else "Restaurant",
                    categoryColor = if (typeStr.contains("INCOME")) 0xFF4CAF50 else 0xFFFF5722,
                    dateTimeEpochMillis = parsedMillis,
                    notes = notesStr?.ifBlank { null },
                    paymentMethod = "Imported"
                )
            )
        }

        return result
    }
}
