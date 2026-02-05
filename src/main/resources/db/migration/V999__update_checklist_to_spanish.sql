-- Actualizar opciones y valores del checklist a español (enums en español)

-- A. RECEPCIÓN: Preferencia contacto
UPDATE checklist_template_item SET options = 'WHATSAPP|TELEFONO|CORREO' WHERE item_code = 'A004_CONTACT_PREF';
UPDATE checklist_item_instance SET options = 'WHATSAPP|TELEFONO|CORREO' WHERE item_code = 'A004_CONTACT_PREF';
UPDATE checklist_item_instance SET value_text = 'TELEFONO' WHERE item_code = 'A004_CONTACT_PREF' AND value_text = 'PHONE';
UPDATE checklist_item_instance SET value_text = 'TELEFONO' WHERE item_code = 'A004_CONTACT_PREF' AND value_text = 'TELÉFONO';
UPDATE checklist_item_instance SET value_text = 'CORREO' WHERE item_code = 'A004_CONTACT_PREF' AND value_text = 'EMAIL';

-- A. RECEPCIÓN: Tipo de pedido
UPDATE checklist_template_item SET options = 'NUEVO|REPARACION|AJUSTE_TALLA|ENGASTE|GRABADO|LIMPIEZA|PULIDO|BANO|PERSONALIZADO' WHERE item_code = 'A005_ORDER_TYPE';
UPDATE checklist_item_instance SET options = 'NUEVO|REPARACION|AJUSTE_TALLA|ENGASTE|GRABADO|LIMPIEZA|PULIDO|BANO|PERSONALIZADO' WHERE item_code = 'A005_ORDER_TYPE';
UPDATE checklist_item_instance SET value_text = 'NUEVO' WHERE item_code = 'A005_ORDER_TYPE' AND value_text IN ('NEW', 'NUEVO');
UPDATE checklist_item_instance SET value_text = 'REPARACION' WHERE item_code = 'A005_ORDER_TYPE' AND value_text IN ('REPAIR', 'REPARACIÓN');
UPDATE checklist_item_instance SET value_text = 'AJUSTE_TALLA' WHERE item_code = 'A005_ORDER_TYPE' AND value_text IN ('RESIZE', 'AJUSTE TALLA');
UPDATE checklist_item_instance SET value_text = 'ENGASTE' WHERE item_code = 'A005_ORDER_TYPE' AND value_text = 'SETTING';
UPDATE checklist_item_instance SET value_text = 'GRABADO' WHERE item_code = 'A005_ORDER_TYPE' AND value_text IN ('ENGRAVING', 'GRABADO');
UPDATE checklist_item_instance SET value_text = 'LIMPIEZA' WHERE item_code = 'A005_ORDER_TYPE' AND value_text IN ('CLEANING', 'LIMPIEZA');
UPDATE checklist_item_instance SET value_text = 'PULIDO' WHERE item_code = 'A005_ORDER_TYPE' AND value_text IN ('POLISHING', 'PULIDO');
UPDATE checklist_item_instance SET value_text = 'BANO' WHERE item_code = 'A005_ORDER_TYPE' AND value_text IN ('PLATING', 'BAÑO');
UPDATE checklist_item_instance SET value_text = 'PERSONALIZADO' WHERE item_code = 'A005_ORDER_TYPE' AND value_text IN ('CUSTOM', 'PERSONALIZADO');

-- A. RECEPCIÓN: Prioridad
UPDATE checklist_template_item SET options = 'BAJA|NORMAL|ALTA|URGENTE' WHERE item_code = 'A007_PRIORITY';
UPDATE checklist_item_instance SET options = 'BAJA|NORMAL|ALTA|URGENTE' WHERE item_code = 'A007_PRIORITY';
UPDATE checklist_item_instance SET value_text = 'BAJA' WHERE item_code = 'A007_PRIORITY' AND value_text = 'LOW';
UPDATE checklist_item_instance SET value_text = 'ALTA' WHERE item_code = 'A007_PRIORITY' AND value_text = 'HIGH';
UPDATE checklist_item_instance SET value_text = 'URGENTE' WHERE item_code = 'A007_PRIORITY' AND value_text = 'URGENT';

