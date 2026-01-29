package com.bolsadeideas.springboot.app.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bolsadeideas.springboot.app.android.data.api.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteDetailScreen(clienteId: Long, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var clienteDetail by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(clienteId) {
        scope.launch {
            try {
                val response = RetrofitClient.api.getClienteDetalle(clienteId)
                clienteDetail = response
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle Cliente") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            clienteDetail?.let { detail ->
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    item {
                        Text(text = "${detail["nombre"]} ${detail["apellido"] ?: ""}", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Email: ${detail["email"] ?: "N/A"}")
                        Text(text = "Teléfono: ${detail["telefono"] ?: "N/A"}")
                        Text(text = "Dirección: ${detail["direccion"] ?: "N/A"}")
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(text = "Pedidos", style = MaterialTheme.typography.titleLarge)
                    }
                    
                    val pedidos = detail["pedidos"] as? List<Map<String, Any>> ?: emptyList()
                    items(pedidos) { pedido ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "Pedido #${(pedido["npedido"] as? Double)?.toLong() ?: pedido["npedido"]}", style = MaterialTheme.typography.titleSmall)
                                Text(text = "Ref: ${pedido["ref"] ?: "N/A"}")
                                Text(text = "Estado: ${pedido["estado"] ?: "N/A"}")
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = "Facturas", style = MaterialTheme.typography.titleLarge)
                    }

                    val facturas = detail["facturas"] as? List<Map<String, Any>> ?: emptyList()
                    items(facturas) { factura ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "Factura #${(factura["id"] as? Double)?.toLong() ?: factura["id"]}", style = MaterialTheme.typography.titleSmall)
                                Text(text = "Total: ${factura["total"]} €")
                            }
                        }
                    }
                }
            }
        }
    }
}
