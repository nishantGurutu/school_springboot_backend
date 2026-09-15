-- Migration: add active column to roles
-- Adds active column with default true
ALTER TABLE roles ADD COLUMN active BOOLEAN DEFAULT true NOT NULL;