-- A. RECEPCIÓN: Estado al recibir (MULTISELECT)
UPDATE checklist_template_item SET options = 'RAYONES|ABOLLADURAS|PIEDRAS_SUELTAS|PIEZAS_FALTANTES|DOBLADO|DEFORMADO' WHERE item_code = 'A010_INTAKE_CONDITION';
UPDATE checklist_item_instance SET options = 'RAYONES|ABOLLADURAS|PIEDRAS_SUELTAS|PIEZAS_FALTANTES|DOBLADO|DEFORMADO' WHERE item_code = 'A010_INTAKE_CONDITION';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'SCRATCHES', 'RAYONES') WHERE item_code = 'A010_INTAKE_CONDITION' AND value_text LIKE '%SCRATCHES%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'DENTS', 'ABOLLADURAS') WHERE item_code = 'A010_INTAKE_CONDITION' AND value_text LIKE '%DENTS%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'LOOSE_STONES', 'PIEDRAS_SUELTAS') WHERE item_code = 'A010_INTAKE_CONDITION' AND value_text LIKE '%LOOSE_STONES%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'LOOSE STONES', 'PIEDRAS_SUELTAS') WHERE item_code = 'A010_INTAKE_CONDITION' AND value_text LIKE '%LOOSE STONES%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'PIEDRAS SUELTAS', 'PIEDRAS_SUELTAS') WHERE item_code = 'A010_INTAKE_CONDITION' AND value_text LIKE '%PIEDRAS SUELTAS%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'MISSING_PARTS', 'PIEZAS_FALTANTES') WHERE item_code = 'A010_INTAKE_CONDITION' AND value_text LIKE '%MISSING_PARTS%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'MISSING PARTS', 'PIEZAS_FALTANTES') WHERE item_code = 'A010_INTAKE_CONDITION' AND value_text LIKE '%MISSING PARTS%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'PIEZAS FALTANTES', 'PIEZAS_FALTANTES') WHERE item_code = 'A010_INTAKE_CONDITION' AND value_text LIKE '%PIEZAS FALTANTES%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'BENT', 'DOBLADO') WHERE item_code = 'A010_INTAKE_CONDITION' AND value_text LIKE '%BENT%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'DEFORMED', 'DEFORMADO') WHERE item_code = 'A010_INTAKE_CONDITION' AND value_text LIKE '%DEFORMED%';

-- C. METALES: Acabado final
UPDATE checklist_template_item SET options = 'ESPEJO|SATINADO|MATE|MARTILLADO|CEPILLADO' WHERE item_code = 'C004_FINISH_TYPE';
UPDATE checklist_item_instance SET options = 'ESPEJO|SATINADO|MATE|MARTILLADO|CEPILLADO' WHERE item_code = 'C004_FINISH_TYPE';
UPDATE checklist_item_instance SET value_text = 'ESPEJO' WHERE item_code = 'C004_FINISH_TYPE' AND value_text = 'MIRROR';
UPDATE checklist_item_instance SET value_text = 'SATINADO' WHERE item_code = 'C004_FINISH_TYPE' AND value_text IN ('SATIN', 'SATINADO');
UPDATE checklist_item_instance SET value_text = 'MATE' WHERE item_code = 'C004_FINISH_TYPE' AND value_text = 'MATTE';
UPDATE checklist_item_instance SET value_text = 'MARTILLADO' WHERE item_code = 'C004_FINISH_TYPE' AND value_text IN ('HAMMERED', 'MARTILLADO');
UPDATE checklist_item_instance SET value_text = 'CEPILLADO' WHERE item_code = 'C004_FINISH_TYPE' AND value_text IN ('BRUSHED', 'CEPILLADO');

