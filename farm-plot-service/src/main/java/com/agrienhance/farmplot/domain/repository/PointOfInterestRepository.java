package com.agrienhance.farmplot.domain.repository;

import com.agrienhance.farmplot.domain.entity.PointOfInterest;
import com.agrienhance.farmplot.domain.enums.ParentEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, UUID> {

    // Find a specific POI by its ID and tenant ID
    Optional<PointOfInterest> findByPoiIdentifierAndTenantId(UUID poiIdentifier, UUID tenantId);

    // Find all POIs for a specific parent entity (Farm or Plot) under a tenant
    List<PointOfInterest> findAllByParentEntityIdentifierAndParentEntityTypeAndTenantId(
            UUID parentEntityIdentifier,
            ParentEntityType parentEntityType,
            UUID tenantId);

    // Paginated version of the above
    Page<PointOfInterest> findAllByParentEntityIdentifierAndParentEntityTypeAndTenantId(
            UUID parentEntityIdentifier,
            ParentEntityType parentEntityType,
            UUID tenantId,
            Pageable pageable);

    // Example: Find POIs of a specific type for a parent entity
    List<PointOfInterest> findAllByParentEntityIdentifierAndParentEntityTypeAndPoiTypeAndTenantId(
            UUID parentEntityIdentifier,
            ParentEntityType parentEntityType,
            com.agrienhance.farmplot.domain.enums.POIType poiType, // Fully qualify if POIType is also a DTO name
            UUID tenantId);

    // You might also want queries that find POIs within a certain geographic area,
    // similar to what we discussed for Plots, using @Query with spatial functions.
    // e.g., find POIs within a given Polygon (bounding box) for a tenant
    @Query("SELECT poi FROM PointOfInterest poi WHERE poi.tenantId = :tenantId AND within(poi.coordinates, :filterGeometry) = true")
    List<PointOfInterest> findPoisWithinGeometry(
            @Param("tenantId") UUID tenantId,
            @Param("filterGeometry") org.locationtech.jts.geom.Geometry filterGeometry);
}