package com.agrienhance.farmplot.application.service.impl;

import com.agrienhance.farmplot.api.dto.farm.CreateFarmRequest;
import com.agrienhance.farmplot.api.dto.farm.FarmResponse;
import com.agrienhance.farmplot.api.dto.farm.UpdateFarmRequest;
import com.agrienhance.farmplot.application.exception.ResourceNotFoundException;
import com.agrienhance.farmplot.application.mapper.FarmMapper; // Import the mapper
import com.agrienhance.farmplot.application.service.FarmApplicationService;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.repository.FarmRepository;

import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class FarmApplicationServiceImpl implements FarmApplicationService {

    private final FarmRepository farmRepository;
    private final FarmMapper farmMapper; // Inject the mapper

    @Override
    @Transactional
    public FarmResponse createFarm(CreateFarmRequest request, UUID tenantId) {
        Farm farm = farmMapper.createRequestToFarm(request);
        farm.setTenantId(tenantId);
        Farm savedFarm = farmRepository.save(farm);
        return farmMapper.farmToFarmResponse(savedFarm);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmResponse getFarmById(UUID farmIdentifier, UUID tenantId) {
        Farm farm = farmRepository.findByFarmIdentifierAndTenantId(farmIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", farmIdentifier.toString()));
        return farmMapper.farmToFarmResponse(farm);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FarmResponse> listFarmsByTenant(UUID tenantId, Pageable pageable) {
        Page<Farm> farmPage = farmRepository.findAllByTenantId(tenantId, pageable);
        return farmPage.map(farmMapper::farmToFarmResponse); // Use method reference
    }

    @Override
    @Transactional
    public FarmResponse updateFarm(UUID farmIdentifier, UpdateFarmRequest request, UUID tenantId) {
        Farm farm = farmRepository.findByFarmIdentifierAndTenantId(farmIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", farmIdentifier.toString()));

        // Mapper applies non-null updates from the request
        farmMapper.updateFarmFromRequest(request, farm); // Apply updates

        // --- Explicit handling for fields that CAN be set to null ---
        if (request.getNotes() == null) {
            farm.setNotes(null);
        }

        Farm updatedFarm = farmRepository.save(farm);
        return farmMapper.farmToFarmResponse(updatedFarm);
    }

    @Override
    @Transactional
    public void deleteFarm(UUID farmIdentifier, UUID tenantId) {
        Farm farm = farmRepository.findByFarmIdentifierAndTenantId(farmIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", farmIdentifier.toString()));
        // Now that we've confirmed it exists under the tenant, we can delete by its
        // primary key.
        farmRepository.deleteById(farm.getFarmIdentifier());
    }
}