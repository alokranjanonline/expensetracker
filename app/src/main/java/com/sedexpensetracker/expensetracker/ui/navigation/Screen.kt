package com.sedexpensetracker.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    object Home : Screen("home", "Home", Icons.Default.Home)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.Receipt)
    object Reports : Screen("reports", "Reports", Icons.Default.BarChart)
    object Budgets : Screen("budgets", "Budgets", Icons.Default.PieChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    object AddEditTransaction : Screen("add_edit_transaction?transactionId={transactionId}&initialType={initialType}") {
        fun createRoute(transactionId: String? = null, initialType: String = "EXPENSE"): String {
            return "add_edit_transaction?transactionId=${transactionId ?: ""}&initialType=$initialType"
        }
    }
    object CategoryManager : Screen("category_manager")

    companion object {
        val bottomNavItems = listOf(Home, Transactions, Reports, Budgets, Settings)
    }
}
