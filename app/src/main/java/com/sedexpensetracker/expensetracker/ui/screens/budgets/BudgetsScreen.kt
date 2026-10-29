package com.sedexpensetracker.expensetracker.ui.screens.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sedexpensetracker.expensetracker.data.database.entity.CategoryEntity
import com.sedexpensetracker.expensetracker.ui.components.CategoryIconHelper
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import com.sedexpensetracker.expensetracker.viewmodel.BudgetStatus
import com.sedexpensetracker.expensetracker.viewmodel.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(viewModel: BudgetViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    var showOverallBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForBudget by remember { mutableStateOf<CategoryEntity?>(null) }
    var budgetInputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Management", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Overall Budget Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Monthly Overall Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Month: ${uiState.yearMonth}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                            IconButton(onClick = {
                                budgetInputText = if (uiState.overallBudgetAmountMinorUnits > 0) (uiState.overallBudgetAmountMinorUnits / 100.0).toString() else ""
                                showOverallBudgetDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Overall Budget")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = CurrencyFormatter.formatAmount(uiState.overallBudgetAmountMinorUnits, uiState.currencySymbol),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Spent: ${CurrencyFormatter.formatAmount(uiState.overallSpentAmountMinorUnits, uiState.currencySymbol)}", style = MaterialTheme.typography.bodyMedium)
                            Text("Remaining: ${CurrencyFormatter.formatAmount(uiState.overallRemainingMinorUnits, uiState.currencySymbol)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (uiState.overallUsagePercentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = when (uiState.overallStatus) {
                                BudgetStatus.EXCEEDED -> Color(0xFFF44336)
                                BudgetStatus.WARNING -> Color(0xFFFF9800)
                                BudgetStatus.NORMAL -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            item {
                Text("Category Budgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(uiState.categoryBudgets) { catBudget ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(catBudget.category.colorHex).copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(catBudget.category.iconName),
                                        contentDescription = null,
                                        tint = Color(catBudget.category.colorHex),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(catBudget.category.name, fontWeight = FontWeight.Bold)
                                Text(
                                    if (catBudget.budgetAmountMinorUnits > 0)
                                        "${CurrencyFormatter.formatAmount(catBudget.spentAmountMinorUnits, uiState.currencySymbol)} / ${CurrencyFormatter.formatAmount(catBudget.budgetAmountMinorUnits, uiState.currencySymbol)}"
                                    else "No budget set",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = {
                                selectedCategoryForBudget = catBudget.category
                                budgetInputText = if (catBudget.budgetAmountMinorUnits > 0) (catBudget.budgetAmountMinorUnits / 100.0).toString() else ""
                            }) {
                                Icon(if (catBudget.budgetAmountMinorUnits > 0) Icons.Default.Edit else Icons.Default.Add, contentDescription = "Set Budget")
                            }
                        }

                        if (catBudget.budgetAmountMinorUnits > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { (catBudget.usagePercentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = when (catBudget.status) {
                                    BudgetStatus.EXCEEDED -> Color(0xFFF44336)
                                    BudgetStatus.WARNING -> Color(0xFFFF9800)
                                    BudgetStatus.NORMAL -> Color(0xFF4CAF50)
                                }
                            )

                            if (catBudget.status != BudgetStatus.NORMAL) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (catBudget.status == BudgetStatus.EXCEEDED) "Budget Exceeded!" else "Approaching Budget Limit (>80%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (catBudget.status == BudgetStatus.EXCEEDED) Color(0xFFF44336) else Color(0xFFFF9800)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // Set Overall Budget Dialog
        if (showOverallBudgetDialog) {
            AlertDialog(
                onDismissRequest = { showOverallBudgetDialog = false },
                title = { Text("Set Overall Monthly Budget") },
                text = {
                    OutlinedTextField(
                        value = budgetInputText,
                        onValueChange = { budgetInputText = it },
                        label = { Text("Budget Amount (${uiState.currencySymbol})") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.setOverallBudget(budgetInputText)
                        showOverallBudgetDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOverallBudgetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Set Category Budget Dialog
        selectedCategoryForBudget?.let { cat ->
            AlertDialog(
                onDismissRequest = { selectedCategoryForBudget = null },
                title = { Text("Set Budget for ${cat.name}") },
                text = {
                    OutlinedTextField(
                        value = budgetInputText,
                        onValueChange = { budgetInputText = it },
                        label = { Text("Budget Amount (${uiState.currencySymbol})") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.setCategoryBudget(cat.id, budgetInputText)
                        selectedCategoryForBudget = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedCategoryForBudget = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
