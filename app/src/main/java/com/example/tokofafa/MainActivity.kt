package com.example.tokofafa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tokofafa.produk.AddProductScreen
import com.example.tokofafa.produk.ProductListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TokoSayaTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            onCreateStore = {
                                navController.navigate("menu") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("menu") {
                        MenuScreen(navController = navController)
                    }
                    composable("product_list") {
                        ProductListScreen(navController = navController)
                    }
                    composable(
                        "add_product/{productId}",
                        arguments = listOf(navArgument("productId") { defaultValue = "-1" })
                    ) { backStackEntry ->
                        val productId = backStackEntry.arguments?.getString("productId")?.toLongOrNull() ?: -1L
                        AddProductScreen(
                            navController = navController,
                            productId = productId
                        )
                    }
                }
            }
        }
    }
}