-- C. METALES: Tipo de baño
UPDATE checklist_template_item SET options = 'RODIO|ORO|RUTENIO|OTRO' WHERE item_code = 'C006_PLATING_TYPE';
UPDATE checklist_item_instance SET options = 'RODIO|ORO|RUTENIO|OTRO' WHERE item_code = 'C006_PLATING_TYPE';
UPDATE checklist_item_instance SET value_text = 'RODIO' WHERE item_code = 'C006_PLATING_TYPE' AND value_text IN ('RHODIUM', 'RODIO');
UPDATE checklist_item_instance SET value_text = 'ORO' WHERE item_code = 'C006_PLATING_TYPE' AND value_text = 'GOLD';
UPDATE checklist_item_instance SET value_text = 'RUTENIO' WHERE item_code = 'C006_PLATING_TYPE' AND value_text IN ('RUTHENIUM', 'RUTENIO');
UPDATE checklist_item_instance SET value_text = 'OTRO' WHERE item_code = 'C006_PLATING_TYPE' AND value_text = 'OTHER';

-- D. PIEDRAS: Familia
UPDATE checklist_template_item SET options = 'DIAMANTE_NATURAL|DIAMANTE_LABORATORIO|PRECIOSAS_4|GEMA_FINA|CUARZO_CALCEDONIA|FELDESPATO|OPALO|PERLA|ORGANICA|ORNAMENTAL|SINTETICA_SIMULANTE|OTRA' WHERE item_code = 'D002_STONE_FAMILY';
UPDATE checklist_item_instance SET options = 'DIAMANTE_NATURAL|DIAMANTE_LABORATORIO|PRECIOSAS_4|GEMA_FINA|CUARZO_CALCEDONIA|FELDESPATO|OPALO|PERLA|ORGANICA|ORNAMENTAL|SINTETICA_SIMULANTE|OTRA' WHERE item_code = 'D002_STONE_FAMILY';
UPDATE checklist_item_instance SET value_text = 'DIAMANTE_NATURAL' WHERE item_code = 'D002_STONE_FAMILY' AND value_text IN ('DIAMOND_NATURAL', 'DIAMANTE NATURAL');
UPDATE checklist_item_instance SET value_text = 'DIAMANTE_LABORATORIO' WHERE item_code = 'D002_STONE_FAMILY' AND value_text IN ('DIAMOND_LAB', 'DIAMANTE LABORATORIO');
UPDATE checklist_item_instance SET value_text = 'PRECIOSAS_4' WHERE item_code = 'D002_STONE_FAMILY' AND value_text IN ('PRECIOUS_4', 'PRECIOSAS 4');
UPDATE checklist_item_instance SET value_text = 'GEMA_FINA' WHERE item_code = 'D002_STONE_FAMILY' AND value_text IN ('FINE_GEM', 'GEMA FINA');
UPDATE checklist_item_instance SET value_text = 'CUARZO_CALCEDONIA' WHERE item_code = 'D002_STONE_FAMILY' AND value_text IN ('QUARTZ_CHALCEDONY', 'CUARZO/CALCEDONIA');
UPDATE checklist_item_instance SET value_text = 'FELDESPATO' WHERE item_code = 'D002_STONE_FAMILY' AND value_text IN ('FELDSPAR', 'FELDESPATO');
UPDATE checklist_item_instance SET value_text = 'OPALO' WHERE item_code = 'D002_STONE_FAMILY' AND value_text IN ('OPAL', 'ÓPALO');
UPDATE checklist_item_instance SET value_text = 'PERLA' WHERE item_code = 'D002_STONE_FAMILY' AND value_text IN ('PEARL', 'PERLA');
UPDATE checklist_item_instance SET value_text = 'ORGANICA' WHERE item_code = 'D002_STONE_FAMILY' AND value_text IN ('ORGANIC', 'ORGÁNICA');
UPDATE checklist_item_instance SET value_text = 'ORNAMENTAL' WHERE item_code = 'D002_STONE_FAMILY' AND value_text = 'ORNAMENTAL';
UPDATE checklist_item_instance SET value_text = 'SINTETICA_SIMULANTE' WHERE item_code = 'D002_STONE_FAMILY' AND value_text IN ('SYNTHETIC_SIMULANT', 'SINTÉTICA/SIMULANTE');
UPDATE checklist_item_instance SET value_text = 'OTRA' WHERE item_code = 'D002_STONE_FAMILY' AND value_text = 'OTHER';

