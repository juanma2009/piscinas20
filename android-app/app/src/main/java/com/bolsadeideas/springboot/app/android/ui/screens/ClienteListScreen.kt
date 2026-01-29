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
import com.bolsadeideas.springboot.app.android.data.model.ClienteDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteListScreen(
    onClienteClick: (Long) -> Unit,
    onEditCliente: (ClienteDto) -> Unit,
    onDeleteCliente: (ClienteDto) -> Unit,
    onNewCliente: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var allClientes by remember { mutableStateOf<List<ClienteDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isFirstLoad by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(0) }
    var isLastPage by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var clienteToDelete by remember { mutableStateOf<ClienteDto?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val loadNextPage = {
        if (!isLoading && !isLastPage) {
            isLoading = true
            scope.launch {
                try {
                    val response = RetrofitClient.api.listarClientes(page = currentPage)
                    if (currentPage == 0) {
                        allClientes = response.data
                    } else {
                        allClientes = allClientes + response.data
                    }
                    isLastPage = allClientes.size >= response.recordsTotal || response.data.isEmpty()
                    currentPage++
                    errorMessage = null
                } catch (e: Exception) {
                    errorMessage = "Error al cargar clientes"
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

    val filteredClientes = remember(searchQuery, allClientes) {
        if (searchQuery.isBlank()) {
            allClientes
        } else {
            allClientes.filter { 
                it.nombre.contains(searchQuery, ignoreCase = true) || 
                (it.apellido?.contains(searchQuery, ignoreCase = true) ?: false) ||
                (it.email?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Listado de Clientes") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewCliente) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Cliente")
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
                placeholder = { Text("Buscar cliente...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            if (errorMessage != null && allClientes.isEmpty()) {
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
                    itemsIndexed(filteredClientes) { index, cliente ->
                        // Detectar fin de lista para paginación (solo si no hay búsqueda activa)
                        if (searchQuery.isBlank() && index >= allClientes.size - 1 && !isLoading && !isLastPage) {
                            LaunchedEffect(allClientes.size) {
                                loadNextPage()
                            }
                        }

                        ClienteItem(
                            cliente = cliente,
                            onViewClick = { onClienteClick(cliente.id) },
                            onEditClick = { onEditCliente(cliente) },
                            onDeleteClick = { clienteToDelete = cliente }
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

    if (clienteToDelete != null) {
        AlertDialog(
            onDismissRequest = { clienteToDelete = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar al cliente ${clienteToDelete?.nombre} ${clienteToDelete?.apellido ?: ""}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clienteToDelete?.let { onDeleteCliente(it) }
                        clienteToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { clienteToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ClienteItem(
    cliente: ClienteDto,
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
                    Text(text = "${cliente.nombre} ${cliente.apellido ?: ""}", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Email: ${cliente.email ?: "N/A"}")
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
            Text(text = "Teléfono: ${cliente.telefono ?: "N/A"}")
        }
    }
}
