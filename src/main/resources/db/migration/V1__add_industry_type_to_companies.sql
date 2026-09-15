-- Migration: add industry_type to companies
-- Adds industry_type column with default OTHER
ALTER TABLE companies ADD COLUMN industry_type VARCHAR(50) DEFAULT 'OTHER' NOT NULL;
-- If using a DB that doesn't support DEFAULT ... NOT NULL together, adjust accordingly.
