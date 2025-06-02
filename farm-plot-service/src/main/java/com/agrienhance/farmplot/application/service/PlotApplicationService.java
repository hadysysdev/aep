package com.agrienhance.farmplot.application.service;

import com.agrienhance.farmplot.api.dto.landtenure.CreateOrUpdateLandTenureRequest;
import com.agrienhance.farmplot.api.dto.landtenure.LandTenureResponse;
import com.agrienhance.farmplot.api.dto.plot.CreatePlotRequest;
import com.agrienhance.farmplot.api.dto.plot.PlotResponse;
import com.agrienhance.farmplot.api.dto.plot.UpdatePlotRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PlotApplicationService {

    PlotResponse createPlot(CreatePlotRequest createPlotRequest);

    PlotResponse getPlotById(UUID plotIdentifier, UUID tenantId);

    Page<PlotResponse> listPlotsByFarm(UUID farmIdentifier, UUID tenantId, Pageable pageable);

    Page<PlotResponse> listAllPlotsForTenant(UUID tenantId, Pageable pageable); // New method

    PlotResponse updatePlot(UUID plotIdentifier, UpdatePlotRequest updatePlotRequest, UUID tenantId);

    void deletePlot(UUID plotIdentifier, UUID tenantId);

    LandTenureResponse getLandTenureForPlot(UUID plotIdentifier, UUID tenantId);

    LandTenureResponse createOrUpdateLandTenureForPlot(UUID plotIdentifier,
            CreateOrUpdateLandTenureRequest request,
            UUID tenantId);

    void deleteLandTenureForPlot(UUID plotIdentifier, UUID tenantId);
}