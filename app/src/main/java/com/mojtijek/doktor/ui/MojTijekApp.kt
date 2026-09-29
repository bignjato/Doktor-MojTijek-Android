package com.mojtijek.doktor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mojtijek.doktor.ui.home.HomeScreen
import com.mojtijek.doktor.ui.izvjestaji.IzvjestajiScreen
import com.mojtijek.doktor.ui.pracenje.PracenjeScreen
import com.mojtijek.doktor.ui.profil.ProfilScreen
import com.mojtijek.doktor.ui.terapije.TerapijeScreen

private sealed class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Danas : Tab("danas", "Danas", Icons.Filled.Home)
    object Terapije : Tab("terapije", "Terapije", Icons.Filled.Medication)
    object Nalazi : Tab("nalazi", "Nalazi", Icons.Filled.Description)
    object Pracenje : Tab("pracenje", "Praćenje", Icons.Filled.MonitorHeart)
    object Profil : Tab("profil", "Profil", Icons.Filled.Person)
}

private val tabs = listOf(Tab.Danas, Tab.Terapije, Tab.Nalazi, Tab.Pracenje, Tab.Profil)

@Composable
fun MojTijekApp(vm: MojTijekViewModel) {
    val navController = rememberNavController()

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Tab.Danas.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Tab.Danas.route) { HomeScreen(vm) }
            composable(Tab.Terapije.route) { TerapijeScreen(vm) }
            composable(Tab.Nalazi.route) { IzvjestajiScreen(vm) }
            composable(Tab.Pracenje.route) { PracenjeScreen(vm) }
            composable(Tab.Profil.route) { ProfilScreen(vm) }
        }
        
        // Floating pill navigation bar (iOS-style)
        FloatingNavBar(navController)
    }
}

@Composable
private fun BoxScope.FloatingNavBar(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(68.dp),
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                
                NavigationBarItem(
                    icon = {
                        Box(
                            modifier = if (selected) {
                                Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            } else {
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selected) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selected) {
                                    Text(
                                        tab.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    label = {},
                    selected = selected,
                    onClick = {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
