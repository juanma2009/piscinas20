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
import com.bolsadeideas.springboot.app.android.data.model.ClienteDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteListScreen(onClienteClick: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    var clientes by remember { mutableStateOf<List<ClienteDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = RetrofitClient.api.listarClientes()
                clientes = response.data
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Clientes") })
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(clientes) { cliente ->
                    ClienteItem(cliente, onClick = { onClienteClick(cliente.id) })
                }
            }
        }
    }
}

@Composable
fun ClienteItem(cliente: ClienteDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "${cliente.nombre} ${cliente.apellido ?: ""}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Email: ${cliente.email ?: "N/A"}")
            Text(text = "Teléfono: ${cliente.telefono ?: "N/A"}")
        }
    }
}
