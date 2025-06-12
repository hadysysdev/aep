package com.agrienhance.farmplot.domain.repository;

import com.agrienhance.farmplot.domain.entity.Plot;
import org.locationtech.jts.geom.Geometry; // For geospatial queries
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.locationtech.jts.geom.Polygon; // Assuming plotGeometry is Polygon

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlotRepository extends JpaRepository<Plot, UUID> {

    // Basic CRUD methods are inherited.

    Optional<Plot> findByPlotIdentifierAndTenantId(UUID plotIdentifier, UUID tenantId);

    Page<Plot> findAllByTenantId(UUID tenantId, Pageable pageable);

    Page<Plot> findAllByFarm_FarmIdentifierAndTenantId(UUID farmIdentifier, UUID tenantId, Pageable pageable);

    List<Plot> findAllByCultivatorReferenceIdAndTenantId(UUID cultivatorReferenceId, UUID tenantId);

    boolean existsByPlotIdentifierAndTenantId(UUID plotIdentifier, UUID tenantId);

    boolean existsByFarm_FarmIdentifierAndTenantId(UUID farmIdentifier, UUID tenantId);

    // Example of a geospatial query using @Query with native SQL or HQL with
    // spatial functions.
    // This finds plots whose geometry intersects with a given bounding box
    // geometry.
    // The exact function (e.g., ST_Intersects, ST_Contains, ST_Within) and
    // parameter binding
    // will depend on your PostGIS/Hibernate Spatial setup.
    // This example uses named parameters.
    @Query("SELECT p FROM Plot p WHERE p.tenantId = :tenantId AND intersects(p.plotGeometry, :filterGeometry) = true")
    List<Plot> findPlotsIntersecting(
            @Param("tenantId") UUID tenantId,
            @Param("filterGeometry") Geometry filterGeometry // Pass a JTS Geometry (e.g., a Polygon representing a
                                                             // bounding box)
    );

    // Example: Find plots within a certain distance of a point (requires ST_DWithin
    // typically)
    // @Query("SELECT p FROM Plot p WHERE p.tenantId = :tenantId AND
    // ST_DWithin(p.plotGeometry, :point, :distance) = true")
    // List<Plot> findPlotsWithinDistance(
    // @Param("tenantId") UUID tenantId,
    // @Param("point") Point point,
    // @Param("distance") double distance
    // );

    // You can add more derived queries or @Query methods as your application needs
    // them.

    // Calculate area in square meters using PostGIS geography type
    // Note: This is a native query. The exact syntax for casting geometry to
    // geography
    // and ensuring the geometry is valid for such a cast might need care.
    @Query(value = "SELECT ST_Area(CAST(:plotGeometry AS geography)) FROM (SELECT CAST(:plotGeometryText AS geometry) AS plotGeometry) AS subquery", nativeQuery = true)
    Optional<Double> calculateAreaInSquareMeters(@Param("plotGeometryText") String plotGeometryText);
    // Passing geometry as WKT string to native query is often more straightforward
    // than binding JTS types directly in complex native spatial queries.
    // Alternatively, if you save the entity first, you can reference its ID:
    // @Query(value = "SELECT ST_Area(p.plot_geometry::geography) FROM plots p WHERE
    // p.plot_identifier = :plotId", nativeQuery = true)
    // Optional<Double> getCalculatedAreaInSquareMeters(@Param("plotId") UUID
    // plotId);
}