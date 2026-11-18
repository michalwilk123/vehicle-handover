-- Existing vehicles get the default: the customer registers the vehicle.
ALTER TABLE vehicle ADD COLUMN dealership_registers BOOLEAN DEFAULT FALSE NOT NULL;
