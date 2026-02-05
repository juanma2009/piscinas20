-- Script para eliminar duplicados y agregar constraint de unicidad
-- Ejecutar manualmente en PostgreSQL si es necesario

-- Ver duplicados actuales
SELECT pedido_id, template_id, COUNT(*) as count, array_agg(id ORDER BY id) as instance_ids
FROM checklist_instance
GROUP BY pedido_id, template_id
HAVING COUNT(*) > 1;

-- 1. Eliminar items de instancias duplicadas (mantener el de menor ID)
DELETE FROM checklist_item_instance 
WHERE instance_id IN (
    SELECT id FROM checklist_instance ci
    WHERE id NOT IN (
        SELECT MIN(id) 
        FROM checklist_instance 
        GROUP BY pedido_id, template_id
    )
);

-- 2. Eliminar instancias duplicadas (mantener el de menor ID)
DELETE FROM checklist_instance 
WHERE id NOT IN (
    SELECT MIN(id) 
    FROM checklist_instance 
    GROUP BY pedido_id, template_id
);

-- 3. Agregar constraint de unicidad
ALTER TABLE checklist_instance 
DROP CONSTRAINT IF EXISTS uk_checklist_pedido_template;

ALTER TABLE checklist_instance 
ADD CONSTRAINT uk_checklist_pedido_template 
UNIQUE (pedido_id, template_id);

-- 4. Verificar que no quedan duplicados
SELECT pedido_id, template_id, COUNT(*) as count
FROM checklist_instance
GROUP BY pedido_id, template_id
HAVING COUNT(*) > 1;

-- Debería retornar 0 filas
