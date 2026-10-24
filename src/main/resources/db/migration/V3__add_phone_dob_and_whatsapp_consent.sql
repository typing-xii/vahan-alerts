ALTER TABLE app_user ALTER COLUMN email DROP NOT NULL;

ALTER TABLE app_user ADD COLUMN phone_number VARCHAR(16);
ALTER TABLE app_user ADD COLUMN date_of_birth DATE;
ALTER TABLE app_user ADD COLUMN whatsapp_reminder_consent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_user ADD COLUMN whatsapp_consent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_user ADD COLUMN whatsapp_consent_withdrawn_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_user ADD COLUMN whatsapp_consent_version VARCHAR(32);

ALTER TABLE app_user
    ADD CONSTRAINT uk_app_user_phone_number UNIQUE (phone_number);
