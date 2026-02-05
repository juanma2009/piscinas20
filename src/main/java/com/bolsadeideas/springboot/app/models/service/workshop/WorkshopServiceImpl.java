package com.bolsadeideas.springboot.app.models.service.workshop;

import com.bolsadeideas.springboot.app.models.dao.workshop.*;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.entity.workshop.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class WorkshopServiceImpl implements WorkshopService {

    @Autowired
    private ChecklistTemplateDao templateDao;

    @Autowired
    private ChecklistTemplateSectionDao sectionDao;

    @Autowired
    private ChecklistTemplateItemDao itemDao;

    @Autowired
    private ChecklistInstanceDao instanceDao;

    @Autowired
    private ChecklistItemInstanceDao itemInstanceDao;

    @Override
    @Transactional
    public void seedTemplates() {
        String[][] data = {
            {"A_RECEPCION", "A001_CUSTOMER_NAME", "Nombre cliente", "TEXT", "true", "", "", "10"},
            {"A_RECEPCION", "A002_PHONE", "Teléfono", "TEXT", "true", "", "", "20"},
            {"A_RECEPCION", "A003_EMAIL", "Email", "TEXT", "false", "", "", "30"},
            {"A_RECEPCION", "A004_CONTACT_PREF", "Preferencia contacto", "SELECT", "true", "WHATSAPP|TELEFONO|CORREO", "", "40"},
            {"A_RECEPCION", "A005_ORDER_TYPE", "Tipo de pedido", "SELECT", "true", "NUEVO|REPARACION|AJUSTE_TALLA|ENGASTE|GRABADO|LIMPIEZA|PULIDO|BANO|PERSONALIZADO", "", "50"},
            {"A_RECEPCION", "A006_DUE_DATE", "Fecha objetivo (entrega)", "DATE", "true", "", "", "60"},
            {"A_RECEPCION", "A007_PRIORITY", "Prioridad", "SELECT", "true", "BAJA|NORMAL|ALTA|URGENTE", "", "70"},
            {"A_RECEPCION", "A008_HAS_CUSTOMER_PIECE", "¿Trae pieza el cliente?", "CHECKBOX", "true", "", "", "80"},
            {"A_RECEPCION", "A009_INTAKE_DESCRIPTION", "Descripción rápida de la pieza/encargo", "TEXTAREA", "true", "", "", "90"},
            {"A_RECEPCION", "A010_INTAKE_CONDITION", "Estado al recibir", "MULTISELECT", "false", "RAYONES|ABOLLADURAS|PIEDRAS_SUELTAS|PIEZAS_FALTANTES|DOBLADO|DEFORMADO", "A008_HAS_CUSTOMER_PIECE==true", "100"},
            {"A_RECEPCION", "A011_PHOTOS_BEFORE", "Fotos iniciales subidas (mín. 2)", "CHECKBOX", "true", "", "A008_HAS_CUSTOMER_PIECE==true", "110"},
            {"A_RECEPCION", "A012_WORK_REQUEST", "Qué quiere el cliente", "TEXTAREA", "true", "", "", "120"},
            {"A_RECEPCION", "A013_BUDGET_RANGE", "Presupuesto orientativo", "TEXT", "false", "", "", "130"},
            {"A_RECEPCION", "A014_DEPOSIT", "Anticipo (importe)", "NUMBER", "false", "", "", "140"},
            {"A_RECEPCION", "A015_RISK_ACCEPTANCE", "Aceptación: micro-rayas/desgaste preexistente", "CHECKBOX", "true", "", "A008_HAS_CUSTOMER_PIECE==true", "150"},
            {"B_TALLA_MEDIDAS", "B001_RING_SIZE", "Talla anillo", "TEXT", "false", "", "", "10"},
            {"B_TALLA_MEDIDAS", "B002_CHAIN_LENGTH_CM", "Largo cadena (cm)", "NUMBER", "false", "", "", "20"},
            {"B_TALLA_MEDIDAS", "B003_BRACELET_LENGTH_CM", "Largo pulsera (cm)", "NUMBER", "false", "", "", "30"},
            {"B_TALLA_MEDIDAS", "B004_OTHER_MEASURES", "Otras medidas (mm)", "TEXTAREA", "false", "", "", "40"},
            {"C_METALES_CLIENTE", "C001_REUSE_CUSTOMER_METAL", "¿Se reutiliza metal del cliente?", "CHECKBOX", "true", "", "", "10"},
            {"C_METALES_CLIENTE", "C002_RECEIVED_WEIGHT_GR", "Peso recibido (g)", "NUMBER", "false", "", "C001_REUSE_CUSTOMER_METAL==true", "20"},
            {"C_METALES_CLIENTE", "C003_METAL_STATE", "Estado del metal", "TEXTAREA", "false", "", "C001_REUSE_CUSTOMER_METAL==true", "30"},
            {"C_METALES_CLIENTE", "C004_FINISH_TYPE", "Acabado final", "SELECT", "true", "ESPEJO|SATINADO|MATE|MARTILLADO|CEPILLADO", "", "40"},
            {"C_METALES_CLIENTE", "C005_HAS_PLATING", "¿Baño/chapado?", "CHECKBOX", "true", "", "", "50"},
            {"C_METALES_CLIENTE", "C006_PLATING_TYPE", "Tipo de baño", "SELECT", "false", "RODIO|ORO|RUTENIO|OTRO", "C005_HAS_PLATING==true", "60"},
            {"C_METALES_CLIENTE", "C007_PLATING_MICRONS", "Micras", "NUMBER", "false", "", "C005_HAS_PLATING==true", "70"},
            {"C_METALES_CLIENTE", "C008_PLATING_NOTES", "Notas de baño", "TEXTAREA", "false", "", "C005_HAS_PLATING==true", "80"},
            {"D_PIEDRAS", "D001_HAS_STONES", "¿Incluye piedras?", "CHECKBOX", "true", "", "", "10"},
            {"D_PIEDRAS", "D002_STONE_FAMILY", "Familia", "SELECT", "true", "DIAMANTE_NATURAL|DIAMANTE_LABORATORIO|PRECIOSAS_4|GEMA_FINA|CUARZO_CALCEDONIA|FELDESPATO|OPALO|PERLA|ORGANICA|ORNAMENTAL|SINTETICA_SIMULANTE|OTRA", "D001_HAS_STONES==true", "20"},
            {"D_PIEDRAS", "D003_STONE_TYPE", "Nombre/tipo de piedra", "TEXT", "true", "", "D001_HAS_STONES==true", "30"},
            {"D_PIEDRAS", "D004_ORIGIN", "Origen", "SELECT", "true", "CLIENTE|TALLER", "D001_HAS_STONES==true", "40"},
            {"D_PIEDRAS", "D005_SHAPE", "Forma", "SELECT", "false", "REDONDA|PRINCESA|OVAL|PERA|MARQUESA|CORTE_ESMERALDA|CORAZON|CABUJON|OTRA", "D001_HAS_STONES==true", "50"},
            {"D_PIEDRAS", "D006_SIZE_MM", "Medida (mm)", "TEXT", "false", "", "D001_HAS_STONES==true", "60"},
            {"D_PIEDRAS", "D007_CARATS", "Quilates (ct)", "NUMBER", "false", "", "D001_HAS_STONES==true", "70"},
            {"D_PIEDRAS", "D008_QUANTITY", "Cantidad (nº)", "NUMBER", "true", "", "D001_HAS_STONES==true", "80"},
            {"D_PIEDRAS", "D009_TREATMENTS", "Tratamientos conocidos", "TEXTAREA", "false", "", "D001_HAS_STONES==true", "90"},
            {"D_PIEDRAS", "D010_RISK_FLAGS", "Riesgos taller", "MULTISELECT", "false", "SIN_ULTRASONIDO|SIN_VAPOR|SENSIBLE_AL_CALOR|POROSA|RIESGO_DE_FRACTURA|RIESGO_RELLENO_FRACTURAS|RIESGO_CAMBIO_COLOR", "D001_HAS_STONES==true", "100"},
            {"D_PIEDRAS", "D011_HAS_CERTIFICATE", "¿Tiene certificado?", "CHECKBOX", "false", "", "D001_HAS_STONES==true", "110"},
            {"D_PIEDRAS", "D012_CERTIFICATE_REF", "Referencia/adjunto certificado", "TEXT", "false", "", "D011_HAS_CERTIFICATE==true", "120"},
            {"D_PIEDRAS", "D013_STONE_PHOTOS", "Fotos/capturas de la(s) piedra(s)", "CHECKBOX", "false", "", "D001_HAS_STONES==true", "130"},
            {"D_PIEDRAS", "D014_CLIENT_INFORMED", "Confirmación: cliente informado de riesgos", "CHECKBOX", "true", "", "D001_HAS_STONES==true", "140"},
            {"E_DISENO_APROBACION", "E001_REFERENCE_UPLOADED", "Boceto/render/referencia subida", "CHECKBOX", "true", "", "", "10"},
            {"E_DISENO_APROBACION", "E002_KEY_MEASURES_CONFIRMED", "Medidas clave confirmadas", "CHECKBOX", "true", "", "", "20"},
            {"E_DISENO_APROBACION", "E003_SETTING_TYPE", "Tipo de engaste definido", "SELECT", "false", "GARRAS|BISEL|CARRIL|PAVE|INVISIBLE|PEGADO|OTRO", "D001_HAS_STONES==true", "30"},
            {"E_DISENO_APROBACION", "E004_CLIENT_APPROVED", "Aprobación del cliente", "CHECKBOX", "true", "", "", "40"},
            {"E_DISENO_APROBACION", "E005_FINAL_BUDGET_ACCEPTED", "Presupuesto final aceptado", "CHECKBOX", "true", "", "", "50"},
            {"E_DISENO_APROBACION", "E006_APPROVAL_DATE", "Fecha de aprobación", "DATE", "false", "", "E004_CLIENT_APPROVED==true", "60"},
            {"F_PRODUCCION", "F001_MATERIALS_PREPARED", "Material preparado", "CHECKBOX", "true", "", "", "10"},
            {"F_PRODUCCION", "F002_STRUCTURE_DONE", "Fabricación/estructura completada", "CHECKBOX", "true", "", "", "20"},
            {"F_PRODUCCION", "F003_SIZE_ADJUSTED", "Ajuste talla/medidas realizado", "CHECKBOX", "false", "", "", "30"},
            {"F_PRODUCCION", "F004_CASTING_DONE", "Fundición/colada", "CHECKBOX", "false", "", "", "40"},
            {"F_PRODUCCION", "F005_SETTING_DONE", "Engaste realizado", "CHECKBOX", "false", "", "D001_HAS_STONES==true", "50"},
            {"F_PRODUCCION", "F006_ENGRAVING_DONE", "Grabado realizado", "CHECKBOX", "false", "", "A005_ORDER_TYPE==GRABADO", "60"},
            {"F_PRODUCCION", "F007_FINISHING_DONE", "Acabados completados", "CHECKBOX", "true", "", "", "70"},
            {"F_PRODUCCION", "F008_PLATING_DONE", "Baño/chapado aplicado", "CHECKBOX", "false", "", "C005_HAS_PLATING==true", "80"},
            {"F_PRODUCCION", "F009_SAFE_CLEANING_CONFIRMED", "Limpieza final segura", "CHECKBOX", "false", "", "D001_HAS_STONES==true", "90"},
            {"G_CONTROL_CALIDAD", "G001_FINAL_MEASURES_RECORDED", "Medidas finales registradas", "CHECKBOX", "true", "", "", "10"},
            {"G_CONTROL_CALIDAD", "G002_FINAL_WEIGHT_GR", "Peso final (g)", "NUMBER", "false", "", "", "20"},
            {"G_CONTROL_CALIDAD", "G003_RING_SIZE_VERIFIED", "Talla final verificada", "CHECKBOX", "false", "", "", "30"},
            {"G_CONTROL_CALIDAD", "G004_STONES_SECURE", "Piedras seguras", "CHECKBOX", "false", "", "D001_HAS_STONES==true", "40"},
            {"G_CONTROL_CALIDAD", "G005_FINISH_UNIFORM", "Acabado uniforme", "CHECKBOX", "true", "", "", "50"},
            {"G_CONTROL_CALIDAD", "G006_NO_SHARP_EDGES", "Sin aristas cortantes", "CHECKBOX", "true", "", "", "60"},
            {"G_CONTROL_CALIDAD", "G007_HALLMARKS_OK", "Contrastes/marcajes correctos", "CHECKBOX", "false", "", "", "70"},
            {"G_CONTROL_CALIDAD", "G008_PHOTOS_AFTER", "Fotos finales subidas (mín. 2)", "CHECKBOX", "true", "", "", "80"},
            {"G_CONTROL_CALIDAD", "G009_PACKAGING", "Embalaje preparado", "CHECKBOX", "true", "", "", "90"},
            {"H_ENTREGA", "H001_FINAL_PAYMENT", "Pago final realizado", "CHECKBOX", "true", "", "", "10"},
            {"H_ENTREGA", "H002_INVOICE", "Factura/recibo emitido", "CHECKBOX", "true", "", "", "20"},
            {"H_ENTREGA", "H003_WARRANTY", "Garantía entregada", "CHECKBOX", "false", "", "", "30"},
            {"H_ENTREGA", "H004_CARE_INSTRUCTIONS", "Instrucciones de cuidado entregadas", "CHECKBOX", "true", "", "", "40"},
            {"H_ENTREGA", "H005_DELIVERY_RECORD", "Registro de entrega", "CHECKBOX", "true", "", "", "50"}
        };

        Map<String, ChecklistTemplate> templates = new HashMap<>();
        for (String[] row : data) {
            String tCode = row[0];
            ChecklistTemplate template = templates.computeIfAbsent(tCode, code -> {
                return templateDao.findByCode(code).orElseGet(() -> {
                    ChecklistTemplate t = new ChecklistTemplate();
                    t.setCode(code);
                    // Set Spanish names for checklist templates
                    String spanishName = switch (code) {
                        case "A_RECEPCION" -> "A. Recepción";
                        case "B_TALLA_MEDIDAS" -> "B. Talla y Medidas";
                        case "C_METALES_CLIENTE" -> "C. Metales del Cliente";
                        case "D_PIEDRAS" -> "D. Piedras";
                        case "E_DISENO_APROBACION" -> "E. Diseño y Aprobación";
                        case "F_PRODUCCION" -> "F. Producción";
                        case "G_CONTROL_CALIDAD" -> "G. Control de Calidad";
                        case "H_ENTREGA" -> "H. Entrega";
                        default -> code.replace("_", " ");
                    };
                    t.setName(spanishName);
                    t.setVersion("1.0");
                    return templateDao.save(t);
                });
            });

            // Update existing template names to Spanish if needed
            String expectedSpanishName = switch (tCode) {
                case "A_RECEPCION" -> "A. Recepción";
                case "B_TALLA_MEDIDAS" -> "B. Talla y Medidas";
                case "C_METALES_CLIENTE" -> "C. Metales del Cliente";
                case "D_PIEDRAS" -> "D. Piedras";
                case "E_DISENO_APROBACION" -> "E. Diseño y Aprobación";
                case "F_PRODUCCION" -> "F. Producción";
                case "G_CONTROL_CALIDAD" -> "G. Control de Calidad";
                case "H_ENTREGA" -> "H. Entrega";
                default -> tCode.replace("_", " ");
            };

            if (!expectedSpanishName.equals(template.getName())) {
                template.setName(expectedSpanishName);
                templateDao.save(template);
            }

            if (template.getSections().isEmpty()) {
                ChecklistTemplateSection section = new ChecklistTemplateSection();
                section.setTemplate(template);
                section.setCode(tCode);
                section.setTitle(tCode.replace("_", " "));
                section.setSortOrder(10);
                sectionDao.save(section);
                template.getSections().add(section);
            }

            ChecklistTemplateSection section = template.getSections().get(0);
            String iCode = row[1];
            boolean exists = section.getItems().stream().anyMatch(i -> i.getItemCode().equals(iCode));
            if (!exists) {
                ChecklistTemplateItem item = new ChecklistTemplateItem();
                item.setSection(section);
                item.setItemCode(iCode);
                item.setLabel(row[2]);
                item.setInputType(InputType.valueOf(row[3]));
                item.setRequired(Boolean.parseBoolean(row[4]));
                item.setOptions(row[5]);
                item.setConditionExpr(row[6]);
                item.setSortOrder(Integer.parseInt(row[7]));
                itemDao.save(item);
                section.getItems().add(item);
            }
        }
    }

    @Override
    @Transactional
    public void instantiateChecklistsForPedido(Pedido pedido) {
        List<String> codes = Arrays.asList("A_RECEPCION", "B_TALLA_MEDIDAS", "C_METALES_CLIENTE", "D_PIEDRAS", "E_DISENO_APROBACION", "F_PRODUCCION", "G_CONTROL_CALIDAD", "H_ENTREGA");

        for (String code : codes) {
            ChecklistTemplate template = templateDao.findByCode(code).orElse(null);
            if (template != null) {
                // Check if instance already exists for this pedido and template
                ChecklistInstance existingInstance = instanceDao.findByPedidoAndTemplate(pedido.getNpedido(), template.getId()).orElse(null);
                if (existingInstance == null) {
                    ChecklistInstance instance = new ChecklistInstance();
                    instance.setPedido(pedido);
                    instance.setTemplate(template);
                    instanceDao.save(instance);

                    for (ChecklistTemplateSection section : template.getSections()) {
                        for (ChecklistTemplateItem tItem : section.getItems()) {
                            ChecklistItemInstance iItem = new ChecklistItemInstance();
                            iItem.setInstance(instance);
                            iItem.setTemplateItem(tItem);
                            iItem.setItemCode(tItem.getItemCode());
                            iItem.setLabel(tItem.getLabel());
                            iItem.setInputType(tItem.getInputType());
                            iItem.setRequired(tItem.isRequired());
                            iItem.setOptions(tItem.getOptions());
                            iItem.setConditionExpr(tItem.getConditionExpr());
                            iItem.setSortOrder(tItem.getSortOrder());
                            
                            // Auto-populate customer fields from Pedido and mark as read-only
                            if (pedido.getCliente() != null && "A_RECEPCION".equals(code)) {
                                switch (tItem.getItemCode()) {
                                    case "A001_CUSTOMER_NAME":
                                        String fullName = (pedido.getCliente().getNombre() != null ? pedido.getCliente().getNombre() : "") + 
                                                         (pedido.getCliente().getApellido() != null ? " " + pedido.getCliente().getApellido() : "");
                                        iItem.setValueText(fullName.trim());
                                        iItem.setReadOnly(true);
                                        break;
                                    case "A002_PHONE":
                                        iItem.setValueText(pedido.getCliente().getTelefono());
                                        iItem.setReadOnly(true);
                                        break;
                                    case "A003_EMAIL":
                                        iItem.setValueText(pedido.getCliente().getEmail());
                                        iItem.setReadOnly(true);
                                        break;
                                }
                            }
                            
                            itemInstanceDao.save(iItem);
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistInstance> getChecklistsByPedido(Long npedido) {
        List<ChecklistInstance> instances = instanceDao.findByPedidoNpedido(npedido);
        
        Map<String, ChecklistInstance> uniqueInstances = new LinkedHashMap<>();
        for (ChecklistInstance instance : instances) {
            String code = instance.getTemplate().getCode();
            if (!uniqueInstances.containsKey(code)) {
                uniqueInstances.put(code, instance);
            }
        }
        
        List<ChecklistInstance> result = new ArrayList<>(uniqueInstances.values());
        result.sort(Comparator.comparing(i -> i.getTemplate().getCode()));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ChecklistInstance getChecklistInstanceByPedidoAndCode(Long npedido, String code) {
        List<ChecklistInstance> instances = instanceDao.findByPedidoNpedido(npedido);
        return instances.stream()
                .filter(i -> i.getTemplate().getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public ChecklistItemInstance updateChecklistItem(Long itemInstanceId, Map<String, Object> updates, String username) {
        log.info("Updating checklist item {}: {}", itemInstanceId, updates);
        ChecklistItemInstance item = itemInstanceDao.findById(itemInstanceId).orElseThrow(() -> new RuntimeException("Item instance not found"));
        
        log.info("Item found - Label: {}, Current checked: {}, Current valueText: {}", 
            item.getLabel(), item.getChecked(), item.getValueText());
        
        // Prevent updates to read-only fields
        if (Boolean.TRUE.equals(item.getReadOnly())) {
            throw new RuntimeException("Cannot update read-only field: " + item.getLabel());
        }
        
        if (updates.containsKey("checked")) {
            item.setChecked((Boolean) updates.get("checked"));
        }
        if (updates.containsKey("valueText")) {
            item.setValueText((String) updates.get("valueText"));
        }
        if (updates.containsKey("notes")) {
            item.setNotes((String) updates.get("notes"));
        }
        
        item.setCompletedAt(new Date());
        item.setCompletedBy(username);
        
        ChecklistItemInstance saved = itemInstanceDao.save(item);
        log.info("Item saved successfully - ID: {}, checked: {}, valueText: {}", 
            saved.getId(), saved.getChecked(), saved.getValueText());
        
        return saved;
    }

    @Override
    @Transactional
    public List<ChecklistItemInstance> updateChecklistItemsBatch(List<Map<String, Object>> batchUpdates, String username) {
        log.info("Batch updating {} checklist items", batchUpdates.size());
        List<ChecklistItemInstance> updatedItems = new ArrayList<>();
        
        for (Map<String, Object> updateData : batchUpdates) {
            Long itemId = ((Number) updateData.get("itemId")).longValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> updates = (Map<String, Object>) updateData.get("updates");
            
            try {
                ChecklistItemInstance updated = updateChecklistItem(itemId, updates, username);
                updatedItems.add(updated);
            } catch (Exception e) {
                log.error("Error updating item {}: {}", itemId, e.getMessage());
            }
        }
        
        log.info("Batch update completed: {}/{} items updated successfully", 
            updatedItems.size(), batchUpdates.size());
        
        return updatedItems;
    }

    @Override
    @Transactional
    public void removeDuplicateChecklistsForPedido(Long npedido) {
        List<ChecklistInstance> instances = instanceDao.findByPedidoNpedido(npedido);
        
        if (instances.isEmpty()) {
            return;
        }
        
        Map<Long, List<ChecklistInstance>> grouped = new HashMap<>();
        
        for (ChecklistInstance instance : instances) {
            Long templateId = instance.getTemplate().getId();
            grouped.computeIfAbsent(templateId, k -> new ArrayList<>()).add(instance);
        }
        
        for (Map.Entry<Long, List<ChecklistInstance>> entry : grouped.entrySet()) {
            List<ChecklistInstance> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                duplicates.sort(Comparator.comparing(ChecklistInstance::getId));
                for (int i = 1; i < duplicates.size(); i++) {
                    instanceDao.delete(duplicates.get(i));
                }
            }
        }
    }
}
