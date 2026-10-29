package com.sedexpensetracker.expensetracker.ui.screens.transactions

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sedexpensetracker.expensetracker.data.models.TransactionModel
import com.sedexpensetracker.expensetracker.ui.screens.home.TransactionItemRow
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import com.sedexpensetracker.expensetracker.viewmodel.SortOption
import com.sedexpensetracker.expensetracker.viewmodel.TransactionViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionViewModel,
    onEditTransaction: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showFilterSheet by remember { mutableStateOf(false) }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var selectedDetailTx by remember { mutableStateOf<TransactionModel?>(null) }
    var deletingTransaction by remember { mutableStateOf<TransactionModel?>(null) }

    var tempStartMillis by remember { mutableStateOf<Long?>(null) }
    var tempEndMillis by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search transactions or notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ORDER: Expense, Income, All (as requested in requirement #2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.selectedTypeFilter == "EXPENSE",
                    onClick = { viewModel.onTypeFilterChange("EXPENSE") },
                    label = { Text("Expense") }
                )
                FilterChip(
                    selected = uiState.selectedTypeFilter == "INCOME",
                    onClick = { viewModel.onTypeFilterChange("INCOME") },
                    label = { Text("Income") }
                )
                FilterChip(
                    selected = uiState.selectedTypeFilter == "ALL",
                    onClick = { viewModel.onTypeFilterChange("ALL") },
                    label = { Text("All") }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Date Range Button (Opens Start Date & End Date selection dialog)
                IconButton(onClick = {
                    tempStartMillis = uiState.startDateMillis ?: System.currentTimeMillis()
                    tempEndMillis = uiState.endDateMillis ?: tempStartMillis
                    showDateRangeDialog = true
                }) {
                    Icon(
                        imageVector = if (uiState.startDateMillis != null) Icons.Default.DateRange else Icons.Default.CalendarMonth,
                        contentDescription = "Select Date Range",
                        tint = if (uiState.startDateMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.startDateMillis != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Range: ${CurrencyFormatter.formatDate(uiState.startDateMillis!!, uiState.dateFormat)} - ${CurrencyFormatter.formatDate(uiState.endDateMillis ?: uiState.startDateMillis!!, uiState.dateFormat)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { viewModel.onDateRangeChange(null, null) }) {
                        Text("Clear Range")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.groupedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No transactions found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.groupedTransactions.forEach { group ->
                        item {
                            Text(
                                text = group.dateString,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(group.transactions) { tx ->
                            TransactionItemRow(
                                transaction = tx,
                                currencySymbol = uiState.currencySymbol,
                                onClick = { selectedDetailTx = tx }
                            )
                        }
                    }
                }
            }
        }

        // Two Text Selector Boxes Dialog for Start Date and End Date
        if (showDateRangeDialog) {
            AlertDialog(
                onDismissRequest = { showDateRangeDialog = false },
                title = { Text("Select Date Range", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Select Start Date and End Date to filter transactions for that period:", style = MaterialTheme.typography.bodyMedium)

                        // Start Date Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = tempStartMillis ?: System.currentTimeMillis()
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val startCal = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth, 0, 0, 0)
                                            }
                                            tempStartMillis = startCal.timeInMillis
                                            if (tempEndMillis == null || tempEndMillis!! < startCal.timeInMillis) {
                                                val endCal = Calendar.getInstance().apply {
                                                    set(year, month, dayOfMonth, 23, 59, 59)
                                                }
                                                tempEndMillis = endCal.timeInMillis
                                            }
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (tempStartMillis != null) CurrencyFormatter.formatDate(tempStartMillis!!, uiState.dateFormat) else "Select Start Date",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // End Date Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = tempEndMillis ?: tempStartMillis ?: System.currentTimeMillis()
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val endCal = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth, 23, 59, 59)
                                            }
                                            tempEndMillis = endCal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("End Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (tempEndMillis != null) CurrencyFormatter.formatDate(tempEndMillis!!, uiState.dateFormat) else "Select End Date",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempStartMillis != null) {
                                val start = tempStartMillis!!
                                val end = tempEndMillis ?: start
                                viewModel.onDateRangeChange(start, end)
                            }
                            showDateRangeDialog = false
                        }
                    ) {
                        Text("Apply Range", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Transaction Detail Dialog
        selectedDetailTx?.let { tx ->
            AlertDialog(
                onDismissRequest = { selectedDetailTx = null },
                title = { Text(tx.title, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Amount: ${CurrencyFormatter.formatAmount(tx.amountMinorUnits, uiState.currencySymbol)}", fontWeight = FontWeight.Bold)
                        Text("Type: ${tx.type}")
                        Text("Category: ${tx.categoryName}")
                        Text("Date: ${CurrencyFormatter.formatDateWithTime(tx.dateTimeEpochMillis, uiState.dateFormat)}")
                        Text("Payment Method: ${tx.paymentMethod ?: "Cash"}")
                        if (!tx.notes.isNullOrBlank()) {
                            Text("Notes: ${tx.notes}")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val id = tx.id
                        selectedDetailTx = null
                        onEditTransaction(id)
                    }) {
                        Text("Edit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        deletingTransaction = tx
                        selectedDetailTx = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }

        // Delete Confirmation Dialog (Requirement #1)
        deletingTransaction?.let { txToDelete ->
            AlertDialog(
                onDismissRequest = { deletingTransaction = null },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)) },
                title = { Text("Delete Transaction?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete '${txToDelete.title}'? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTransaction(txToDelete)
                            deletingTransaction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingTransaction = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Filter Options Sheet
        if (showFilterSheet) {
            AlertDialog(
                onDismissRequest = { showFilterSheet = false },
                title = { Text("Filter & Sort Transactions") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Sort Order", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.sortOption == SortOption.NEWEST,
                                onClick = { viewModel.onSortOptionChange(SortOption.NEWEST) },
                                label = { Text("Newest") }
                            )
                            FilterChip(
                                selected = uiState.sortOption == SortOption.OLDEST,
                                onClick = { viewModel.onSortOptionChange(SortOption.OLDEST) },
                                label = { Text("Oldest") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.sortOption == SortOption.HIGHEST_AMOUNT,
                                onClick = { viewModel.onSortOptionChange(SortOption.HIGHEST_AMOUNT) },
                                label = { Text("Highest") }
                            )
                            FilterChip(
                                selected = uiState.sortOption == SortOption.LOWEST_AMOUNT,
                                onClick = { viewModel.onSortOptionChange(SortOption.LOWEST_AMOUNT) },
                                label = { Text("Lowest") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showFilterSheet = false }) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.resetFilters()
                        showFilterSheet = false
                    }) {
                        Text("Reset")
                    }
                }
            )
        }
    }
}
