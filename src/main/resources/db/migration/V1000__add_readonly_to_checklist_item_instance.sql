-- Add readOnly column to checklist_item_instance table
ALTER TABLE checklist_item_instance ADD COLUMN IF NOT EXISTS read_only BOOLEAN DEFAULT FALSE;
