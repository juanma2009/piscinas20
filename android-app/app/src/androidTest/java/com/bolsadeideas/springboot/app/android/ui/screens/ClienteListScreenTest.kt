package com.bolsadeideas.springboot.app.android.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import com.bolsadeideas.springboot.app.android.data.model.ClienteDto
import org.junit.Rule
import org.junit.Test

class ClienteListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clienteListScreen_displaysTitleAndSearch() {
        composeTestRule.setContent {
            ClienteListScreen(
                onClienteClick = {},
                onEditCliente = {},
                onDeleteCliente = {},
                onNewCliente = {}
            )
        }

        // Check title
        composeTestRule.onNodeWithText("Listado de Clientes").assertIsDisplayed()
        
        // Check search field
        composeTestRule.onNodeWithText("Buscar cliente...").assertIsDisplayed()
    }

    @Test
    fun clienteListScreen_displaysNewClienteButton() {
        composeTestRule.setContent {
            ClienteListScreen(
                onClienteClick = {},
                onEditCliente = {},
                onDeleteCliente = {},
                onNewCliente = {}
            )
        }

        // Check Floating Action Button
        composeTestRule.onNodeWithContentDescription("Nuevo Cliente").assertIsDisplayed()
    }

    @Test
    fun clienteItem_displaysActions() {
        val mockCliente = ClienteDto(
            id = 1,
            nombre = "Juan",
            apellido = "Pérez",
            email = "juan@example.com",
            telefono = "123456789"
        )

        composeTestRule.setContent {
            ClienteItem(
                cliente = mockCliente,
                onViewClick = {},
                onEditClick = {},
                onDeleteClick = {}
            )
        }

        // Check details
        composeTestRule.onNodeWithText("Juan Pérez").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email: juan@example.com").assertIsDisplayed()

        // Check Action Buttons
        composeTestRule.onNodeWithContentDescription("Ver").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Editar").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Eliminar").assertIsDisplayed()
    }
}