-- D. PIEDRAS: Origen
UPDATE checklist_template_item SET options = 'CLIENTE|TALLER' WHERE item_code = 'D004_ORIGIN';
UPDATE checklist_item_instance SET options = 'CLIENTE|TALLER' WHERE item_code = 'D004_ORIGIN';
UPDATE checklist_item_instance SET value_text = 'CLIENTE' WHERE item_code = 'D004_ORIGIN' AND value_text = 'CUSTOMER';
UPDATE checklist_item_instance SET value_text = 'TALLER' WHERE item_code = 'D004_ORIGIN' AND value_text = 'WORKSHOP';

-- D. PIEDRAS: Forma
UPDATE checklist_template_item SET options = 'REDONDA|PRINCESA|OVAL|PERA|MARQUESA|CORTE_ESMERALDA|CORAZON|CABUJON|OTRA' WHERE item_code = 'D005_SHAPE';
UPDATE checklist_item_instance SET options = 'REDONDA|PRINCESA|OVAL|PERA|MARQUESA|CORTE_ESMERALDA|CORAZON|CABUJON|OTRA' WHERE item_code = 'D005_SHAPE';
UPDATE checklist_item_instance SET value_text = 'REDONDA' WHERE item_code = 'D005_SHAPE' AND value_text = 'ROUND';
UPDATE checklist_item_instance SET value_text = 'PRINCESA' WHERE item_code = 'D005_SHAPE' AND value_text IN ('PRINCESS', 'PRINCESA');
UPDATE checklist_item_instance SET value_text = 'OVAL' WHERE item_code = 'D005_SHAPE' AND value_text = 'OVAL';
UPDATE checklist_item_instance SET value_text = 'PERA' WHERE item_code = 'D005_SHAPE' AND value_text = 'PEAR';
UPDATE checklist_item_instance SET value_text = 'MARQUESA' WHERE item_code = 'D005_SHAPE' AND value_text IN ('MARQUISE', 'MARQUESA');
UPDATE checklist_item_instance SET value_text = 'CORTE_ESMERALDA' WHERE item_code = 'D005_SHAPE' AND value_text IN ('EMERALD_CUT', 'CORTE ESMERALDA');
UPDATE checklist_item_instance SET value_text = 'CORAZON' WHERE item_code = 'D005_SHAPE' AND value_text IN ('HEART', 'CORAZÓN');
UPDATE checklist_item_instance SET value_text = 'CABUJON' WHERE item_code = 'D005_SHAPE' AND value_text IN ('CABOCHON', 'CABUJÓN');
UPDATE checklist_item_instance SET value_text = 'OTRA' WHERE item_code = 'D005_SHAPE' AND value_text = 'OTHER';

