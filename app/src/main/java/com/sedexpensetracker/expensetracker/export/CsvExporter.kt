package com.sedexpensetracker.expensetracker.export

import android.content.Context
import android.net.Uri
import com.sedexpensetracker.expensetracker.data.models.TransactionModel
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import java.io.File

object CsvExporter {

    fun generateCsvContent(transactions: List<TransactionModel>, currencySymbol: String = "$", dateFormatPattern: String = "MMM dd, yyyy"): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Time,Title,Type,Category,Amount,Payment Method,Notes\n")

        for (tx in transactions) {
            val date = CurrencyFormatter.formatDate(tx.dateTimeEpochMillis, dateFormatPattern)
            val time = CurrencyFormatter.formatTime(tx.dateTimeEpochMillis)
            val amountStr = (tx.amountMinorUnits / 100.0).toString()
            val escapedTitle = tx.title.replace("\"", "\"\"")
            val escapedNotes = (tx.notes ?: "").replace("\"", "\"\"")

            sb.append("${tx.id},")
            sb.append("\"$date\",")
            sb.append("\"$time\",")
            sb.append("\"$escapedTitle\",")
            sb.append("\"${tx.type}\",")
            sb.append("\"${tx.categoryName}\",")
            sb.append("$amountStr,")
            sb.append("\"${tx.paymentMethod ?: ""}\",")
            sb.append("\"$escapedNotes\"\n")
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
}
