package com.mojtijek.doktor.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mojtijek.doktor.R
import com.mojtijek.doktor.ui.home.HomeScreen
import com.mojtijek.doktor.ui.nalazi.NalaziScreen
import com.mojtijek.doktor.ui.pracenje.PracenjeScreen
import com.mojtijek.doktor.ui.profil.ProfilScreen
import com.mojtijek.doktor.ui.terapije.TerapijeScreen

// Isti tabovi, redoslijed i nazivi kao iOS RootView.
private class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab("danas", R.string.tab_danas, Icons.Filled.Home),
    Tab("terapije", R.string.tab_terapije, Icons.Filled.Medication),
    Tab("nalazi", R.string.tab_nalazi, Icons.Filled.Description),
    Tab("pracenje", R.string.tab_pracenje, Icons.Filled.MonitorHeart),
    Tab("profil", R.string.tab_profil, Icons.Filled.Person),
)

@Composable
fun MojTijekApp(vm: MojTijekViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val odrediste = entry?.destination
    fun naTab(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = odrediste?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = { naTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label), maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "danas", modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("danas") {
                HomeScreen(
                    vm = vm,
                    onDnevnik = { nav.navigate("dnevnik") },
                    onKartoteka = { nav.navigate("kartoteka") },
                    onKalendar = { nav.navigate("kalendar") },
                    onPracenje = { naTab("pracenje") },
                )
            }
            composable("terapije") { TerapijeScreen(vm) }
            composable("nalazi") { NalaziScreen(vm) }
            composable("pracenje") { PracenjeScreen(vm) }
            composable("profil") { ProfilScreen(vm) }
            composable("dnevnik") { com.mojtijek.doktor.ui.dnevnik.DnevnikScreen(vm = vm, onNavigateBack = { nav.popBackStack() }) }
            composable("kartoteka") { NalaziScreen(vm, onNavigateBack = { nav.popBackStack() }) }
            composable("kalendar") { com.mojtijek.doktor.ui.kalendar.KalendarScreen(vm = vm, onNavigateBack = { nav.popBackStack() }) }
        }
    }
}
