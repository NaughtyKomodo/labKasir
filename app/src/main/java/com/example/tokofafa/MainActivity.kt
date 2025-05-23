package com.example.tokofafa

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tokofafa.bayar.KasirScreen
import com.example.tokofafa.inventori.InventoriScreen
import com.example.tokofafa.produk.AddProductScreen
import com.example.tokofafa.produk.ProductListScreen
import com.example.tokofafa.supplier.SupplierScreen
import com.example.tokofafa.ui.theme.KasirkuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KasirkuTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            Log.d("TokoFafa", "Navigating to HomeScreen")
            HomeScreen(
                onCreateStore = {
                    Log.d("TokoFafa", "onCreateStore triggered, navigating to menu")
                    navController.navigate("menu")
                }
            )
        }
        composable("menu") {
            Log.d("TokoFafa", "Navigating to MenuScreen")
            MenuScreen(navController = navController)
        }
        composable("product_list") {
            Log.d("TokoFafa", "Navigating to ProductListScreen")
            ProductListScreen(navController = navController)
        }
        composable(
            route = "add_product/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.LongType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: -1L
            Log.d("TokoFafa", "Navigating to AddProductScreen with productId: $productId")
            AddProductScreen(navController = navController, productId = productId)
        }
        composable("supplier") {
            Log.d("TokoFafa", "Navigating to SupplierScreen")
            SupplierScreen(navController = navController)
        }
        composable(
            route = "supplier_info/{supplierId}",
            arguments = listOf(navArgument("supplierId") { type = NavType.IntType })
        ) { backStackEntry ->
            val supplierId = backStackEntry.arguments?.getInt("supplierId") ?: -1
            Log.d("TokoFafa", "Navigating to SupplierInfoScreen with supplierId: $supplierId")
            SupplierInfoScreen(navController = navController, supplierId = supplierId)
        }
        composable("inventori") {
            Log.d("TokoFafa", "Navigating to InventoriScreen")
            InventoriScreen(navController = navController)
        }
        composable("kasir") {
            Log.d("TokoFafa", "Navigating to KasirScreen")
            KasirScreen(navController = navController)
        }
    }
}