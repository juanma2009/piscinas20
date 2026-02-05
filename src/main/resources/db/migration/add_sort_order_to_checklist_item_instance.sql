ALTER TABLE checklist_item_instance ADD COLUMN IF NOT EXISTS sort_order INTEGER;

UPDATE checklist_item_instance cii
SET sort_order = cti.sort_order
FROM checklist_template_item cti
WHERE cii.template_item_id = cti.id
AND cii.sort_order IS NULL;
