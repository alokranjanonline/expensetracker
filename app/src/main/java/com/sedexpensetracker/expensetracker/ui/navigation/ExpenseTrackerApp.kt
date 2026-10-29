package com.sedexpensetracker.expensetracker.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlinx.coroutines.launch

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

            val mainTabScreens = listOf(
                Screen.Home,
                Screen.Transactions,
                Screen.Reports,
                Screen.Budgets,
                Screen.Settings
            )

            val pagerState = rememberPagerState(initialPage = 0, pageCount = { mainTabScreens.size })
            val coroutineScope = rememberCoroutineScope()

            val isMainTabRoute = currentRoute == "main_tabs" || currentRoute == null || Screen.bottomNavItems.any { it.route == currentRoute }

            Scaffold(
                bottomBar = {
                    if (isMainTabRoute) {
                        NavigationBar {
                            mainTabScreens.forEachIndexed { index, screen ->
                                NavigationBarItem(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    icon = {
                                        screen.icon?.let {
                                            Icon(it, contentDescription = screen.title)
                                        }
                                    },
                                    label = {
                                        screen.title?.let { title ->
                                            Text(
                                                text = title,
                                                maxLines = 1,
                                                softWrap = false,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (isMainTabRoute) {
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = mainNavController,
                        startDestination = "main_tabs"
                    ) {
                        composable("main_tabs") {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                when (page) {
                                    0 -> {
                                        val homeViewModel: HomeViewModel = viewModel(factory = factory)
                                        HomeScreen(
                                            viewModel = homeViewModel,
                                            onNavigateToAddTransaction = { type ->
                                                mainNavController.navigate(Screen.AddEditTransaction.createRoute(initialType = type))
                                            },
                                            onNavigateToTransactions = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(1)
                                                }
                                            },
                                            onTransactionClick = { tx ->
                                                mainNavController.navigate(Screen.AddEditTransaction.createRoute(transactionId = tx.id))
                                            }
                                        )
                                    }
                                    1 -> {
                                        val transactionViewModel: TransactionViewModel = viewModel(factory = factory)
                                        TransactionsScreen(
                                            viewModel = transactionViewModel,
                                            onEditTransaction = { id ->
                                                mainNavController.navigate(Screen.AddEditTransaction.createRoute(transactionId = id))
                                            }
                                        )
                                    }
                                    2 -> {
                                        val reportsViewModel: ReportsViewModel = viewModel(factory = factory)
                                        ReportsScreen(viewModel = reportsViewModel)
                                    }
                                    3 -> {
                                        val budgetViewModel: BudgetViewModel = viewModel(factory = factory)
                                        BudgetsScreen(viewModel = budgetViewModel)
                                    }
                                    4 -> {
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
                                }
                            }
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
}
