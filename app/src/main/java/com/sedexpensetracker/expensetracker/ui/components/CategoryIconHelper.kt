package com.sedexpensetracker.expensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    fun getIcon(iconName: String): ImageVector {
        return when (iconName) {
            "Restaurant" -> Icons.Default.Restaurant
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "ShoppingBag" -> Icons.Default.ShoppingBag
            "Receipt" -> Icons.Default.Receipt
            "Movie" -> Icons.Default.Movie
            "LocalHospital" -> Icons.Default.LocalHospital
            "School" -> Icons.Default.School
            "Home" -> Icons.Default.Home
            "Flight" -> Icons.Default.Flight
            "SelfImprovement" -> Icons.Default.SelfImprovement
            "Payments" -> Icons.Default.Payments
            "Work" -> Icons.Default.Work
            "Store" -> Icons.Default.Store
            "TrendingUp" -> Icons.Default.TrendingUp
            "CardGiftcard" -> Icons.Default.CardGiftcard
            "AttachMoney" -> Icons.Default.AttachMoney
            "Fastfood" -> Icons.Default.Fastfood
            "LocalGroceryStore" -> Icons.Default.LocalGroceryStore
            "FitnessCenter" -> Icons.Default.FitnessCenter
            "Pets" -> Icons.Default.Pets
            "SportsEsports" -> Icons.Default.SportsEsports
            "DirectionsBus" -> Icons.Default.DirectionsBus
            "Build" -> Icons.Default.Build
            else -> Icons.Default.Category
        }
    }

    val availableIcons = listOf(
        "Restaurant", "DirectionsCar", "ShoppingBag", "Receipt", "Movie",
        "LocalHospital", "School", "Home", "Flight", "SelfImprovement",
        "Payments", "Work", "Store", "TrendingUp", "CardGiftcard",
        "AttachMoney", "Fastfood", "LocalGroceryStore", "FitnessCenter",
        "Pets", "SportsEsports", "DirectionsBus", "Build"
    )
}
