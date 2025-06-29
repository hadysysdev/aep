-- V1__Initial_schema.sql

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Farms Table
CREATE TABLE farms (
    farm_identifier UUID PRIMARY KEY,
    farm_name VARCHAR(255),
    owner_reference_id UUID NOT NULL,
    country_code CHAR(2) NOT NULL,
    region VARCHAR(255),
    general_location_coordinates GEOMETRY(Point, 4326),
    notes TEXT,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Plots Table
CREATE TABLE plots (
    plot_identifier UUID PRIMARY KEY,
    farm_identifier UUID NOT NULL REFERENCES farms(farm_identifier) ON DELETE CASCADE,
    plot_name VARCHAR(255),
    cultivator_reference_id UUID,
    plot_geometry GEOMETRY(Polygon, 4326) NOT NULL,
    land_tenure_type VARCHAR(50),
    calculated_area_hectares NUMERIC(10,4) GENERATED ALWAYS AS (ST_Area(plot_geometry::geography) / 10000.0) STORED,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Spatial index for plot_geometry
CREATE INDEX idx_plots_plot_geometry ON plots USING GIST (plot_geometry);

-- Land Tenures Table
CREATE TABLE land_tenures (
    land_tenure_identifier UUID PRIMARY KEY,
    plot_identifier UUID NOT NULL REFERENCES plots(plot_identifier) ON DELETE CASCADE UNIQUE,
    tenure_type VARCHAR(50) NOT NULL,
    lease_start_date DATE,
    lease_end_date DATE,
    owner_details TEXT,
    agreement_document_reference VARCHAR(255),
    tenant_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Points Of Interest Table
CREATE TABLE points_of_interest (
    poi_identifier UUID PRIMARY KEY,
    parent_entity_identifier UUID NOT NULL,
    parent_entity_type VARCHAR(10) NOT NULL,
    poi_name VARCHAR(255),
    poi_type VARCHAR(50) NOT NULL,
    coordinates GEOMETRY(Point, 4326) NOT NULL,
    notes TEXT,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Spatial index for POI coordinates
CREATE INDEX idx_poi_coordinates ON points_of_interest USING GIST (coordinates);
