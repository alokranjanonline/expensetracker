package com.sedexpensetracker.expensetracker.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sedexpensetracker.expensetracker.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToCategoryManager: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showSampleCsvDialog by remember { mutableStateOf(false) }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportToCsv(context, it) }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.backupData(context, it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreData(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = uiState.userDisplayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(uiState.userDisplayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(uiState.userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))

                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (uiState.isUserVerified) Color(0xFF2E7D32) else Color(0xFFFF9800)
                        ) {
                            Text(
                                text = if (uiState.isUserVerified) "Verified Account" else "Verification Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Theme Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("App Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (key, label) ->
                            FilterChip(
                                selected = uiState.settings.themeMode == key,
                                onClick = { viewModel.onThemeChange(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Currency, Date Format & Categories Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Currency Symbol") },
                        supportingContent = { Text("${uiState.settings.currencySymbol} (${uiState.settings.currencyCode})") },
                        leadingContent = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        modifier = Modifier.clickable { showCurrencyDialog = true }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Date Display Format") },
                        supportingContent = { Text(uiState.settings.dateFormat) },
                        leadingContent = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        modifier = Modifier.clickable { showDateFormatDialog = true }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Manage Categories") },
                        supportingContent = { Text("Add, edit or remove custom categories") },
                        leadingContent = { Icon(Icons.Default.Category, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToCategoryManager() }
                    )
                }
            }

            // Notifications Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Notification Preferences", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily Expense Reminders")
                        Switch(
                            checked = uiState.settings.dailyReminderEnabled,
                            onCheckedChange = { viewModel.onToggleDailyReminder(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Budget Limit Alerts")
                        Switch(
                            checked = uiState.settings.budgetAlertsEnabled,
                            onCheckedChange = { viewModel.onToggleBudgetAlerts(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recurring Payment Reminders")
                        Switch(
                            checked = uiState.settings.recurringAlertsEnabled,
                            onCheckedChange = { viewModel.onToggleRecurringAlerts(it) }
                        )
                    }
                }
            }

            // Data Export & Backup Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Export to CSV") },
                        supportingContent = { Text("Save transactions for spreadsheet software") },
                        leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                        modifier = Modifier.clickable {
                            csvExportLauncher.launch("expense_tracker_${System.currentTimeMillis()}.csv")
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Backup Data") },
                        supportingContent = { Text("Create a local JSON backup file") },
                        leadingContent = { Icon(Icons.Default.Backup, contentDescription = null) },
                        modifier = Modifier.clickable {
                            backupLauncher.launch("expense_tracker_backup_${System.currentTimeMillis()}.json")
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Restore Data (CSV or JSON)") },
                        supportingContent = { Text("Restore records from a CSV or JSON file") },
                        leadingContent = { Icon(Icons.Default.Restore, contentDescription = null) },
                        modifier = Modifier.clickable {
                            restoreLauncher.launch(arrayOf("*/*"))
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("View Sample CSV Format") },
                        supportingContent = { Text("Required column headers for CSV import") },
                        leadingContent = { Icon(Icons.Default.TableChart, contentDescription = null) },
                        modifier = Modifier.clickable { showSampleCsvDialog = true }
                    )
                }
            }

            // Privacy & Clear Data
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Privacy Policy & Data Storage") },
                        supportingContent = { Text("Financial data synced securely with your account") },
                        leadingContent = { Icon(Icons.Default.PrivacyTip, contentDescription = null) },
                        modifier = Modifier.clickable { showPrivacyDialog = true }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Clear All Data", color = MaterialTheme.colorScheme.error) },
                        supportingContent = { Text("Permanently remove all saved transaction records") },
                        leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { showClearDataDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Sample CSV Format Dialog
        if (showSampleCsvDialog) {
            AlertDialog(
                onDismissRequest = { showSampleCsvDialog = false },
                title = { Text("Sample CSV Format for Restore", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your CSV file must include these headers in the first line:", style = MaterialTheme.typography.bodySmall)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Date,Amount,Title,notes,categoryName,type\nFeb 14 2025,120.50,Grocery Shopping,Weekly groceries,Food & Dining,EXPENSE\nFeb 15 2025,5000.00,Monthly Salary,Salary payout,Salary,INCOME",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Text("Supported date formats: 'MMM dd, yyyy', 'dd/MM/yyyy', 'yyyy-MM-dd', 'MM/dd/yyyy'.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    Button(onClick = { showSampleCsvDialog = false }) {
                        Text("Got It")
                    }
                }
            )
        }

        // Date Format Dialog
        if (showDateFormatDialog) {
            AlertDialog(
                onDismissRequest = { showDateFormatDialog = false },
                title = { Text("Select Date Format") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "MMM dd, yyyy" to "MMM dd, yyyy (e.g., Feb 14, 2025)",
                            "dd/MM/yyyy" to "dd/MM/yyyy (e.g., 14/02/2025)",
                            "yyyy-MM-dd" to "yyyy-MM-dd (e.g., 2025-02-14)",
                            "MM/dd/yyyy" to "MM/dd/yyyy (e.g., 02/14/2025)"
                        ).forEach { (fmt, label) ->
                            TextButton(
                                onClick = {
                                    viewModel.onDateFormatChange(fmt)
                                    showDateFormatDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(label, fontWeight = if (uiState.settings.dateFormat == fmt) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDateFormatDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Currency Selector Dialog
        if (showCurrencyDialog) {
            AlertDialog(
                onDismissRequest = { showCurrencyDialog = false },
                title = { Text("Select Currency") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("$", "USD", "US Dollar ($)"),
                            Triple("₹", "INR", "Indian Rupee (₹)"),
                            Triple("€", "EUR", "Euro (€)"),
                            Triple("£", "GBP", "British Pound (£)"),
                            Triple("C$", "CAD", "Canadian Dollar (C$)"),
                            Triple("A$", "AUD", "Australian Dollar (A$)")
                        ).forEach { (sym, code, label) ->
                            TextButton(
                                onClick = {
                                    viewModel.onCurrencyChange(sym, code)
                                    showCurrencyDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(label, fontWeight = if (uiState.settings.currencySymbol == sym) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCurrencyDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy & Firebase Cloud Sync") },
                text = {
                    Text("Personal Expense Tracker stores your financial records securely in your Firebase Realtime Database cloud instance, protected under your authenticated user ID. Data is accessible only when logged in with your verified credentials.")
                },
                confirmButton = {
                    Button(onClick = { showPrivacyDialog = false }) { Text("Got It") }
                }
            )
        }

        // Clear All Data Confirmation Dialog
        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Clear All Transaction Data?") },
                text = {
                    Text("This action will permanently delete all saved transactions and budget records from your cloud account. This process cannot be undone.", color = MaterialTheme.colorScheme.error)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllData()
                            showClearDataDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
