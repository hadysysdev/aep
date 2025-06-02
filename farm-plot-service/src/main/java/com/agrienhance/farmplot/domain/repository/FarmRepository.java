package com.agrienhance.farmplot.domain.repository;

import com.agrienhance.farmplot.domain.entity.Farm;
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
public interface FarmRepository extends JpaRepository<Farm, UUID> {

    // Basic CRUD methods like save(), findById(), findAll(), deleteById() are
    // inherited.

    // Custom query examples using method name derivation:
    Optional<Farm> findByFarmIdentifierAndTenantId(UUID farmIdentifier, UUID tenantId);

    Page<Farm> findAllByTenantId(UUID tenantId, Pageable pageable);

    List<Farm> findAllByTenantIdAndOwnerReferenceId(UUID tenantId, UUID ownerReferenceId);

    List<Farm> findAllByTenantIdAndCountryCode(UUID tenantId, String countryCode, Pageable pageable);

    // Example of a more complex query using @Query if needed
    // This is just illustrative; for simple cases, method derivation is preferred.
    @Query("SELECT f FROM Farm f WHERE f.tenantId = :tenantId AND LOWER(f.farmName) LIKE LOWER(concat('%', :nameQuery, '%'))")
    Page<Farm> findByTenantIdAndFarmNameContainingIgnoreCase(
            @Param("tenantId") UUID tenantId,
            @Param("nameQuery") String nameQuery,
            Pageable pageable);

    // You can add more custom query methods here as needed.
    // For example, queries involving geospatial searches on
    // `generalLocationCoordinates`
    // would require native queries or integration with Hibernate Spatial functions
    // in @Query.
    // e.g., "SELECT f FROM Farm f WHERE f.tenantId = :tenantId AND
    // ST_DWithin(f.generalLocationCoordinates, :point, :distance) = true"
    // (Actual syntax for ST_DWithin might vary based on DB and Hibernate Spatial
    // dialect configuration)
}