-- D. PIEDRAS: Riesgos taller (MULTISELECT)
UPDATE checklist_template_item SET options = 'SIN_ULTRASONIDO|SIN_VAPOR|SENSIBLE_AL_CALOR|POROSA|RIESGO_DE_FRACTURA|RIESGO_RELLENO_FRACTURAS|RIESGO_CAMBIO_COLOR' WHERE item_code = 'D010_RISK_FLAGS';
UPDATE checklist_item_instance SET options = 'SIN_ULTRASONIDO|SIN_VAPOR|SENSIBLE_AL_CALOR|POROSA|RIESGO_DE_FRACTURA|RIESGO_RELLENO_FRACTURAS|RIESGO_CAMBIO_COLOR' WHERE item_code = 'D010_RISK_FLAGS';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'NO_ULTRASONIC', 'SIN_ULTRASONIDO') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%NO_ULTRASONIC%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'NO ULTRASONIC', 'SIN_ULTRASONIDO') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%NO ULTRASONIC%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'SIN ULTRASONIDO', 'SIN_ULTRASONIDO') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%SIN ULTRASONIDO%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'NO_STEAM', 'SIN_VAPOR') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%NO_STEAM%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'NO STEAM', 'SIN_VAPOR') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%NO STEAM%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'SIN VAPOR', 'SIN_VAPOR') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%SIN VAPOR%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'HEAT_SENSITIVE', 'SENSIBLE_AL_CALOR') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%HEAT_SENSITIVE%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'HEAT SENSITIVE', 'SENSIBLE_AL_CALOR') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%HEAT SENSITIVE%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'SENSIBLE AL CALOR', 'SENSIBLE_AL_CALOR') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%SENSIBLE AL CALOR%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'POROUS', 'POROSA') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%POROUS%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'CLEAVAGE_RISK', 'RIESGO_DE_FRACTURA') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%CLEAVAGE_RISK%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'CLEAVAGE RISK', 'RIESGO_DE_FRACTURA') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%CLEAVAGE RISK%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'RIESGO DE FRACTURA', 'RIESGO_DE_FRACTURA') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%RIESGO DE FRACTURA%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'FRACTURE_FILLED_RISK', 'RIESGO_RELLENO_FRACTURAS') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%FRACTURE_FILLED_RISK%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'FRACTURE FILLED RISK', 'RIESGO_RELLENO_FRACTURAS') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%FRACTURE FILLED RISK%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'RIESGO RELLENO FRACTURAS', 'RIESGO_RELLENO_FRACTURAS') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%RIESGO RELLENO FRACTURAS%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'COLOR_CHANGE_RISK', 'RIESGO_CAMBIO_COLOR') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%COLOR_CHANGE_RISK%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'COLOR CHANGE RISK', 'RIESGO_CAMBIO_COLOR') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%COLOR CHANGE RISK%';
UPDATE checklist_item_instance SET value_text = REPLACE(value_text, 'RIESGO CAMBIO COLOR', 'RIESGO_CAMBIO_COLOR') WHERE item_code = 'D010_RISK_FLAGS' AND value_text LIKE '%RIESGO CAMBIO COLOR%';

-- E. DISEÑO: Tipo de engaste
UPDATE checklist_template_item SET options = 'GARRAS|BISEL|CARRIL|PAVE|INVISIBLE|PEGADO|OTRO' WHERE item_code = 'E003_SETTING_TYPE';
UPDATE checklist_item_instance SET options = 'GARRAS|BISEL|CARRIL|PAVE|INVISIBLE|PEGADO|OTRO' WHERE item_code = 'E003_SETTING_TYPE';
UPDATE checklist_item_instance SET value_text = 'GARRAS' WHERE item_code = 'E003_SETTING_TYPE' AND value_text = 'PRONGS';
UPDATE checklist_item_instance SET value_text = 'BISEL' WHERE item_code = 'E003_SETTING_TYPE' AND value_text = 'BEZEL';
UPDATE checklist_item_instance SET value_text = 'CARRIL' WHERE item_code = 'E003_SETTING_TYPE' AND value_text = 'CHANNEL';
UPDATE checklist_item_instance SET value_text = 'PAVE' WHERE item_code = 'E003_SETTING_TYPE' AND value_text IN ('PAVE', 'PAVÉ');
UPDATE checklist_item_instance SET value_text = 'INVISIBLE' WHERE item_code = 'E003_SETTING_TYPE' AND value_text = 'INVISIBLE';
UPDATE checklist_item_instance SET value_text = 'PEGADO' WHERE item_code = 'E003_SETTING_TYPE' AND value_text IN ('GLUED', 'PEGADO');
UPDATE checklist_item_instance SET value_text = 'OTRO' WHERE item_code = 'E003_SETTING_TYPE' AND value_text = 'OTHER';

-- F. PRODUCCIÓN: Actualizar condición
UPDATE checklist_template_item SET condition_expr = 'A005_ORDER_TYPE==GRABADO' WHERE item_code = 'F006_ENGRAVING_DONE';
UPDATE checklist_item_instance SET condition_expr = 'A005_ORDER_TYPE==GRABADO' WHERE item_code = 'F006_ENGRAVING_DONE';
