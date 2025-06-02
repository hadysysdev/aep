package com.agrienhance.farmplot.application.service;

import com.agrienhance.farmplot.api.dto.farm.CreateFarmRequest;
import com.agrienhance.farmplot.api.dto.farm.FarmResponse;
import com.agrienhance.farmplot.api.dto.farm.UpdateFarmRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FarmApplicationService {

    /**
     * Creates a new farm based on the provided request data.
     *
     * @param createFarmRequest DTO containing data for the new farm.
     * @return FarmResponse DTO of the created farm.
     */
    FarmResponse createFarm(CreateFarmRequest createFarmRequest);

    /**
     * Retrieves a specific farm by its identifier and tenant ID.
     *
     * @param farmIdentifier The UUID of the farm.
     * @param tenantId       The UUID of the tenant who owns the farm.
     * @return FarmResponse DTO of the found farm.
     * @throws com.agrienhance.farmplot.application.exception.ResourceNotFoundException if
     *                                                                                  the
     *                                                                                  farm
     *                                                                                  is
     *                                                                                  not
     *                                                                                  found.
     */
    FarmResponse getFarmById(UUID farmIdentifier, UUID tenantId);

    /**
     * Lists all farms belonging to a specific tenant, with pagination.
     *
     * @param tenantId The UUID of the tenant.
     * @param pageable Pagination and sorting information.
     * @return A Page of FarmResponse DTOs.
     */
    Page<FarmResponse> listFarmsByTenant(UUID tenantId, Pageable pageable);

    /**
     * Updates an existing farm.
     *
     * @param farmIdentifier    The UUID of the farm to update.
     * @param updateFarmRequest DTO containing updated data.
     * @param tenantId          The UUID of the tenant who owns the farm.
     * @return FarmResponse DTO of the updated farm.
     * @throws com.agrienhance.farmplot.application.exception.ResourceNotFoundException if
     *                                                                                  the
     *                                                                                  farm
     *                                                                                  is
     *                                                                                  not
     *                                                                                  found.
     */
    FarmResponse updateFarm(UUID farmIdentifier, UpdateFarmRequest updateFarmRequest, UUID tenantId);

    /**
     * Deletes a farm by its identifier and tenant ID.
     *
     * @param farmIdentifier The UUID of the farm to delete.
     * @param tenantId       The UUID of the tenant who owns the farm.
     * @throws com.agrienhance.farmplot.application.exception.ResourceNotFoundException if
     *                                                                                  the
     *                                                                                  farm
     *                                                                                  is
     *                                                                                  not
     *                                                                                  found.
     */
    void deleteFarm(UUID farmIdentifier, UUID tenantId);
}