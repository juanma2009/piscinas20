package com.bolsadeideas.springboot.app.service.workshop;

import com.bolsadeideas.springboot.app.config.TestConfig;
import com.bolsadeideas.springboot.app.models.dao.PedidoDao;
import com.bolsadeideas.springboot.app.models.dao.workshop.ChecklistInstanceDao;
import com.bolsadeideas.springboot.app.models.dao.workshop.ChecklistItemInstanceDao;
import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistInstance;
import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistItemInstance;
import com.bolsadeideas.springboot.app.models.service.IClienteService;
import com.bolsadeideas.springboot.app.models.service.workshop.WorkshopService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.cache.type=caffeine",
    "spring.cloudinary.cloud-name=test",
    "spring.cloudinary.api-key=test",
    "spring.cloudinary.api-secret=test",
    "cloudinary.upload-preset=test",
    "google.oauth.client-id=test",
    "google.oauth.client-secret=test"
})
@Import(TestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class WorkshopServiceIntegrationTest {

    @Autowired
    private WorkshopService workshopService;

    @Autowired
    private IClienteService clienteService;

    @Autowired
    private PedidoDao pedidoDao;

    @Autowired
    private ChecklistInstanceDao instanceDao;

    @Autowired
    private ChecklistItemInstanceDao itemInstanceDao;

    private static Long testPedidoId;
    private static Long testChecklistItemId;

    @BeforeAll
    static void setupClass() {
        System.out.println("🧪 Iniciando tests de integración de WorkshopService");
    }

    @BeforeEach
    void setup() {
        if (testPedidoId == null) {
            Cliente cliente = clienteService.findAll().stream().findFirst().orElse(null);
            
            if (cliente == null) {
                cliente = new Cliente();
                cliente.setNombre("Cliente");
                cliente.setApellido("de Prueba Setup");
                cliente.setEmail("setup@test.com");
                cliente.setTelefono("123456789");
                cliente.setDireccion("Calle Setup 123");
                clienteService.save(cliente);
            }
            
            Pedido existingPedido = StreamSupport.stream(pedidoDao.findAll().spliterator(), false)
                .filter(p -> "Pedido de prueba para checklist".equals(p.getObservacion()))
                .findFirst()
                .orElse(null);
            
            if (existingPedido != null) {
                testPedidoId = existingPedido.getNpedido();
            } else {
                Pedido pedido = new Pedido();
                pedido.setCliente(cliente);
                pedido.setObservacion("Pedido de prueba para checklist");
                pedido.setEstado("Pendiente");
                pedido.setTipoPedido("Pedido");
                pedido.setFechaEntrega(new Date());
                
                Pedido saved = pedidoDao.save(pedido);
                testPedidoId = saved.getNpedido();
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("01 - Seed Templates: Verifica que se crean las plantillas correctamente")
    @Rollback(false)
    void testSeedTemplates() {
        System.out.println("\n📋 Test 1: Seed Templates");
        
        workshopService.seedTemplates();
        
        System.out.println("✅ Templates creadas correctamente");
    }

    @Test
    @Order(2)
    @DisplayName("02 - Crear pedido de prueba")
    @Rollback(false)
    void testCreateTestPedido() {
        System.out.println("\n🛒 Test 2: Crear pedido de prueba");
        
        Cliente cliente = clienteService.findAll().stream().findFirst().orElse(null);
        
        if (cliente == null) {
            System.out.println("  ⚠️ No hay clientes, creando cliente de prueba...");
            cliente = new Cliente();
            cliente.setNombre("Cliente");
            cliente.setApellido("de Prueba");
            cliente.setEmail("prueba@test.com");
            cliente.setTelefono("123456789");
            cliente.setDireccion("Calle Test 123");
            clienteService.save(cliente);
            System.out.println("  ✅ Cliente creado con ID: " + cliente.getId());
        }
        
        assertNotNull(cliente, "Debe existir al menos un cliente en la BD");
        
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setObservacion("Pedido de prueba para checklist");
        pedido.setEstado("Pendiente");
        pedido.setTipoPedido("Pedido");
        pedido.setFechaEntrega(new Date());
        
        Pedido saved = pedidoDao.save(pedido);
        testPedidoId = saved.getNpedido();
        
        assertNotNull(testPedidoId, "El pedido debe tener un ID después de guardarse");
        System.out.println("✅ Pedido creado con ID: " + testPedidoId);
    }

    @Test
    @Order(3)
    @DisplayName("03 - Instantiate Checklists: Crea checklists para el pedido")
    @Rollback(false)
    void testInstantiateChecklistsForPedido() {
        System.out.println("\n📝 Test 3: Instantiate Checklists");
        
        assertNotNull(testPedidoId, "Debe existir un pedido de prueba");
        
        Pedido pedido = pedidoDao.findById(testPedidoId).orElse(null);
        assertNotNull(pedido, "El pedido debe existir en la BD");
        
        workshopService.instantiateChecklistsForPedido(pedido);
        
        List<ChecklistInstance> instances = workshopService.getChecklistsByPedido(testPedidoId);
        
        assertNotNull(instances, "Las instancias de checklist no deben ser null");
        assertEquals(8, instances.size(), "Debe haber 8 checklists (A-H)");
        
        System.out.println("✅ Checklists creados: " + instances.size());
        
        for (ChecklistInstance instance : instances) {
            assertNotNull(instance.getTemplate(), "Cada instancia debe tener una plantilla");
            assertNotNull(instance.getTemplate().getCode(), "Cada plantilla debe tener un código");
            assertTrue(instance.getItems().size() > 0, "Cada checklist debe tener items");
            System.out.println("  - " + instance.getTemplate().getName() + " (" + instance.getItems().size() + " items)");
        }
    }

    @Test
    @Order(4)
    @DisplayName("04 - Get Checklists: Recupera los checklists del pedido")
    void testGetChecklistsByPedido() {
        System.out.println("\n🔍 Test 4: Get Checklists");
        
        assertNotNull(testPedidoId, "Debe existir un pedido de prueba");
        
        List<ChecklistInstance> instances = workshopService.getChecklistsByPedido(testPedidoId);
        
        assertNotNull(instances, "Las instancias no deben ser null");
        assertEquals(8, instances.size(), "Debe recuperar 8 checklists");
        
        String[] expectedCodes = {"A_RECEPCION", "B_TALLA_MEDIDAS", "C_METALES_CLIENTE", "D_PIEDRAS", 
                                  "E_DISENO_APROBACION", "F_PRODUCCION", "G_CONTROL_CALIDAD", "H_ENTREGA"};
        
        for (int i = 0; i < expectedCodes.length; i++) {
            assertEquals(expectedCodes[i], instances.get(i).getTemplate().getCode(), 
                "El código del checklist " + i + " debe ser " + expectedCodes[i]);
        }
        
        System.out.println("✅ Checklists recuperados correctamente");
    }

    @Test
    @Order(5)
    @DisplayName("05 - No Duplicate Checklists: Verifica que no se crean duplicados")
    @Rollback(false)
    void testNoDuplicateChecklists() {
        System.out.println("\n🚫 Test 5: No Duplicate Checklists");
        
        assertNotNull(testPedidoId, "Debe existir un pedido de prueba");
        
        Pedido pedido = pedidoDao.findById(testPedidoId).orElse(null);
        assertNotNull(pedido, "El pedido debe existir");
        
        List<ChecklistInstance> beforeCount = workshopService.getChecklistsByPedido(testPedidoId);
        int countBefore = beforeCount.size();
        
        workshopService.instantiateChecklistsForPedido(pedido);
        
        List<ChecklistInstance> afterCount = workshopService.getChecklistsByPedido(testPedidoId);
        int countAfter = afterCount.size();
        
        assertEquals(countBefore, countAfter, "No debe crear duplicados al llamar instantiate dos veces");
        System.out.println("✅ No se crearon duplicados (" + countAfter + " checklists)");
    }

    @Test
    @Order(6)
    @DisplayName("06 - Update Checklist Item: Actualiza un item del checklist")
    @Rollback(false)
    void testUpdateChecklistItem() {
        System.out.println("\n✏️ Test 6: Update Checklist Item");
        
        assertNotNull(testPedidoId, "Debe existir un pedido de prueba");
        
        List<ChecklistInstance> instances = workshopService.getChecklistsByPedido(testPedidoId);
        ChecklistInstance firstInstance = instances.get(0);
        ChecklistItemInstance firstItem = firstInstance.getItems().get(0);
        
        testChecklistItemId = firstItem.getId();
        
        assertNotNull(testChecklistItemId, "El item debe tener un ID");
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("valueText", "Juan Pérez - Cliente Test");
        updates.put("notes", "Nota de prueba para test de integración");
        
        ChecklistItemInstance updated = workshopService.updateChecklistItem(
            testChecklistItemId, updates, "testUser"
        );
        
        assertNotNull(updated, "El item actualizado no debe ser null");
        assertEquals("Juan Pérez - Cliente Test", updated.getValueText(), "El valueText debe actualizarse");
        assertEquals("Nota de prueba para test de integración", updated.getNotes(), "Las notas deben actualizarse");
        assertEquals("testUser", updated.getCompletedBy(), "El usuario debe registrarse");
        assertNotNull(updated.getCompletedAt(), "La fecha de completado debe registrarse");
        
        System.out.println("✅ Item actualizado correctamente");
        System.out.println("  - ValueText: " + updated.getValueText());
        System.out.println("  - Notes: " + updated.getNotes());
        System.out.println("  - CompletedBy: " + updated.getCompletedBy());
    }

    @Test
    @Order(7)
    @DisplayName("07 - Get Updated Item: Verifica que los datos persisten")
    void testGetUpdatedItem() {
        System.out.println("\n🔎 Test 7: Get Updated Item");
        
        assertNotNull(testChecklistItemId, "Debe existir un item de prueba");
        
        ChecklistItemInstance retrieved = itemInstanceDao.findById(testChecklistItemId).orElse(null);
        
        assertNotNull(retrieved, "El item debe existir en la BD");
        assertEquals("Juan Pérez - Cliente Test", retrieved.getValueText(), 
            "El valueText debe persistir en la BD");
        assertEquals("Nota de prueba para test de integración", retrieved.getNotes(), 
            "Las notas deben persistir en la BD");
        assertEquals("testUser", retrieved.getCompletedBy(), 
            "El usuario debe persistir en la BD");
        assertNotNull(retrieved.getCompletedAt(), 
            "La fecha de completado debe persistir en la BD");
        
        System.out.println("✅ Datos persistidos correctamente en la BD");
    }

    @Test
    @Order(8)
    @DisplayName("08 - Update Multiple Items in Batch: Actualiza varios items a la vez")
    @Rollback(false)
    void testUpdateChecklistItemsBatch() {
        System.out.println("\n📦 Test 8: Update Multiple Items in Batch");
        
        assertNotNull(testPedidoId, "Debe existir un pedido de prueba");
        
        List<ChecklistInstance> instances = workshopService.getChecklistsByPedido(testPedidoId);
        ChecklistInstance firstInstance = instances.get(0);
        
        List<ChecklistItemInstance> items = firstInstance.getItems();
        assertTrue(items.size() >= 3, "Debe haber al menos 3 items para el test batch");
        
        List<Map<String, Object>> batchUpdates = List.of(
            Map.of(
                "itemId", items.get(0).getId(),
                "updates", Map.of("valueText", "Valor Batch 1", "notes", "Nota Batch 1")
            ),
            Map.of(
                "itemId", items.get(1).getId(),
                "updates", Map.of("valueText", "Valor Batch 2", "notes", "Nota Batch 2")
            ),
            Map.of(
                "itemId", items.get(2).getId(),
                "updates", Map.of("valueText", "Valor Batch 3", "checked", true)
            )
        );
        
        List<ChecklistItemInstance> updatedItems = workshopService.updateChecklistItemsBatch(
            batchUpdates, "batchTestUser"
        );
        
        assertNotNull(updatedItems, "Los items actualizados no deben ser null");
        assertEquals(3, updatedItems.size(), "Deben actualizarse 3 items");
        
        assertEquals("Valor Batch 1", updatedItems.get(0).getValueText());
        assertEquals("Valor Batch 2", updatedItems.get(1).getValueText());
        assertEquals("Valor Batch 3", updatedItems.get(2).getValueText());
        assertTrue(updatedItems.get(2).getChecked());
        
        System.out.println("✅ Batch update completado correctamente");
        System.out.println("  - Items actualizados: " + updatedItems.size());
    }

    @Test
    @Order(9)
    @DisplayName("09 - Get Checklist by Code: Recupera un checklist específico por código")
    void testGetChecklistInstanceByPedidoAndCode() {
        System.out.println("\n🎯 Test 9: Get Checklist by Code");
        
        assertNotNull(testPedidoId, "Debe existir un pedido de prueba");
        
        ChecklistInstance instance = workshopService.getChecklistInstanceByPedidoAndCode(
            testPedidoId, "A_RECEPCION"
        );
        
        assertNotNull(instance, "Debe encontrar el checklist A_RECEPCION");
        assertEquals("A_RECEPCION", instance.getTemplate().getCode());
        assertTrue(instance.getItems().size() > 0, "El checklist debe tener items");
        
        System.out.println("✅ Checklist específico recuperado correctamente");
        System.out.println("  - Código: " + instance.getTemplate().getCode());
        System.out.println("  - Nombre: " + instance.getTemplate().getName());
        System.out.println("  - Items: " + instance.getItems().size());
    }

    @Test
    @Order(10)
    @DisplayName("10 - Remove Duplicate Checklists: Elimina duplicados si existen")
    void testRemoveDuplicateChecklists() {
        System.out.println("\n🧹 Test 10: Remove Duplicate Checklists");
        
        assertNotNull(testPedidoId, "Debe existir un pedido de prueba");
        
        List<ChecklistInstance> before = workshopService.getChecklistsByPedido(testPedidoId);
        int countBefore = before.size();
        
        workshopService.removeDuplicateChecklistsForPedido(testPedidoId);
        
        List<ChecklistInstance> after = workshopService.getChecklistsByPedido(testPedidoId);
        int countAfter = after.size();
        
        assertEquals(8, countAfter, "Debe haber exactamente 8 checklists después de limpiar");
        
        System.out.println("✅ Duplicados eliminados correctamente");
        System.out.println("  - Antes: " + countBefore + " checklists");
        System.out.println("  - Después: " + countAfter + " checklists");
    }

    @Test
    @Order(11)
    @DisplayName("11 - Verify Data Integrity: Verifica la integridad de todos los datos")
    void testDataIntegrity() {
        System.out.println("\n🔒 Test 11: Verify Data Integrity");
        
        assertNotNull(testPedidoId, "Debe existir un pedido de prueba");
        
        List<ChecklistInstance> instances = workshopService.getChecklistsByPedido(testPedidoId);
        
        int totalItems = 0;
        int itemsWithValue = 0;
        
        for (ChecklistInstance instance : instances) {
            assertNotNull(instance.getPedido(), "Cada instancia debe estar vinculada a un pedido");
            assertEquals(testPedidoId, instance.getPedido().getNpedido(), 
                "El pedido vinculado debe ser el correcto");
            
            for (ChecklistItemInstance item : instance.getItems()) {
                totalItems++;
                
                assertNotNull(item.getItemCode(), "Cada item debe tener un código");
                assertNotNull(item.getLabel(), "Cada item debe tener una etiqueta");
                assertNotNull(item.getInputType(), "Cada item debe tener un tipo de input");
                
                if (item.getValueText() != null && !item.getValueText().isEmpty()) {
                    itemsWithValue++;
                }
            }
        }
        
        assertTrue(totalItems > 50, "Debe haber más de 50 items en total");
        assertTrue(itemsWithValue > 0, "Debe haber al menos un item con valor");
        
        System.out.println("✅ Integridad de datos verificada");
        System.out.println("  - Total items: " + totalItems);
        System.out.println("  - Items con valor: " + itemsWithValue);
        System.out.println("  - Cobertura: " + String.format("%.2f%%", (itemsWithValue * 100.0 / totalItems)));
    }

    @AfterAll
    static void teardownClass() {
        System.out.println("\n✅ Tests de integración completados exitosamente");
        System.out.println("📊 Resumen:");
        System.out.println("  - Pedido de prueba ID: " + testPedidoId);
        System.out.println("  - Item de prueba ID: " + testChecklistItemId);
    }
}
