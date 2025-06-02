package com.agrienhance.farmplot.domain.repository;

import com.agrienhance.farmplot.domain.entity.LandTenure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LandTenureRepository extends JpaRepository<LandTenure, UUID> {

    // Find land tenure by the plot's identifier and tenant ID
    // This assumes the Plot object within LandTenure is eagerly fetched or its ID
    // is accessible
    // Or, if plot_identifier is directly on LandTenure as a foreign key column in
    // the DB:
    Optional<LandTenure> findByPlot_PlotIdentifierAndTenantId(UUID plotIdentifier, UUID tenantId);

    // Method to check if a LandTenure record exists for a given plot and tenant
    boolean existsByPlot_PlotIdentifierAndTenantId(UUID plotIdentifier, UUID tenantId);

    // Delete by plot identifier and tenant ID might be useful if managing tenure as
    // a strict sub-resource
    void deleteByPlot_PlotIdentifierAndTenantId(UUID plotIdentifier, UUID tenantId);
}