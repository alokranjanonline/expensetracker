package com.sedexpensetracker.expensetracker.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sedexpensetracker.expensetracker.data.models.CategoryModel
import com.sedexpensetracker.expensetracker.ui.components.CategoryIconHelper
import com.sedexpensetracker.expensetracker.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryModel?>(null) }

    var categoryNameInput by remember { mutableStateOf("") }
    var categoryTypeInput by remember { mutableStateOf("EXPENSE") }
    var selectedIconName by remember { mutableStateOf("Restaurant") }
    var selectedColorHex by remember { mutableStateOf(0xFFFF5722L) }

    val colorOptions = listOf(
        0xFFFF5722L, 0xFF2196F3L, 0xFFE91E63L, 0xFF9C27B0L,
        0xFF673AB7L, 0xFF4CAF50L, 0xFF00BCD4L, 0xFF795548L,
        0xFF009688L, 0xFFFF9800L, 0xFF607D8BL, 0xFF8BC34AL
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                categoryNameInput = ""
                categoryTypeInput = "EXPENSE"
                selectedIconName = "Restaurant"
                selectedColorHex = 0xFFFF5722L
                showAddCategoryDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Custom Category")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            items(uiState.categories) { category ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(category.colorHex).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(category.iconName),
                                    contentDescription = null,
                                    tint = Color(category.colorHex),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(category.name, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${category.type} • ${if (category.isCustom) "Custom" else "Standard"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = {
                            editingCategory = category
                            categoryNameInput = category.name
                            categoryTypeInput = category.type
                            selectedIconName = category.iconName
                            selectedColorHex = category.colorHex
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Category")
                        }

                        IconButton(onClick = { viewModel.deleteCategory(category) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Add Category Dialog
        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Create Custom Category") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = categoryNameInput,
                            onValueChange = { categoryNameInput = it },
                            label = { Text("Category Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = categoryTypeInput == "EXPENSE",
                                onClick = { categoryTypeInput = "EXPENSE" },
                                label = { Text("Expense") }
                            )
                            FilterChip(
                                selected = categoryTypeInput == "INCOME",
                                onClick = { categoryTypeInput = "INCOME" },
                                label = { Text("Income") }
                            )
                        }

                        Text("Select Color", fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            colorOptions.take(6).forEach { colorHex ->
                                Surface(
                                    shape = CircleShape,
                                    color = Color(colorHex),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { selectedColorHex = colorHex }
                                ) {
                                    if (selectedColorHex == colorHex) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Text("Select Icon", fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CategoryIconHelper.availableIcons.take(5).forEach { iconName ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedIconName == iconName) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { selectedIconName = iconName }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(iconName),
                                            contentDescription = null,
                                            tint = if (selectedIconName == iconName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (categoryNameInput.isNotBlank()) {
                                viewModel.addCustomCategory(
                                    name = categoryNameInput,
                                    type = categoryTypeInput,
                                    iconName = selectedIconName,
                                    colorHex = selectedColorHex
                                )
                                categoryNameInput = ""
                                showAddCategoryDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Edit Category Dialog
        editingCategory?.let { categoryToEdit ->
            AlertDialog(
                onDismissRequest = { editingCategory = null },
                title = { Text("Edit Category") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = categoryNameInput,
                            onValueChange = { categoryNameInput = it },
                            label = { Text("Category Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = categoryTypeInput == "EXPENSE",
                                onClick = { categoryTypeInput = "EXPENSE" },
                                label = { Text("Expense") }
                            )
                            FilterChip(
                                selected = categoryTypeInput == "INCOME",
                                onClick = { categoryTypeInput = "INCOME" },
                                label = { Text("Income") }
                            )
                        }

                        Text("Select Color", fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            colorOptions.take(6).forEach { colorHex ->
                                Surface(
                                    shape = CircleShape,
                                    color = Color(colorHex),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { selectedColorHex = colorHex }
                                ) {
                                    if (selectedColorHex == colorHex) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Text("Select Icon", fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CategoryIconHelper.availableIcons.take(5).forEach { iconName ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedIconName == iconName) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { selectedIconName = iconName }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(iconName),
                                            contentDescription = null,
                                            tint = if (selectedIconName == iconName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (categoryNameInput.isNotBlank()) {
                                val updated = categoryToEdit.copy(
                                    name = categoryNameInput,
                                    type = categoryTypeInput,
                                    iconName = selectedIconName,
                                    colorHex = selectedColorHex
                                )
                                viewModel.updateCategory(updated)
                                editingCategory = null
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingCategory = null }) { Text("Cancel") }
                }
            )
        }
    }
}
