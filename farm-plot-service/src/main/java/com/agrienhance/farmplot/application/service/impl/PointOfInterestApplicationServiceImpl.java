package com.agrienhance.farmplot.application.service.impl;

import com.agrienhance.farmplot.api.dto.poi.CreatePointOfInterestRequest;
import com.agrienhance.farmplot.api.dto.poi.PointOfInterestResponse;
import com.agrienhance.farmplot.api.dto.poi.UpdatePointOfInterestRequest;
import com.agrienhance.farmplot.application.exception.ResourceNotFoundException;
import com.agrienhance.farmplot.application.exception.ValidationException; // A new custom exception
import com.agrienhance.farmplot.application.mapper.PointOfInterestMapper;
import com.agrienhance.farmplot.application.service.PointOfInterestApplicationService;
import com.agrienhance.farmplot.domain.entity.PointOfInterest;
import com.agrienhance.farmplot.domain.enums.ParentEntityType;
import com.agrienhance.farmplot.domain.repository.FarmRepository;
import com.agrienhance.farmplot.domain.repository.PlotRepository;
import com.agrienhance.farmplot.domain.repository.PointOfInterestRepository;

import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PointOfInterestApplicationServiceImpl implements PointOfInterestApplicationService {

    private final PointOfInterestRepository poiRepository;
    private final FarmRepository farmRepository;
    private final PlotRepository plotRepository;
    private final PointOfInterestMapper poiMapper;

    @Override
    @Transactional
    public PointOfInterestResponse createPoi(UUID parentEntityIdentifier,
            ParentEntityType parentEntityType,
            UUID tenantId,
            CreatePointOfInterestRequest request) {
        // Validate parent entity exists and belongs to the specified tenant
        validateParentEntity(parentEntityIdentifier, parentEntityType, tenantId);

        PointOfInterest poi = poiMapper.createRequestToPoi(request);
        poi.setParentEntityIdentifier(parentEntityIdentifier);
        poi.setParentEntityType(parentEntityType);
        poi.setTenantId(tenantId);

        PointOfInterest savedPoi = poiRepository.save(poi);
        return poiMapper.poiToPoiResponse(savedPoi);
    }

    @Override
    @Transactional(readOnly = true)
    public PointOfInterestResponse getPoiById(UUID poiIdentifier, UUID tenantId) {
        PointOfInterest poi = poiRepository.findByPoiIdentifierAndTenantId(poiIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointOfInterest", poiIdentifier.toString()));
        return poiMapper.poiToPoiResponse(poi);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PointOfInterestResponse> listPoisByParent(UUID parentEntityIdentifier,
            ParentEntityType parentEntityType, UUID tenantId) {
        // Validate parent entity exists and belongs to the tenant before listing its
        // POIs
        validateParentEntity(parentEntityIdentifier, parentEntityType, tenantId);

        List<PointOfInterest> pois = poiRepository.findAllByParentEntityIdentifierAndParentEntityTypeAndTenantId(
                parentEntityIdentifier, parentEntityType, tenantId);
        return pois.stream()
                .map(poiMapper::poiToPoiResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PointOfInterestResponse> listPoisByParentPaginated(UUID parentEntityIdentifier,
            ParentEntityType parentEntityType, UUID tenantId, Pageable pageable) {
        validateParentEntity(parentEntityIdentifier, parentEntityType, tenantId);
        Page<PointOfInterest> poiPage = poiRepository.findAllByParentEntityIdentifierAndParentEntityTypeAndTenantId(
                parentEntityIdentifier, parentEntityType, tenantId, pageable);
        return poiPage.map(poiMapper::poiToPoiResponse);
    }

    @Override
    @Transactional
    public PointOfInterestResponse updatePoi(UUID poiIdentifier, UpdatePointOfInterestRequest request, UUID tenantId) {
        PointOfInterest poi = poiRepository.findByPoiIdentifierAndTenantId(poiIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointOfInterest", poiIdentifier.toString()));

        poiMapper.updatePoiFromRequest(request, poi);

        PointOfInterest updatedPoi = poiRepository.save(poi);
        return poiMapper.poiToPoiResponse(updatedPoi);
    }

    @Override
    @Transactional
    public void deletePoi(UUID poiIdentifier, UUID tenantId) {
        PointOfInterest poi = poiRepository.findByPoiIdentifierAndTenantId(poiIdentifier, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointOfInterest", poiIdentifier.toString()));
        poiRepository.deleteById(poi.getPoiIdentifier());
    }

    // Helper method to validate parent entity
    private void validateParentEntity(UUID parentEntityIdentifier, ParentEntityType parentEntityType, UUID tenantId) {
        boolean parentExists;
        String parentTypeString = "";

        if (parentEntityType == ParentEntityType.FARM) {
            parentTypeString = "Farm";
            parentExists = farmRepository.findByFarmIdentifierAndTenantId(parentEntityIdentifier, tenantId).isPresent();
        } else if (parentEntityType == ParentEntityType.PLOT) {
            parentTypeString = "Plot";
            parentExists = plotRepository.findByPlotIdentifierAndTenantId(parentEntityIdentifier, tenantId).isPresent();
        } else {
            throw new ValidationException("Invalid parent entity type specified.");
        }

        if (!parentExists) {
            throw new ResourceNotFoundException(parentTypeString,
                    parentEntityIdentifier.toString() + " under tenant " + tenantId);
        }
    }
}