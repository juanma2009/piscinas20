package com.bolsadeideas.springboot.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bolsadeideas.springboot.app.android.data.model.PedidoDto
import com.bolsadeideas.springboot.app.android.ui.screens.*
import com.bolsadeideas.springboot.app.android.ui.theme.JoyeriaTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JoyeriaTheme {
                var isLoggedIn by remember { mutableStateOf(false) }

                if (!isLoggedIn) {
                    LoginScreen(onLoginSuccess = { isLoggedIn = true })
                } else {
                    val navController = rememberNavController()
                    var selectedItem by remember { mutableIntStateOf(0) }
                    val items = listOf("Pedidos", "Clientes", "Galería")
                    val icons = listOf(Icons.Filled.List, Icons.Filled.Person, Icons.Filled.Search)

                    var selectedPedido by remember { mutableStateOf<PedidoDto?>(null) }
                    var selectedClienteId by remember { mutableStateOf<Long?>(null) }

                    if (selectedPedido != null) {
                        PedidoDetailScreen(pedido = selectedPedido!!, onBack = { selectedPedido = null })
                    } else if (selectedClienteId != null) {
                        ClienteDetailScreen(clienteId = selectedClienteId!!, onBack = { selectedClienteId = null })
                    } else {
                        Scaffold(
                            bottomBar = {
                                NavigationBar {
                                    items.forEachIndexed { index, item ->
                                        NavigationBarItem(
                                            icon = { Icon(icons[index], contentDescription = item) },
                                            label = { Text(item) },
                                            selected = selectedItem == index,
                                            onClick = {
                                                selectedItem = index
                                                navController.navigate(item)
                                            }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = "Pedidos",
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                composable("Pedidos") { 
                                    PedidoListScreen(onPedidoClick = { selectedPedido = it }) 
                                }
                                composable("Clientes") { 
                                    ClienteListScreen(onClienteClick = { selectedClienteId = it }) 
                                }
                                composable("Galería") { 
                                    GalleryScreen(onPedidoClick = { selectedPedido = it }) 
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
