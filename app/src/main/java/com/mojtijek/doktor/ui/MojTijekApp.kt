package com.mojtijek.doktor.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mojtijek.doktor.ui.home.HomeScreen
import com.mojtijek.doktor.ui.kalendar.KalendarScreen
import com.mojtijek.doktor.ui.pracenje.PracenjeScreen
import com.mojtijek.doktor.ui.profil.ProfilScreen
import com.mojtijek.doktor.ui.terapije.TerapijeScreen

private sealed class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object MojDan : Tab("moj_dan", "Moj dan", Icons.Filled.Home)
    object Terapije : Tab("terapije", "Terapije", Icons.Filled.Medication)
    object Kalendar : Tab("kalendar", "Pregledi", Icons.Filled.CalendarMonth)
    object Pracenje : Tab("pracenje", "Praćenje", Icons.Filled.MonitorHeart)
    object Profil : Tab("profil", "Obitelj", Icons.Filled.Person)
}

private val tabs = listOf(Tab.MojDan, Tab.Terapije, Tab.Kalendar, Tab.Pracenje, Tab.Profil)

@Composable
fun MojTijekApp(vm: MojTijekViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.MojDan.route,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Tab.MojDan.route) { HomeScreen(vm) }
            composable(Tab.Terapije.route) { TerapijeScreen(vm) }
            composable(Tab.Kalendar.route) { KalendarScreen(vm) }
            composable(Tab.Pracenje.route) { PracenjeScreen(vm) }
            composable(Tab.Profil.route) { ProfilScreen(vm) }
        }
    }
}
