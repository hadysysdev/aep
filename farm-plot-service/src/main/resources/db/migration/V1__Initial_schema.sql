-- Enable PostGIS extension if not already enabled
-- (The postgis/postgis Docker image usually does this for the initial DB,
-- but it's good practice to ensure it's available)
CREATE EXTENSION IF NOT EXISTS postgis;

-- Farms Table
CREATE TABLE farms (
    farm_identifier UUID PRIMARY KEY,
    farm_name VARCHAR(255),
    owner_reference_id UUID NOT NULL,
    country_code CHAR(2) NOT NULL,
    region VARCHAR(255),
    general_location_coordinates GEOMETRY(Point, 4326), -- WGS84 SRID
    notes TEXT,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Index on tenant_id for farms
CREATE INDEX idx_farms_tenant_id ON farms(tenant_id);
CREATE INDEX idx_farms_owner_reference_id ON farms(owner_reference_id);


-- Plots Table
CREATE TABLE plots (
    plot_identifier UUID PRIMARY KEY,
    farm_identifier UUID NOT NULL REFERENCES farms(farm_identifier) ON DELETE CASCADE,
    plot_name VARCHAR(255),
    cultivator_reference_id UUID,
    plot_geometry GEOMETRY(Polygon, 4326) NOT NULL, -- Or GEOMETRY(MultiPolygon, 4326)
    land_tenure_type VARCHAR(50), -- Stores LandTenureType enum as string
    -- Generated column for area in hectares using PostGIS geography type for accuracy
    -- ST_Area(geography) returns area in square meters. Divide by 10000 for hectares.
    -- calculated_area_hectares DOUBLE PRECISION GENERATED ALWAYS AS (ST_Area(plot_geometry::geography) / 10000.0) STORED,
    calculated_area_hectares NUMERIC(10,4) GENERATED ALWAYS AS (ST_Area(plot_geometry::geography) / 10000.0) STORED,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Spatial index for plot_geometry
CREATE INDEX idx_plots_plot_geometry ON plots USING GIST (plot_geometry);
-- Other useful indexes for plots
CREATE INDEX idx_plots_farm_identifier ON plots(farm_identifier);
CREATE INDEX idx_plots_tenant_id ON plots(tenant_id);
CREATE INDEX idx_plots_cultivator_reference_id ON plots(cultivator_reference_id);


-- Land Tenures Table
CREATE TABLE land_tenures (
    land_tenure_identifier UUID PRIMARY KEY,
    plot_identifier UUID NOT NULL REFERENCES plots(plot_identifier) ON DELETE CASCADE UNIQUE, -- Enforces OneToOne
    tenure_type VARCHAR(50) NOT NULL, -- LandTenureType enum
    lease_start_date DATE,
    lease_end_date DATE,
    owner_details TEXT,
    agreement_document_reference VARCHAR(255),
    tenant_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Index for land_tenures
CREATE INDEX idx_landtenures_plot_identifier ON land_tenures(plot_identifier);
CREATE INDEX idx_landtenures_tenant_id ON land_tenures(tenant_id);


-- Points Of Interest Table
CREATE TABLE points_of_interest (
    poi_identifier UUID PRIMARY KEY,
    parent_entity_identifier UUID NOT NULL,
    parent_entity_type VARCHAR(10) NOT NULL, -- "FARM" or "PLOT" (ParentEntityType enum)
    poi_name VARCHAR(255),
    poi_type VARCHAR(50) NOT NULL, -- POIType enum
    coordinates GEOMETRY(Point, 4326) NOT NULL,
    notes TEXT,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Spatial index for POI coordinates
CREATE INDEX idx_poi_coordinates ON points_of_interest USING GIST (coordinates);
-- Other useful indexes for POIs
CREATE INDEX idx_poi_parent_entity ON points_of_interest(parent_entity_identifier, parent_entity_type);
CREATE INDEX idx_poi_tenant_id ON points_of_interest(tenant_id);
CREATE INDEX idx_poi_type ON points_of_interest(poi_type);

-- You might want to add CHECK constraints for enum-like VARCHAR fields if desired,
-- e.g., ALTER TABLE plots ADD CONSTRAINT check_plot_land_tenure_type CHECK (land_tenure_type IN ('OWNED', 'LEASED', ...));
-- However, application-level validation usually handles this.