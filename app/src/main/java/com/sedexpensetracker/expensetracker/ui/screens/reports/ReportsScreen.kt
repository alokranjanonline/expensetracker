package com.sedexpensetracker.expensetracker.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sedexpensetracker.expensetracker.ui.components.BarChart
import com.sedexpensetracker.expensetracker.ui.components.CategoryIconHelper
import com.sedexpensetracker.expensetracker.ui.components.DonutPieChart
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import com.sedexpensetracker.expensetracker.viewmodel.ReportPeriod
import com.sedexpensetracker.expensetracker.viewmodel.ReportsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics", fontWeight = FontWeight.Bold) }
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

            // Period Selector Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedPeriod == ReportPeriod.DAILY,
                        onClick = { viewModel.onPeriodChange(ReportPeriod.DAILY) },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = uiState.selectedPeriod == ReportPeriod.WEEKLY,
                        onClick = { viewModel.onPeriodChange(ReportPeriod.WEEKLY) },
                        label = { Text("7 Days") }
                    )
                    FilterChip(
                        selected = uiState.selectedPeriod == ReportPeriod.MONTHLY,
                        onClick = { viewModel.onPeriodChange(ReportPeriod.MONTHLY) },
                        label = { Text("This Month") }
                    )
                }
            }

            // Overview Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Financial Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Income", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    CurrencyFormatter.formatAmount(uiState.totalIncomeMinorUnits, uiState.currencySymbol),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            Column {
                                Text("Total Expense", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    CurrencyFormatter.formatAmount(uiState.totalExpenseMinorUnits, uiState.currencySymbol),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Net Savings:", fontWeight = FontWeight.Bold)
                            Text(
                                CurrencyFormatter.formatAmount(uiState.netSavingsMinorUnits, uiState.currencySymbol),
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.netSavingsMinorUnits >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Daily Average Expense:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                CurrencyFormatter.formatAmount(uiState.dailyAverageExpenseMinorUnits, uiState.currencySymbol),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Income vs Expense Comparison Bar Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Income vs Expense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = Color(0xFF4CAF50), shape = RoundedCornerShape(2.dp), modifier = Modifier.size(10.dp)) {}
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Income", style = MaterialTheme.typography.labelSmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = Color(0xFFF44336), shape = RoundedCornerShape(2.dp), modifier = Modifier.size(10.dp)) {}
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Expense", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        BarChart(points = uiState.barChartPoints)
                    }
                }
            }

            // Category Expense Breakdown Donut Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Expense Breakdown by Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(16.dp))

                        DonutPieChart(
                            slices = uiState.pieSlices,
                            centerText = CurrencyFormatter.formatAmount(uiState.totalExpenseMinorUnits, uiState.currencySymbol),
                            centerSubText = "Spent"
                        )
                    }
                }
            }

            // Top Spending Categories Header
            item {
                Text("Top Spending Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(uiState.topSpendingCategories) { slice ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(slice.categoryColor).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(slice.categoryIcon),
                                    contentDescription = null,
                                    tint = Color(slice.categoryColor),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(slice.categoryName, fontWeight = FontWeight.Bold)
                            Text("${String.format(Locale.US, "%.1f", slice.percentage)}% of total expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            CurrencyFormatter.formatAmount(slice.amountMinorUnits, uiState.currencySymbol),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
