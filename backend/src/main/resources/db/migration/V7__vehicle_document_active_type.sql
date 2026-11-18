-- One active (not rejected) document per type and vehicle. active_type is the type while the document is
-- active and NULL after rejection; NULL values do not take part in the unique constraint.
ALTER TABLE vehicle_document ADD COLUMN active_type VARCHAR(30);

-- If existing data already has two active documents of one type, the constraint below fails the migration
-- on purpose: such rows need a decision by staff, not a silent change.
UPDATE vehicle_document SET active_type = type WHERE status <> 'REJECTED';

ALTER TABLE vehicle_document
    ADD CONSTRAINT uq_vehicle_document_active_type UNIQUE (vehicle_id, active_type);
