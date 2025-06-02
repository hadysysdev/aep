package com.agrienhance.farmplot.api.controller;

import com.agrienhance.farmplot.api.dto.farm.CreateFarmRequest;
import com.agrienhance.farmplot.api.dto.farm.FarmResponse;
import com.agrienhance.farmplot.api.dto.farm.UpdateFarmRequest;
// We'll need an application service later, for now, we can just define the interface or methods.
// import com.agrienhance.farmplot.application.service.FarmApplicationService; 
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
// import org.springframework.security.access.prepost.PreAuthorize; // For authorization later

import java.util.UUID;
import com.agrienhance.farmplot.api.dto.poi.CreatePointOfInterestRequest;
import com.agrienhance.farmplot.api.dto.poi.PointOfInterestResponse;
import com.agrienhance.farmplot.application.service.FarmApplicationService;
import com.agrienhance.farmplot.application.service.PointOfInterestApplicationService; // Import
import com.agrienhance.farmplot.domain.enums.ParentEntityType; // Import
import org.springframework.data.domain.Page; // If using paginated list
import org.springframework.data.domain.Pageable; // If using paginated list
import java.util.List; // If using non-paginated list

@RestController
@RequestMapping("/v1/farms")
@Tag(name = "Farm Management", description = "APIs for managing farms")
@AllArgsConstructor
public class FarmController {

        // Placeholder for the application service - to be implemented
        private final FarmApplicationService farmApplicationService;
        private final PointOfInterestApplicationService poiApplicationService; // Add this

        // @Autowired
        // public FarmController(FarmApplicationService farmApplicationService) {
        // this.farmApplicationService = farmApplicationService;
        // }

        @Operation(summary = "Register a new farm")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Farm created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FarmResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PostMapping
        // @PreAuthorize("hasAuthority('CREATE_FARM')") // Example for security
        public ResponseEntity<FarmResponse> registerFarm(
                        @Valid @RequestBody CreateFarmRequest createFarmRequest) {
                // FarmResponse createdFarm =
                // farmApplicationService.createFarm(createFarmRequest);
                // For stub:
                FarmResponse createdFarm = FarmResponse.builder()
                                .farmIdentifier(UUID.randomUUID()) // Mock ID
                                .farmName(createFarmRequest.getFarmName())
                                .ownerReferenceId(createFarmRequest.getOwnerReferenceId())
                                .tenantId(createFarmRequest.getTenantId())
                                // ... other fields ...
                                .build();
                return new ResponseEntity<>(createdFarm, HttpStatus.CREATED);
        }

        @Operation(summary = "Get farm details by its identifier")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Farm details retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FarmResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Farm not found")
        })
        @GetMapping("/{farmIdentifier}")
        // @PreAuthorize("hasAuthority('VIEW_FARM')")
        public ResponseEntity<FarmResponse> getFarmById(
                        @Parameter(description = "UUID of the farm to retrieve") @PathVariable UUID farmIdentifier) {
                // FarmResponse farm = farmApplicationService.getFarmById(farmIdentifier,
                // tenantId); // tenantId from security context
                // For stub:
                if (farmIdentifier.equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) { // Mock a
                                                                                                      // non-found case
                        return ResponseEntity.notFound().build();
                }
                FarmResponse farm = FarmResponse.builder().farmIdentifier(farmIdentifier).farmName("Mocked Farm")
                                .build();
                return ResponseEntity.ok(farm);
        }

        @Operation(summary = "List all farms for the current tenant (paginated)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "List of farms retrieved")
        })
        @GetMapping
        // @PreAuthorize("hasAuthority('LIST_FARMS')")
        public ResponseEntity<Page<FarmResponse>> listFarms(
                        // We would get tenantId from security context in a real app
                        // For now, we can imagine it's implicitly handled or passed if needed for
                        // service layer
                        @Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "farmName") Pageable pageable) {
                // Page<FarmResponse> farms = farmApplicationService.listFarmsByTenant(tenantId,
                // pageable);
                // For stub:
                Page<FarmResponse> farms = Page.empty(pageable); // Return empty page for stub
                return ResponseEntity.ok(farms);
        }

        @Operation(summary = "Update an existing farm")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Farm updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FarmResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "404", description = "Farm not found")
        })
        @PutMapping("/{farmIdentifier}")
        // @PreAuthorize("hasAuthority('UPDATE_FARM')")
        public ResponseEntity<FarmResponse> updateFarm(
                        @Parameter(description = "UUID of the farm to update") @PathVariable UUID farmIdentifier,
                        @Valid @RequestBody UpdateFarmRequest updateFarmRequest) {
                // FarmResponse updatedFarm = farmApplicationService.updateFarm(farmIdentifier,
                // updateFarmRequest, tenantId);
                // For stub:
                FarmResponse updatedFarm = FarmResponse.builder().farmIdentifier(farmIdentifier)
                                .farmName(updateFarmRequest.getFarmName()).build();
                return ResponseEntity.ok(updatedFarm);
        }

        @Operation(summary = "Delete a farm by its identifier")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Farm deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Farm not found")
        })
        @DeleteMapping("/{farmIdentifier}")
        // @PreAuthorize("hasAuthority('DELETE_FARM')")
        public ResponseEntity<Void> deleteFarm(
                        @Parameter(description = "UUID of the farm to delete") @PathVariable UUID farmIdentifier) {
                // farmApplicationService.deleteFarm(farmIdentifier, tenantId);
                // For stub:
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Create a Point Of Interest for a specific farm")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "POI created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PointOfInterestResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "404", description = "Farm not found")
        })
        @PostMapping("/{farmIdentifier}/pois")
        public ResponseEntity<PointOfInterestResponse> createFarmPoi(
                        @Parameter(description = "UUID of the farm") @PathVariable UUID farmIdentifier,
                        @Valid @RequestBody CreatePointOfInterestRequest request) {
                // UUID tenantId = ... from security context ...
                UUID tenantId = UUID.randomUUID(); // Placeholder for tenantId
                // For createPoi, the request DTO already contains tenantId
                PointOfInterestResponse createdPoi = poiApplicationService.createPoi(
                                farmIdentifier,
                                ParentEntityType.FARM,
                                tenantId,
                                request);
                return new ResponseEntity<>(createdPoi, HttpStatus.CREATED);
        }

        @Operation(summary = "List all Points Of Interest for a specific farm")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "List of POIs retrieved"),
                        @ApiResponse(responseCode = "404", description = "Farm not found")
        })
        @GetMapping("/{farmIdentifier}/pois")
        public ResponseEntity<List<PointOfInterestResponse>> listFarmPois( // Or Page<PointOfInterestResponse> with
                                                                           // Pageable
                        @Parameter(description = "UUID of the farm") @PathVariable UUID farmIdentifier) {
                // UUID tenantId = ... from security context ...
                UUID tenantId = UUID.randomUUID(); // Placeholder
                // If using Pageable: Page<PointOfInterestResponse> pois =
                // poiApplicationService.listPoisByParentPaginated(farmIdentifier,
                // ParentEntityType.FARM, tenantId, pageable);
                List<PointOfInterestResponse> pois = poiApplicationService.listPoisByParent(farmIdentifier,
                                ParentEntityType.FARM, tenantId);
                return ResponseEntity.ok(pois);
        }
}