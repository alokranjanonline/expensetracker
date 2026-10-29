package com.sedexpensetracker.expensetracker.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.sedexpensetracker.expensetracker.ui.screens.auth.ForgotPasswordScreen
import com.sedexpensetracker.expensetracker.ui.screens.auth.LoginScreen
import com.sedexpensetracker.expensetracker.ui.screens.auth.RegisterScreen
import com.sedexpensetracker.expensetracker.ui.screens.budgets.BudgetsScreen
import com.sedexpensetracker.expensetracker.ui.screens.home.HomeScreen
import com.sedexpensetracker.expensetracker.ui.screens.reports.ReportsScreen
import com.sedexpensetracker.expensetracker.ui.screens.settings.CategoryManagerScreen
import com.sedexpensetracker.expensetracker.ui.screens.settings.SettingsScreen
import com.sedexpensetracker.expensetracker.ui.screens.transactions.AddEditTransactionScreen
import com.sedexpensetracker.expensetracker.ui.screens.transactions.TransactionsScreen
import com.sedexpensetracker.expensetracker.ui.theme.ExpenseTrackerTheme
import com.sedexpensetracker.expensetracker.viewmodel.*

@Composable
fun ExpenseTrackerApp() {
    val context = LocalContext.current
    val factory = remember { AppViewModelFactory(context) }

    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    val authUiState by authViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    val darkTheme = when (settingsUiState.settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    ExpenseTrackerTheme(darkTheme = darkTheme) {
        val rootNavController = rememberNavController()

        if (!authUiState.isLoggedIn) {
            NavHost(
                navController = rootNavController,
                startDestination = Screen.Login.route
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        viewModel = authViewModel,
                        onNavigateToRegister = { rootNavController.navigate(Screen.Register.route) },
                        onNavigateToForgotPassword = { rootNavController.navigate(Screen.ForgotPassword.route) },
                        onLoginSuccess = { }
                    )
                }

                composable(Screen.Register.route) {
                    RegisterScreen(
                        viewModel = authViewModel,
                        onNavigateToLogin = { rootNavController.popBackStack() }
                    )
                }

                composable(Screen.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        viewModel = authViewModel,
                        onNavigateToLogin = { rootNavController.popBackStack() }
                    )
                }
            }
        } else {
            val mainNavController = rememberNavController()
            val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val showBottomBar = Screen.bottomNavItems.any { it.route == currentRoute }

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            Screen.bottomNavItems.forEach { screen ->
                                NavigationBarItem(
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        mainNavController.navigate(screen.route) {
                                            popUpTo(mainNavController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        screen.icon?.let {
                                            Icon(it, contentDescription = screen.title)
                                        }
                                    },
                                    label = {
                                        screen.title?.let { Text(it) }
                                    }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (showBottomBar) {
                        FloatingActionButton(
                            onClick = {
                                mainNavController.navigate(Screen.AddEditTransaction.createRoute())
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = mainNavController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Home.route) {
                        val homeViewModel: HomeViewModel = viewModel(factory = factory)
                        HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToAddTransaction = { type ->
                                mainNavController.navigate(Screen.AddEditTransaction.createRoute(initialType = type))
                            },
                            onNavigateToTransactions = {
                                mainNavController.navigate(Screen.Transactions.route)
                            },
                            onTransactionClick = { tx ->
                                mainNavController.navigate(Screen.AddEditTransaction.createRoute(transactionId = tx.id))
                            }
                        )
                    }

                    composable(Screen.Transactions.route) {
                        val transactionViewModel: TransactionViewModel = viewModel(factory = factory)
                        TransactionsScreen(
                            viewModel = transactionViewModel,
                            onEditTransaction = { id ->
                                mainNavController.navigate(Screen.AddEditTransaction.createRoute(transactionId = id))
                            }
                        )
                    }

                    composable(Screen.Reports.route) {
                        val reportsViewModel: ReportsViewModel = viewModel(factory = factory)
                        ReportsScreen(viewModel = reportsViewModel)
                    }

                    composable(Screen.Budgets.route) {
                        val budgetViewModel: BudgetViewModel = viewModel(factory = factory)
                        BudgetsScreen(viewModel = budgetViewModel)
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateToCategoryManager = {
                                mainNavController.navigate(Screen.CategoryManager.route)
                            },
                            onLogout = {
                                authViewModel.logout()
                            }
                        )
                    }

                    composable(
                        route = Screen.AddEditTransaction.route,
                        arguments = listOf(
                            navArgument("transactionId") { type = NavType.StringType; defaultValue = "" },
                            navArgument("initialType") { type = NavType.StringType; defaultValue = "EXPENSE" }
                        )
                    ) { backStackEntry ->
                        val txIdArg = backStackEntry.arguments?.getString("transactionId")
                        val txId = if (txIdArg.isNullOrBlank()) null else txIdArg
                        val initialType = backStackEntry.arguments?.getString("initialType") ?: "EXPENSE"

                        val addEditViewModel: AddEditTransactionViewModel = viewModel(factory = factory)

                        AddEditTransactionScreen(
                            viewModel = addEditViewModel,
                            transactionId = txId,
                            initialType = initialType,
                            onNavigateBack = { mainNavController.popBackStack() }
                        )
                    }

                    composable(Screen.CategoryManager.route) {
                        CategoryManagerScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = { mainNavController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
