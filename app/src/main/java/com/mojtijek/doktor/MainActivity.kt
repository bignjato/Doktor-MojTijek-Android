package com.mojtijek.doktor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mojtijek.doktor.ui.MojTijekApp
import com.mojtijek.doktor.ui.MojTijekViewModel
import com.mojtijek.doktor.ui.theme.DoktorMojTijekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repo = (application as DoktorApp).repository

        setContent {
            DoktorMojTijekTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val vm: MojTijekViewModel = viewModel(factory = MojTijekViewModel.Factory(repo))
                    MojTijekApp(vm)
                }
            }
        }
    }
}
