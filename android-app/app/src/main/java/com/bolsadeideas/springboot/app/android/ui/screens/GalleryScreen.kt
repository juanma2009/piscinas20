package com.bolsadeideas.springboot.app.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bolsadeideas.springboot.app.android.data.api.RetrofitClient
import com.bolsadeideas.springboot.app.android.data.model.PedidoDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onPedidoClick: (PedidoDto) -> Unit) {
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
            TopAppBar(title = { Text("Galería de Trabajos") })
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val allImages = pedidos.flatMap { p -> 
                p.imagenes?.map { url -> Pair(url, p) } ?: emptyList() 
            }

            if (allImages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No hay fotos disponibles")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = padding,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allImages) { (url, pedido) ->
                        Card(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onPedidoClick(pedido) }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Trabajo #${pedido.npedido}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
