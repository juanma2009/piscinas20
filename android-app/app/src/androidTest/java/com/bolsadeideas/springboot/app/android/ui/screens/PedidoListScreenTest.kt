package com.bolsadeideas.springboot.app.android.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import com.bolsadeideas.springboot.app.android.data.model.PedidoDto
import org.junit.Rule
import org.junit.Test

class PedidoListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pedidoListScreen_displaysTitleAndSearch() {
        composeTestRule.setContent {
            PedidoListScreen(
                onPedidoClick = {},
                onEditPedido = {},
                onDeletePedido = {},
                onNewPedido = {}
            )
        }

        // Check title
        composeTestRule.onNodeWithText("Listado de Trabajos").assertIsDisplayed()
        
        // Check search field
        composeTestRule.onNodeWithText("Buscar...").assertIsDisplayed()
    }

    @Test
    fun pedidoListScreen_displaysNewPedidoButton() {
        composeTestRule.setContent {
            PedidoListScreen(
                onPedidoClick = {},
                onEditPedido = {},
                onDeletePedido = {},
                onNewPedido = {}
            )
        }

        // Check Floating Action Button
        composeTestRule.onNodeWithContentDescription("Nuevo Pedido").assertIsDisplayed()
    }

    @Test
    fun pedidoItem_displaysActions() {
        val mockPedido = PedidoDto(
            npedido = 123456,
            ref = "REF123",
            cliente = "PIÑA-COMAS",
            tipoPedido = "Pedido",
            estado = "Finalizado",
            fechaFinalizado = "17-01-2026",
            fechaEntrega = "01-01-2026",
            metal = "Oro Amarillo",
            pieza = "Anillo",
            tipo = "Anillo",
            cobrado = 600.0,
            imagenes = emptyList()
        )

        composeTestRule.setContent {
            PedidoItem(
                pedido = mockPedido,
                onViewClick = {},
                onEditClick = {},
                onDeleteClick = {}
            )
        }

        // Check details
        composeTestRule.onNodeWithText("Pedido: 123456").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cliente: PIÑA-COMAS").assertIsDisplayed()

        // Check Action Buttons
        composeTestRule.onNodeWithContentDescription("Ver").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Editar").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Eliminar").assertIsDisplayed()
    }
}
