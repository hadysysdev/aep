package com.agrienhance.farmplot.application.service;

import com.agrienhance.farmplot.api.dto.poi.CreatePointOfInterestRequest;
import com.agrienhance.farmplot.api.dto.poi.PointOfInterestResponse;
import com.agrienhance.farmplot.api.dto.poi.UpdatePointOfInterestRequest;
import com.agrienhance.farmplot.domain.enums.ParentEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PointOfInterestApplicationService {

        PointOfInterestResponse createPoi(UUID parentEntityIdentifier,
                        ParentEntityType parentEntityType,
                        UUID tenantId,
                        CreatePointOfInterestRequest request); // tenantId will be in request or from
                                                               // context

        PointOfInterestResponse getPoiById(UUID poiIdentifier, UUID tenantId);

        List<PointOfInterestResponse> listPoisByParent(UUID parentEntityIdentifier, ParentEntityType parentEntityType,
                        UUID tenantId);

        Page<PointOfInterestResponse> listPoisByParentPaginated(UUID parentEntityIdentifier,
                        ParentEntityType parentEntityType, UUID tenantId, Pageable pageable);

        PointOfInterestResponse updatePoi(UUID poiIdentifier, UpdatePointOfInterestRequest request, UUID tenantId);

        void deletePoi(UUID poiIdentifier, UUID tenantId);
}