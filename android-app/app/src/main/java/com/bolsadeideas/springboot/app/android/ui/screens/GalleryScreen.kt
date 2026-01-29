package com.bolsadeideas.springboot.app.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bolsadeideas.springboot.app.android.data.api.RetrofitClient
import com.bolsadeideas.springboot.app.android.data.model.PedidoDto
import com.bolsadeideas.springboot.app.android.data.model.PedidoFotoDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onPedidoClick: (PedidoDto) -> Unit) {
    val scope = rememberCoroutineScope()
    var fotos by remember { mutableStateOf<List<PedidoFotoDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isFirstLoad by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(0) }
    var isLastPage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val loadNextPage = {
        if (!isLoading && !isLastPage) {
            isLoading = true
            scope.launch {
                try {
                    val response = RetrofitClient.api.buscarFotos(page = currentPage, size = 20)
                    if (currentPage == 0) {
                        fotos = response.content
                    } else {
                        fotos = fotos + response.content
                    }
                    isLastPage = response.content.isEmpty() || currentPage >= response.totalPages - 1
                    currentPage++
                    errorMessage = null
                } catch (e: Exception) {
                    errorMessage = "Error al cargar imágenes"
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Galería de Trabajos") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (errorMessage != null && fotos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { loadNextPage() }) {
                            Text("Reintentar")
                        }
                    }
                }
            } else if (isFirstLoad) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (fotos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No hay fotos disponibles")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(fotos) { index, foto ->
                        // Detectamos cuando llegamos al final para cargar más
                        if (index >= fotos.size - 1 && !isLoading && !isLastPage) {
                            LaunchedEffect(fotos.size) {
                                loadNextPage()
                            }
                        }

                        Card(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { 
                                    onPedidoClick(PedidoDto(
                                        npedido = foto.npedido,
                                        ref = foto.ref,
                                        cliente = foto.clienteNombre ?: "N/A",
                                        tipoPedido = foto.tipoPedido,
                                        estado = foto.estado,
                                        fechaFinalizado = foto.dfecha,
                                        fechaEntrega = foto.fechaEntrega,
                                        metal = null,
                                        pieza = foto.pieza,
                                        tipo = foto.tipo,
                                        cobrado = null,
                                        imagenes = listOf(foto.imagenUrl)
                                    ))
                                }
                        ) {
                            AsyncImage(
                                model = foto.imagenUrl,
                                contentDescription = "Trabajo #${foto.npedido}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    if (isLoading && !isFirstLoad) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
