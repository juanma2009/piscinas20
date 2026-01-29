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
import com.bolsadeideas.springboot.app.android.data.api.RetrofitClient
import com.bolsadeideas.springboot.app.android.data.model.ClienteDto
import com.bolsadeideas.springboot.app.android.data.model.PedidoDto
import com.bolsadeideas.springboot.app.android.ui.screens.*
import com.bolsadeideas.springboot.app.android.ui.theme.JoyeriaTheme
import kotlinx.coroutines.launch

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
                    var editingPedido by remember { mutableStateOf<PedidoDto?>(null) }
                    var selectedClienteId by remember { mutableStateOf<Long?>(null) }
                    var editingCliente by remember { mutableStateOf<ClienteDto?>(null) }

                    val scope = rememberCoroutineScope()
                    var refreshTrigger by remember { mutableIntStateOf(0) }

                    if (selectedPedido != null) {
                        PedidoDetailScreen(pedido = selectedPedido!!, onBack = { selectedPedido = null })
                    } else if (editingPedido != null) {
                        PedidoEditScreen(
                            pedido = editingPedido!!,
                            onBack = { editingPedido = null },
                            onSaveSuccess = { 
                                editingPedido = null
                                refreshTrigger++
                            }
                        )
                    } else if (selectedClienteId != null) {
                        ClienteDetailScreen(clienteId = selectedClienteId!!, onBack = { selectedClienteId = null })
                    } else if (editingCliente != null) {
                        ClienteEditScreen(
                            cliente = editingCliente!!,
                            onBack = { editingCliente = null },
                            onSaveSuccess = { 
                                editingCliente = null
                                refreshTrigger++
                            }
                        )
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
                                    key(refreshTrigger) {
                                        PedidoListScreen(
                                            onPedidoClick = { selectedPedido = it },
                                            onEditPedido = { editingPedido = it },
                                            onDeletePedido = { pedido ->
                                                scope.launch {
                                                    try {
                                                        val response = RetrofitClient.api.eliminarPedido(pedido.npedido)
                                                        if (response.isSuccessful) {
                                                            refreshTrigger++
                                                        }
                                                    } catch (e: Exception) {
                                                        // Handle error
                                                    }
                                                }
                                            },
                                            onNewPedido = {
                                                editingPedido = PedidoDto(
                                                    npedido = 0L,
                                                    ref = "",
                                                    cliente = "",
                                                    tipoPedido = null,
                                                    estado = "Nuevo",
                                                    fechaFinalizado = null,
                                                    fechaEntrega = null,
                                                    metal = null,
                                                    pieza = null,
                                                    tipo = null,
                                                    cobrado = 0.0,
                                                    imagenes = null
                                                )
                                            }
                                        )
                                    }
                                }
                                composable("Clientes") { 
                                    key(refreshTrigger) {
                                        ClienteListScreen(
                                            onClienteClick = { selectedClienteId = it },
                                            onEditCliente = { editingCliente = it },
                                            onDeleteCliente = { cliente ->
                                                scope.launch {
                                                    try {
                                                        val response = RetrofitClient.api.eliminarCliente(cliente.id)
                                                        if (response.isSuccessful) {
                                                            refreshTrigger++
                                                        }
                                                    } catch (e: Exception) {
                                                        // Handle error
                                                    }
                                                }
                                            },
                                            onNewCliente = {
                                                editingCliente = ClienteDto(
                                                    id = 0L,
                                                    nombre = "",
                                                    apellido = "",
                                                    email = "",
                                                    telefono = ""
                                                )
                                            }
                                        )
                                    }
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
