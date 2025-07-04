package com.agrienhance.farmplot.api.controller;

import com.agrienhance.farmplot.api.dto.landtenure.CreateOrUpdateLandTenureRequest;
import com.agrienhance.farmplot.api.dto.landtenure.LandTenureResponse;
import com.agrienhance.farmplot.api.dto.plot.CreatePlotRequest;
import com.agrienhance.farmplot.api.dto.plot.PlotResponse;
import com.agrienhance.farmplot.api.dto.plot.UpdatePlotRequest;
import com.agrienhance.farmplot.application.service.PlotApplicationService;

// import com.agrienhance.farmplot.application.service.PlotApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

import com.agrienhance.farmplot.api.dto.poi.CreatePointOfInterestRequest;
import com.agrienhance.farmplot.api.dto.poi.PointOfInterestResponse;
import com.agrienhance.farmplot.application.service.PointOfInterestApplicationService; // Import
import com.agrienhance.farmplot.domain.enums.ParentEntityType; // Import
import org.springframework.data.domain.Page; // If using paginated list
import org.springframework.data.domain.Pageable; // If using paginated list

import java.net.URI;
import java.util.List; // If using non-paginated list

@RestController
@RequestMapping("/v1/plots")
@Tag(name = "Plot Management", description = "APIs for managing plots")
@AllArgsConstructor
public class PlotController {

        // Placeholder for the application service
        private final PlotApplicationService plotApplicationService;
        private final PointOfInterestApplicationService poiApplicationService; // Add this

        private UUID getAuthenticatedTenantId() {
                // TODO: Replace with actual logic to extract tenantId from Spring Security
                // context
                // For now, we return a hardcoded UUID for testing purposes.
                // This MUST be replaced before going to production.
                return UUID.fromString("a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5");
        }

