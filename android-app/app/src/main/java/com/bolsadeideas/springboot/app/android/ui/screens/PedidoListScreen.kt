package com.bolsadeideas.springboot.app.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bolsadeideas.springboot.app.android.data.api.RetrofitClient
import com.bolsadeideas.springboot.app.android.data.model.PedidoDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoListScreen(
    onPedidoClick: (PedidoDto) -> Unit,
    onEditPedido: (PedidoDto) -> Unit,
    onDeletePedido: (PedidoDto) -> Unit,
    onNewPedido: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var allPedidos by remember { mutableStateOf<List<PedidoDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isFirstLoad by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(0) }
    var isLastPage by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var pedidoToDelete by remember { mutableStateOf<PedidoDto?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val loadNextPage = {
        if (!isLoading && !isLastPage) {
            isLoading = true
            scope.launch {
                try {
                    val response = RetrofitClient.api.listarPedidos(page = currentPage)
                    if (currentPage == 0) {
                        allPedidos = response.data
                    } else {
                        allPedidos = allPedidos + response.data
                    }
                    // Asumimos que si vienen menos de los esperados o la respuesta indica fin, paramos.
                    // En DataTables, recordsTotal nos dice el total.
                    isLastPage = allPedidos.size >= response.recordsTotal || response.data.isEmpty()
                    currentPage++
                    errorMessage = null
                } catch (e: Exception) {
                    errorMessage = "Error al cargar pedidos"
                } finally {
                    isLoading = false
                    isFirstLoad = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadNextPage()
    }

    val filteredPedidos = remember(searchQuery, allPedidos) {
        if (searchQuery.isBlank()) {
            allPedidos
        } else {
            allPedidos.filter { 
                it.cliente.contains(searchQuery, ignoreCase = true) || 
                (it.ref?.contains(searchQuery, ignoreCase = true) ?: false) ||
                it.npedido.toString().contains(searchQuery)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Listado de Trabajos") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewPedido) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Pedido")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Buscar...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            if (errorMessage != null && allPedidos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { loadNextPage() }) {
                            Text("Reintentar")
                        }
                    }
                }
            } else if (isFirstLoad) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    itemsIndexed(filteredPedidos) { index, pedido ->
                        // Detectar fin de lista para paginación (solo si no hay búsqueda activa)
                        if (searchQuery.isBlank() && index >= allPedidos.size - 1 && !isLoading && !isLastPage) {
                            LaunchedEffect(allPedidos.size) {
                                loadNextPage()
                            }
                        }

                        PedidoItem(
                            pedido = pedido,
                            onViewClick = { onPedidoClick(pedido) },
                            onEditClick = { onEditPedido(pedido) },
                            onDeleteClick = { pedidoToDelete = pedido }
                        )
                    }

                    if (isLoading && !isFirstLoad) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (pedidoToDelete != null) {
        AlertDialog(
            onDismissRequest = { pedidoToDelete = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar el pedido #${pedidoToDelete?.npedido}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pedidoToDelete?.let { onDeletePedido(it) }
                        pedidoToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pedidoToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun PedidoItem(
    pedido: PedidoDto,
    onViewClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Pedido: ${pedido.npedido}", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Cliente: ${pedido.cliente}")
                }
                Row {
                    IconButton(
                        onClick = onViewClick,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF2196F3), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Ver")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onEditClick,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFFC107), contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFF44336), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Referencia: ${pedido.ref ?: "N/A"}")
            Text(text = "Estado: ${pedido.estado ?: "N/A"}")
            Text(text = "Pieza: ${pedido.pieza ?: "N/A"} - ${pedido.tipo ?: "N/A"}")
        }
    }
}
