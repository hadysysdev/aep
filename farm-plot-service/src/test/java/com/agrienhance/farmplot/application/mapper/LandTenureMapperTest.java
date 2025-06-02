package com.agrienhance.farmplot.application.mapper;

import com.agrienhance.farmplot.api.dto.landtenure.CreateOrUpdateLandTenureRequest;
import com.agrienhance.farmplot.api.dto.landtenure.LandTenureResponse;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.LandTenure;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.enums.LandTenureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { LandTenureMapperImpl.class }) // No direct geometry here
class LandTenureMapperTest {

    @Autowired
    private LandTenureMapper landTenureMapper;

    private Plot testPlot;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        Farm testFarm = Farm.builder().farmIdentifier(UUID.randomUUID()).tenantId(tenantId).build();
        testPlot = Plot.builder()
                .plotIdentifier(UUID.randomUUID())
                .farm(testFarm)
                .tenantId(tenantId) // Important: Plot must have tenantId for mapper to pick up for LandTenure if
                                    // needed
                .build();
    }

    @Test
    void shouldMapRequestToLandTenure() {
        CreateOrUpdateLandTenureRequest request = CreateOrUpdateLandTenureRequest.builder()
                .tenureType(LandTenureType.LEASED)
                .leaseStartDate(LocalDate.of(2025, 1, 1))
                .leaseEndDate(LocalDate.of(2025, 12, 31))
                .ownerDetails("Mr. Lessor")
                .build();

        LandTenure tenure = landTenureMapper.requestToLandTenure(request);

        assertThat(tenure).isNotNull();
        assertThat(tenure.getTenureType()).isEqualTo(LandTenureType.LEASED);
        assertThat(tenure.getLeaseStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        // Ignored fields
        assertThat(tenure.getPlot()).isNull();
        assertThat(tenure.getTenantId()).isNull(); // To be set by service
    }

    @Test
    void shouldMapLandTenureToResponse() {
        LandTenure tenure = LandTenure.builder()
                .landTenureIdentifier(UUID.randomUUID())
                .plot(testPlot) // Associate with the plot
                .tenureType(LandTenureType.OWNED)
                .tenantId(testPlot.getTenantId())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .version(1L)
                .build();

        LandTenureResponse response = landTenureMapper.landTenureToLandTenureResponse(tenure);

        assertThat(response).isNotNull();
        assertThat(response.getLandTenureIdentifier()).isEqualTo(tenure.getLandTenureIdentifier());
        assertThat(response.getPlotIdentifier()).isEqualTo(testPlot.getPlotIdentifier());
        assertThat(response.getTenureType()).isEqualTo(LandTenureType.OWNED);
        assertThat(response.getTenantId()).isEqualTo(testPlot.getTenantId());
    }
}