        @Operation(summary = "Define a new plot for a farm")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Plot created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlotResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., farm not found, invalid geometry)")
        })
        @PostMapping
        // @PreAuthorize("hasAuthority('CREATE_PLOT')")
        public ResponseEntity<PlotResponse> definePlot(
                        @Valid @RequestBody CreatePlotRequest createPlotRequest) {

                PlotResponse createdPlot = plotApplicationService.createPlot(createPlotRequest,
                                getAuthenticatedTenantId());
                URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                                .path("/{plotIdentifier}")
                                .buildAndExpand(createdPlot.getPlotIdentifier())
                                .toUri();
                return ResponseEntity.created(location).body(createdPlot);

        }

        @Operation(summary = "Get plot details by its identifier")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Plot details retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlotResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Plot not found")
        })
        @GetMapping("/{plotIdentifier}")
        // @PreAuthorize("hasAuthority('VIEW_PLOT')")
        public ResponseEntity<PlotResponse> getPlotById(
                        @Parameter(description = "UUID of the plot to retrieve") @PathVariable UUID plotIdentifier) {
                PlotResponse plot = plotApplicationService.getPlotById(plotIdentifier, getAuthenticatedTenantId());
                return ResponseEntity.ok(plot);
        }

        @Operation(summary = "List all plots (paginated), optionally filtered by farm identifier")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "List of plots retrieved")
        })
        @GetMapping
        // @PreAuthorize("hasAuthority('LIST_PLOTS')")
        public ResponseEntity<Page<PlotResponse>> listPlots(
                        @Parameter(description = "Optional UUID of the farm to filter plots by") @RequestParam(required = false) UUID farmIdentifier,
                        // tenantId would come from security context
                        @Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "plotName") Pageable pageable) {
                Page<PlotResponse> plots = Page.empty(pageable);
                if (farmIdentifier != null) {
                        plots = plotApplicationService.listPlotsByFarm(farmIdentifier, getAuthenticatedTenantId(),
                                        pageable);
                } else {
                        plots = plotApplicationService.listPlots(getAuthenticatedTenantId(), pageable);
                }
                return ResponseEntity.ok(plots);

        }

        @Operation(summary = "Update an existing plot")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Plot updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlotResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "404", description = "Plot not found")
        })
        @PutMapping("/{plotIdentifier}")
        // @PreAuthorize("hasAuthority('UPDATE_PLOT')")
        public ResponseEntity<PlotResponse> updatePlot(
                        @Parameter(description = "UUID of the plot to update") @PathVariable UUID plotIdentifier,
                        @Valid @RequestBody UpdatePlotRequest updatePlotRequest) {
                PlotResponse updatedPlot = plotApplicationService.updatePlot(plotIdentifier, updatePlotRequest,
                                getAuthenticatedTenantId());
                return ResponseEntity.ok(updatedPlot);
        }

        @Operation(summary = "Delete a plot by its identifier")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Plot deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Plot not found")
        })
        @DeleteMapping("/{plotIdentifier}")
        // @PreAuthorize("hasAuthority('DELETE_PLOT')")
        public ResponseEntity<Void> deletePlot(
                        @Parameter(description = "UUID of the plot to delete") @PathVariable UUID plotIdentifier) {
                plotApplicationService.deletePlot(plotIdentifier, getAuthenticatedTenantId());
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Get land tenure information for a specific plot")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Land tenure retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LandTenureResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Plot or Land Tenure not found")
        })
        @GetMapping("/{plotIdentifier}/land-tenure")
        // @PreAuthorize("hasAuthority('MANAGE_TENURE')")
        public ResponseEntity<LandTenureResponse> getLandTenureForPlot(
                        @Parameter(description = "UUID of the plot") @PathVariable UUID plotIdentifier) {

                LandTenureResponse response = plotApplicationService.getLandTenureForPlot(plotIdentifier,
                                getAuthenticatedTenantId());
                return ResponseEntity.ok(response);

        }

        @Operation(summary = "Create or update land tenure information for a specific plot")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Land tenure created/updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LandTenureResponse.class))),
                        @ApiResponse(responseCode = "201", description = "Land tenure created successfully (if you distinguish create vs update response code)"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "404", description = "Plot not found")
        })
        @PutMapping("/{plotIdentifier}/land-tenure")
        // @PreAuthorize("hasAuthority('MANAGE_TENURE')")
        public ResponseEntity<LandTenureResponse> createOrUpdateLandTenure(
                        @Parameter(description = "UUID of the plot") @PathVariable UUID plotIdentifier,
                        @Valid @RequestBody CreateOrUpdateLandTenureRequest request) {
                LandTenureResponse response = plotApplicationService.createOrUpdateLandTenureForPlot(plotIdentifier,
                                request, getAuthenticatedTenantId());
                return ResponseEntity.ok(response); // Could also be CREATED if it was definitely a new resource
        }

        @Operation(summary = "Delete land tenure information for a specific plot")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Land tenure deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Plot or Land Tenure not found")
        })
        @DeleteMapping("/{plotIdentifier}/land-tenure")
        // @PreAuthorize("hasAuthority('MANAGE_TENURE')")
        public ResponseEntity<Void> deleteLandTenure(
                        @Parameter(description = "UUID of the plot") @PathVariable UUID plotIdentifier) {
                plotApplicationService.deleteLandTenureForPlot(plotIdentifier, getAuthenticatedTenantId());
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Create a Point Of Interest for a specific plot")
        // ... (ApiResponses similar to FarmController's createFarmPoi) ...
        @PostMapping("/{plotIdentifier}/pois")
        public ResponseEntity<PointOfInterestResponse> createPlotPoi(
                        @Parameter(description = "UUID of the plot") @PathVariable UUID plotIdentifier,
                        @Valid @RequestBody CreatePointOfInterestRequest request) {
                PointOfInterestResponse createdPoi = poiApplicationService.createPoi(
                                plotIdentifier,
                                ParentEntityType.PLOT,
                                getAuthenticatedTenantId(),
                                request);
                return new ResponseEntity<>(createdPoi, HttpStatus.CREATED);
        }

        @Operation(summary = "List all Points Of Interest for a specific plot")
        // ... (ApiResponses similar to FarmController's listFarmPois) ...
        @GetMapping("/{plotIdentifier}/pois")
        public ResponseEntity<List<PointOfInterestResponse>> listPlotPois( // Or Page<PointOfInterestResponse> with
                                                                           // Pageable
                        @Parameter(description = "UUID of the plot") @PathVariable UUID plotIdentifier) {
                List<PointOfInterestResponse> pois = poiApplicationService.listPoisByParent(plotIdentifier,
                                ParentEntityType.PLOT, getAuthenticatedTenantId());
                return ResponseEntity.ok(pois);
        }
}