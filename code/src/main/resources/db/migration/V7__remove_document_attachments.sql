ALTER TABLE vehicle_document DROP CONSTRAINT chk_vehicle_document_attachment;
ALTER TABLE vehicle_document DROP COLUMN attachment_key;
ALTER TABLE vehicle_document DROP COLUMN attachment_media_type;
ALTER TABLE vehicle_document DROP COLUMN attachment_size;

CREATE INDEX idx_vehicle_document_expiry
    ON vehicle_document (expiry_date);
