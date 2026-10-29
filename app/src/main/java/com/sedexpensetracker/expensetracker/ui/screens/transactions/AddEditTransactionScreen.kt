package com.sedexpensetracker.expensetracker.ui.screens.transactions

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sedexpensetracker.expensetracker.ui.components.CategoryIconHelper
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import com.sedexpensetracker.expensetracker.viewmodel.AddEditTransactionViewModel
import com.sedexpensetracker.expensetracker.viewmodel.FocusTarget
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: AddEditTransactionViewModel,
    transactionId: String? = null,
    initialType: String = "EXPENSE",
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val amountFocusRequester = remember { FocusRequester() }
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(transactionId) {
        if (!transactionId.isNullOrBlank()) {
            viewModel.loadExistingTransaction(transactionId)
        } else if (initialType.isNotBlank()) {
            viewModel.onTypeChange(initialType)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            when (uiState.focusTarget) {
                FocusTarget.AMOUNT -> amountFocusRequester.requestFocus()
                FocusTarget.TITLE -> titleFocusRequester.requestFocus()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == null) "Add Transaction" else "Edit Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Success Dialog
            if (uiState.saveSuccess) {
                AlertDialog(
                    onDismissRequest = { },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(48.dp)) },
                    title = { Text("Transaction Saved", fontWeight = FontWeight.Bold) },
                    text = { Text("Your transaction has been saved successfully!") },
                    confirmButton = {
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Error Banner
            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }

            // Type Segmented Toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.type == "EXPENSE",
                    onClick = { viewModel.onTypeChange("EXPENSE") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Expense")
                }
                SegmentedButton(
                    selected = uiState.type == "INCOME",
                    onClick = { viewModel.onTypeChange("INCOME") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Income")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DATE SELECTOR SECTION (ABOVE AMOUNT BOX)
            Text(
                text = "Transaction Date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewModel.stepDateByDays(-1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val calendar = Calendar.getInstance().apply {
                                    timeInMillis = uiState.dateTimeEpochMillis
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val cal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        viewModel.onDateTimeChange(cal.timeInMillis)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = CurrencyFormatter.formatDate(uiState.dateTimeEpochMillis, uiState.dateFormat),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.stepDateByDays(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input - Primary validation focus target
            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = { viewModel.onAmountChange(it) },
                label = { Text("Amount (${uiState.currencySymbol})") },
                leadingIcon = { Text(uiState.currencySymbol, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title Input
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Transaction Title") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Picker Grid
            Text("Select Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.categories.chunked(3).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (category in rowCategories) {
                            val isSelected = uiState.selectedCategory?.id == category.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(category.colorHex) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.onCategoryChange(category) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(category.iconName),
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(category.colorHex),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = category.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes Input
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text("Optional Notes") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Recurring Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Recurring Transaction", fontWeight = FontWeight.Bold)
                    Text("Repeat this entry automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = uiState.isRecurring,
                    onCheckedChange = { viewModel.onRecurringChange(it) }
                )
            }

            if (uiState.isRecurring) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { interval ->
                        FilterChip(
                            selected = uiState.recurrenceInterval == interval,
                            onClick = { viewModel.onRecurrenceIntervalChange(interval) },
                            label = { Text(interval) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveTransaction() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
