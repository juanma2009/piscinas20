package com.bolsadeideas.springboot.app.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bolsadeideas.springboot.app.android.data.api.RetrofitClient
import com.bolsadeideas.springboot.app.android.data.model.PedidoDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoListScreen(onPedidoClick: (PedidoDto) -> Unit) {
    val scope = rememberCoroutineScope()
    var pedidos by remember { mutableStateOf<List<PedidoDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = RetrofitClient.api.listarPedidos()
                pedidos = response.data
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Pedidos") })
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(pedidos) { pedido ->
                    PedidoItem(pedido, onClick = { onPedidoClick(pedido) })
                }
            }
        }
    }
}

@Composable
fun PedidoItem(pedido: PedidoDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Pedido: ${pedido.npedido}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Cliente: ${pedido.cliente}")
            Text(text = "Referencia: ${pedido.ref ?: "N/A"}")
            Text(text = "Estado: ${pedido.estado ?: "N/A"}")
            Text(text = "Pieza: ${pedido.pieza ?: "N/A"} - ${pedido.tipo ?: "N/A"}")
        }
    }
}
