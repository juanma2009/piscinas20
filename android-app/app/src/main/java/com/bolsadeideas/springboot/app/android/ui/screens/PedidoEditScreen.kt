package com.bolsadeideas.springboot.app.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bolsadeideas.springboot.app.android.data.api.RetrofitClient
import com.bolsadeideas.springboot.app.android.data.model.PedidoDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoEditScreen(pedido: PedidoDto, onBack: () -> Unit, onSaveSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var cliente by remember { mutableStateOf(pedido.cliente) }
    var ref by remember { mutableStateOf(pedido.ref ?: "") }
    var estado by remember { mutableStateOf(pedido.estado ?: "") }
    var pieza by remember { mutableStateOf(pedido.pieza ?: "") }
    var tipo by remember { mutableStateOf(pedido.tipo ?: "") }
    var metal by remember { mutableStateOf(pedido.metal ?: "") }
    var cobrado by remember { mutableStateOf(pedido.cobrado?.toString() ?: "0.0") }
    
    var isSaving by remember { mutableStateOf(false) }

    val onSave = {
        if (cliente.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("El cliente es obligatorio") }
        } else {
            isSaving = true
            scope.launch {
                try {
                    val updatedPedido = pedido.copy(
                        cliente = cliente,
                        ref = ref,
                        estado = estado,
                        pieza = pieza,
                        tipo = tipo,
                        metal = metal,
                        cobrado = cobrado.toDoubleOrNull() ?: 0.0
                    )
                    val response = RetrofitClient.api.guardarPedido(updatedPedido)
                if (response.isSuccessful) {
                    onSaveSuccess()
                } else {
                    snackbarHostState.showSnackbar("Error al guardar: ${response.code()}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isSaving = false
            }
        }
    }
}

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (pedido.npedido == 0L) "Nuevo Pedido" else "Editar Pedido #${pedido.npedido}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onSave() },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Guardar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = cliente,
                onValueChange = { cliente = it },
                label = { Text("Cliente") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = ref,
                onValueChange = { ref = it },
                label = { Text("Referencia") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = estado,
                onValueChange = { estado = it },
                label = { Text("Estado") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pieza,
                onValueChange = { pieza = it },
                label = { Text("Pieza") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = tipo,
                onValueChange = { tipo = it },
                label = { Text("Tipo") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = metal,
                onValueChange = { metal = it },
                label = { Text("Metal") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = cobrado,
                onValueChange = { cobrado = it },
                label = { Text("Cobrado (€)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onSave() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(if (pedido.npedido == 0L) "Crear Pedido" else "Guardar Cambios")
            }
        }
    }
}
