package com.bolsadeideas.springboot.app.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bolsadeideas.springboot.app.android.data.model.PedidoDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoDetailScreen(pedido: PedidoDto, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle Pedido #${pedido.npedido}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
            Text(text = "Cliente: ${pedido.cliente}", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            
            DetailItem("Referencia", pedido.ref ?: "N/A")
            DetailItem("Estado", pedido.estado ?: "N/A")
            DetailItem("Pieza", pedido.pieza ?: "N/A")
            DetailItem("Tipo", pedido.tipo ?: "N/A")
            DetailItem("Metal", pedido.metal ?: "N/A")
            DetailItem("Precio", "${pedido.cobrado ?: 0.0} €")

            if (!pedido.imagenes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Galería de Imágenes", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(250.dp)
                ) {
                    items(pedido.imagenes) { url ->
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.width(200.dp).fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "No hay imágenes disponibles", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = "$label: ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
