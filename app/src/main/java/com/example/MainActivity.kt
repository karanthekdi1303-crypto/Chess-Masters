package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.chess.data.AdManager
import com.example.chess.data.ChessApplication
import com.example.chess.ui.GameScreen
import com.example.chess.ui.HomeScreen
import com.example.chess.ui.OpeningTrainerScreen
import com.example.chess.ui.ProfileScreen
import com.example.chess.viewmodel.ChessViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val app = application as ChessApplication
            val viewModel: ChessViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ChessViewModel(app.repository, app) as T
                    }
                }
            )
            val navController = rememberNavController()
            val stats by viewModel.stats.collectAsState()
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

            // Root security lock: if they are not logged in/unlink their email, they are booted to Login Page
            androidx.compose.runtime.LaunchedEffect(stats.email, currentRoute) {
                if (stats.email.isEmpty() && currentRoute != null && currentRoute != "welcome") {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            NavHost(navController = navController, startDestination = "welcome") {
                composable("welcome") {
                    com.example.chess.ui.LoginScreen(
                        viewModel = viewModel,
                        onNavigateToHome = {
                            navController.navigate("home") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    )
                }
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToGame = {
                            AdManager.showInterstitial(this@MainActivity) {
                                navController.navigate("game")
                            }
                        },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onNavigateToStats = { navController.navigate("stats") },
                        onNavigateToTrainer = {
                            AdManager.showInterstitial(this@MainActivity) {
                                navController.navigate("trainer")
                            }
                        }
                    )
                }
                composable("stats") {
                    com.example.chess.ui.StatsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("trainer") {
                    OpeningTrainerScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("game") {
                    GameScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
      }
    }
  }
}
