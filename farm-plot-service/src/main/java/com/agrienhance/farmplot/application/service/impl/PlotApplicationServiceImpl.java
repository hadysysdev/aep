package com.agrienhance.farmplot.application.service.impl;

import com.agrienhance.farmplot.api.dto.landtenure.CreateOrUpdateLandTenureRequest;
import com.agrienhance.farmplot.api.dto.landtenure.LandTenureResponse;
import com.agrienhance.farmplot.api.dto.plot.CreatePlotRequest;
import com.agrienhance.farmplot.api.dto.plot.PlotResponse;
import com.agrienhance.farmplot.api.dto.plot.UpdatePlotRequest;
import com.agrienhance.farmplot.application.exception.ResourceNotFoundException;
import com.agrienhance.farmplot.application.mapper.LandTenureMapper;
import com.agrienhance.farmplot.application.mapper.PlotMapper;
import com.agrienhance.farmplot.application.service.PlotApplicationService;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.LandTenure;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.repository.FarmRepository;
import com.agrienhance.farmplot.domain.repository.LandTenureRepository;
import com.agrienhance.farmplot.domain.repository.PlotRepository;

import lombok.AllArgsConstructor;

import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class PlotApplicationServiceImpl implements PlotApplicationService {

    private final PlotRepository plotRepository;
    private final FarmRepository farmRepository; // Needed to fetch Farm entity
    private final PlotMapper plotMapper;

    private final LandTenureRepository landTenureRepository;
    private final LandTenureMapper landTenureMapper;

    // We might also need a service to calculate area from geometry if not done by
    // database
    // private final GeospatialCalculationService geospatialService;

    // public PlotApplicationServiceImpl(PlotRepository plotRepository,
    // FarmRepository farmRepository,
    // PlotMapper plotMapper) {
    // this.plotRepository = plotRepository;
    // this.farmRepository = farmRepository;
    // this.plotMapper = plotMapper;
    // }

    @Override
    @Transactional
    public PlotResponse createPlot(CreatePlotRequest request) {
        // 1. Fetch the associated Farm to ensure it exists under the given tenant
        Farm farm = farmRepository.findByFarmIdentifierAndTenantId(request.getFarmIdentifier(), request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm",
                        request.getFarmIdentifier().toString() + " with tenant " + request.getTenantId()));

        // 2. Map DTO to Entity (excluding farm for now as per mapper config)
        Plot plot = plotMapper.createRequestToPlot(request);

        // 3. Set the fetched Farm entity and ensure tenantId consistency
        plot.setFarm(farm);
        plot.setTenantId(farm.getTenantId()); // Inherit tenantId from the farm

        // 4. (Optional) Calculate area if plotGeometry is present
        if (plot.getPlotGeometry() != null) {
            // Area calculation: JTS area is in square degrees for WGS84.
            // Conversion to hectares is non-trivial and depends on latitude or requires an
            // equal-area projection.
            // For simplicity, we'll store a placeholder or use a dedicated geospatial
            // library/PostGIS function.
            // Let's assume it might be calculated by a @PrePersist or a DB trigger for now,
            // or a dedicated service.
            // plot.setCalculatedAreaHectares(calculateAreaInHectares(plot.getPlotGeometry()));
        }

        Plot savedPlot = plotRepository.save(plot);
        return plotMapper.plotToPlotResponse(savedPlot);
    }

    @Override
    @Transactional(readOnly = true)
    public PlotResponse getPlotById(UUID plotIdentifier, UUID tenantId) {
        Plot plot = plotRepository.findByPlotIdentifierAndTenantId(plotIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot", plotIdentifier.toString()));
        return plotMapper.plotToPlotResponse(plot);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlotResponse> listPlotsByFarm(UUID farmIdentifier, UUID tenantId, Pageable pageable) {
        // Ensure farm exists and belongs to tenant first (optional, depends on desired
        // strictness)
        if (!farmRepository.existsById(farmIdentifier)) { // Simplified check, proper tenant check needed
            throw new ResourceNotFoundException("Farm", farmIdentifier.toString());
        }

        if (!farmRepository.existsByFarmIdentifierAndTenantId(farmIdentifier, tenantId)) {
            throw new ResourceNotFoundException("Farm", farmIdentifier.toString() + " with tenant " + tenantId);

        }

        Page<Plot> plotPage = plotRepository.findAllByFarm_FarmIdentifierAndTenantId(farmIdentifier, tenantId,
                pageable);
        return plotPage.map(plotMapper::plotToPlotResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlotResponse> listAllPlotsForTenant(UUID tenantId, Pageable pageable) {
        Page<Plot> plotPage = plotRepository.findAllByTenantId(tenantId, pageable);
        return plotPage.map(plotMapper::plotToPlotResponse);
    }

    @Override
    @Transactional
    public PlotResponse updatePlot(UUID plotIdentifier, UpdatePlotRequest request, UUID tenantId) {
        Plot plot = plotRepository.findByPlotIdentifierAndTenantId(plotIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot", plotIdentifier.toString()));

        plotMapper.updatePlotFromRequest(request, plot);

        // Recalculate area if geometry changed
        if (request.getPlotGeometry() != null && plot.getPlotGeometry() != null) {
            // plot.setCalculatedAreaHectares(calculateAreaInHectares(plot.getPlotGeometry()));
        }

        Plot updatedPlot = plotRepository.save(plot);
        return plotMapper.plotToPlotResponse(updatedPlot);
    }

    @Override
    @Transactional
    public void deletePlot(UUID plotIdentifier, UUID tenantId) {
        Plot plot = plotRepository.findByPlotIdentifierAndTenantId(plotIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot", plotIdentifier.toString()));
        plotRepository.deleteById(plot.getPlotIdentifier());
    }

    // private Double calculateAreaInHectares(Polygon polygon) {
    // if (polygon == null || polygon.isEmpty()) {
    // return null;
    // }
    // // IMPORTANT: polygon.getArea() for WGS84 (SRID 4326) returns area in square
    // degrees.
    // // Accurate conversion to metric units (like hectares) requires either:
    // // 1. Reprojecting the geometry to an equal-area projection ON THE DATABASE
    // (using ST_Transform and ST_Area).
    // // 2. Using complex formulas that account for latitude (less accurate for
    // larger areas).
    // // For this example, we'll acknowledge this complexity. In a real app, this
    // would be a call
    // // to a PostGIS function via a native query or a specialized geospatial
    // library.
    // // Returning raw square degrees for now, or null.
    // // return polygon.getArea(); // This is in square degrees! Not hectares.
    // return null; // Placeholder - actual calculation is complex from raw WGS84
    // geometry.
    // }

    @Override
    @Transactional(readOnly = true)
    public LandTenureResponse getLandTenureForPlot(UUID plotIdentifier, UUID tenantId) {
        // First ensure plot exists and belongs to tenant
        Plot plot = plotRepository.findByPlotIdentifierAndTenantId(plotIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot", plotIdentifier.toString()));

        return landTenureRepository.findByPlot_PlotIdentifierAndTenantId(plot.getPlotIdentifier(), tenantId)
                .map(landTenureMapper::landTenureToLandTenureResponse)
                .orElseThrow(() -> new ResourceNotFoundException("LandTenure for Plot", plotIdentifier.toString()));
    }

    @Override
    @Transactional
    public LandTenureResponse createOrUpdateLandTenureForPlot(UUID plotIdentifier,
            CreateOrUpdateLandTenureRequest request,
            UUID tenantId) {
        Plot plot = plotRepository.findByPlotIdentifierAndTenantId(plotIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot", plotIdentifier.toString()));

        // Try to find existing tenure, or create a new one
        LandTenure landTenure = landTenureRepository.findByPlot_PlotIdentifierAndTenantId(plotIdentifier, tenantId)
                .orElseGet(() -> {
                    LandTenure newTenure = landTenureMapper.requestToLandTenure(request);
                    newTenure.setPlot(plot);
                    newTenure.setTenantId(plot.getTenantId()); // Set from plot
                    return newTenure;
                });

        // If it existed, update it
        if (landTenure.getLandTenureIdentifier() != null) { // Check if it's an existing entity
            landTenureMapper.updateLandTenureFromRequest(request, landTenure);
        }
        // If it was new (from orElseGet), its fields are already set by
        // requestToLandTenure,
        // and plot/tenantId are set above.

        LandTenure savedLandTenure = landTenureRepository.save(landTenure);
        plot.setLandTenureType(savedLandTenure.getTenureType());
        plotRepository.save(plot);
        return landTenureMapper.landTenureToLandTenureResponse(savedLandTenure);
    }

    @Override
    @Transactional
    public void deleteLandTenureForPlot(UUID plotIdentifier, UUID tenantId) {
        // Ensure plot exists and belongs to tenant
        Plot plot = plotRepository.findByPlotIdentifierAndTenantId(plotIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot", plotIdentifier.toString()));

        if (!landTenureRepository.existsByPlot_PlotIdentifierAndTenantId(plot.getPlotIdentifier(), tenantId)) {
            throw new ResourceNotFoundException("LandTenure for Plot", plotIdentifier.toString());
        }
        landTenureRepository.deleteByPlot_PlotIdentifierAndTenantId(plot.getPlotIdentifier(), tenantId);
    }

}