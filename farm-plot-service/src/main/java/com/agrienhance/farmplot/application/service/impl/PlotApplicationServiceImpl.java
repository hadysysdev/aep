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

import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    @Override
    @Transactional
    public PlotResponse createPlot(CreatePlotRequest request, UUID tenantId) {
        // 1. Fetch the associated Farm to ensure it exists under the given tenant
        Farm farm = farmRepository.findByFarmIdentifierAndTenantId(request.getFarmIdentifier(),
                tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm",
                        request.getFarmIdentifier().toString() + " with tenant " + tenantId));

        // 2. Map DTO to Entity (excluding farm for now as per mapper config)
        Plot plot = plotMapper.createRequestToPlot(request);

        // 3. Set the fetched Farm entity and ensure tenantId consistency
        plot.setFarm(farm);
        plot.setTenantId(farm.getTenantId()); // Inherit tenantId from the farm

        Plot savedPlot = plotRepository.saveAndFlush(plot);
        entityManager.refresh(savedPlot);
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
    public Page<PlotResponse> listPlots(UUID tenantId, Pageable pageable) {

        Page<Plot> plotPage = plotRepository.findAllByTenantId(tenantId,
                pageable);
        return plotPage.map(plotMapper::plotToPlotResponse);

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

        // Clear the denormalized field on the plot and save it
        plot.setLandTenureType(null);
        plotRepository.save(plot);
    }

}