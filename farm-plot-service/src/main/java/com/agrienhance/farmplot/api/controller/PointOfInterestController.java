package com.agrienhance.farmplot.api.controller;

import com.agrienhance.farmplot.api.dto.poi.PointOfInterestResponse;
import com.agrienhance.farmplot.api.dto.poi.UpdatePointOfInterestRequest;
import com.agrienhance.farmplot.application.service.PointOfInterestApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/pois")
@RequiredArgsConstructor
@Tag(name = "Point of Interest Management", description = "APIs for managing points of interest directly by their ID.")
public class PointOfInterestController {

    private final PointOfInterestApplicationService poiApplicationService;

    // This is a placeholder for getting the tenant ID.
    // In a real app, this would come from a security context (e.g., JWT).
    private UUID getTenantIdFromContext() {
        return UUID.fromString("a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5");
    }

    @GetMapping("/{poiIdentifier}")
    @Operation(summary = "Get a specific Point of Interest by its identifier")
    public ResponseEntity<PointOfInterestResponse> getPoiById(@PathVariable UUID poiIdentifier) {
        // Corrected method name from findPoiById to getPoiById
        PointOfInterestResponse response = poiApplicationService.getPoiById(poiIdentifier, getTenantIdFromContext());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{poiIdentifier}")
    @Operation(summary = "Update a specific Point of Interest")
    public ResponseEntity<PointOfInterestResponse> updatePoi(
            @PathVariable UUID poiIdentifier,
            @Valid @RequestBody UpdatePointOfInterestRequest request) {
        // Corrected argument order to match the service method signature
        PointOfInterestResponse response = poiApplicationService.updatePoi(poiIdentifier, request,
                getTenantIdFromContext());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{poiIdentifier}")
    @Operation(summary = "Delete a specific Point of Interest")
    public ResponseEntity<Void> deletePoi(@PathVariable UUID poiIdentifier) {
        poiApplicationService.deletePoi(poiIdentifier, getTenantIdFromContext());
        return ResponseEntity.noContent().build();
    }
}