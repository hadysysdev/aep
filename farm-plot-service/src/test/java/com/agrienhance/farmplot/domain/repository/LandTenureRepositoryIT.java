package com.agrienhance.farmplot.domain.repository;

import com.agrienhance.farmplot.config.AbstractIntegrationTest;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.LandTenure;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.enums.LandTenureType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional; // Ensure this is imported

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Transactional // Ensures tests run in transactions and roll back
public class LandTenureRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private LandTenureRepository landTenureRepository;

    @Autowired
    private PlotRepository plotRepository;

    @Autowired
    private FarmRepository farmRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private Farm testFarm;
    private Plot testPlot;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        // Clean up in specific order due to foreign key constraints
        landTenureRepository.deleteAllInBatch();
        plotRepository.deleteAllInBatch();
        farmRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();

        tenantId = UUID.randomUUID();
        testFarm = Farm.builder()
                .farmName("Farm for Tenure Tests")
                .ownerReferenceId(UUID.randomUUID())
                .countryCode("LT")
                .tenantId(tenantId)
                .build();
        farmRepository.saveAndFlush(testFarm);

        // Use a much smaller polygon, e.g., 0.001 x 0.001 degrees
        double side = 0.001; // Or similar small value like 0.0001
        double originX = 0.0;
        double originY = 0.0;

        Polygon plotGeom = createSimpleSquarePolygon(side, originX, originY);

        testPlot = Plot.builder()
                .farm(testFarm)
                .plotName("Plot for Tenure")
                .plotGeometry(plotGeom)
                .landTenureType(LandTenureType.UNKNOWN) // Initial type on plot
                .tenantId(testFarm.getTenantId())
                .build();
        plotRepository.saveAndFlush(testPlot);
    }

    // No @AfterEach needed as @Transactional will roll back changes.
    // If you disable rollback for specific tests, then manual cleanup might be
    // needed here.

    @Test
    void shouldSaveAndRetrieveLandTenure() {
        // Given
        LandTenure landTenure = LandTenure.builder()
                .plot(testPlot)
                .tenureType(LandTenureType.LEASED)
                .leaseStartDate(LocalDate.of(2025, 1, 1))
                .leaseEndDate(LocalDate.of(2026, 12, 31))
                .ownerDetails("Mr. Landlord")
                .tenantId(testPlot.getTenantId()) // Should be set by @PrePersist from plot
                .build();

        // When
        LandTenure savedLandTenure = landTenureRepository.save(landTenure);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<LandTenure> foundOpt = landTenureRepository.findById(savedLandTenure.getLandTenureIdentifier());
        assertThat(foundOpt).isPresent();
        LandTenure found = foundOpt.get();

        assertThat(found.getPlot().getPlotIdentifier()).isEqualTo(testPlot.getPlotIdentifier());
        assertThat(found.getTenureType()).isEqualTo(LandTenureType.LEASED);
        assertThat(found.getLeaseStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(found.getOwnerDetails()).isEqualTo("Mr. Landlord");
        assertThat(found.getTenantId()).isEqualTo(testPlot.getTenantId()); // Check tenantId consistency
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getVersion()).isNotNull().isEqualTo(0L);
    }

    @Test
    void findByPlot_PlotIdentifierAndTenantId_whenExists_shouldReturnLandTenure() {
        // Given
        LandTenure landTenure = LandTenure.builder()
                .plot(testPlot)
                .tenureType(LandTenureType.OWNED)
                .tenantId(testPlot.getTenantId())
                .build();
        landTenureRepository.saveAndFlush(landTenure);
        entityManager.clear();

        // When
        Optional<LandTenure> foundOpt = landTenureRepository
                .findByPlot_PlotIdentifierAndTenantId(testPlot.getPlotIdentifier(), testPlot.getTenantId());

        // Then
        assertThat(foundOpt).isPresent();
        assertThat(foundOpt.get().getTenureType()).isEqualTo(LandTenureType.OWNED);
    }

    @Test
    void findByPlot_PlotIdentifierAndTenantId_whenNotExists_shouldReturnEmpty() {
        // When
        Optional<LandTenure> foundOpt = landTenureRepository
                .findByPlot_PlotIdentifierAndTenantId(testPlot.getPlotIdentifier(), testPlot.getTenantId());
        // Then
        assertThat(foundOpt).isNotPresent();
    }

    @Test
    void existsByPlot_PlotIdentifierAndTenantId_shouldWorkCorrectly() {
        // Given
        assertThat(landTenureRepository.existsByPlot_PlotIdentifierAndTenantId(testPlot.getPlotIdentifier(),
                testPlot.getTenantId())).isFalse();

        LandTenure landTenure = LandTenure.builder()
                .plot(testPlot)
                .tenureType(LandTenureType.OWNED)
                .tenantId(testPlot.getTenantId())
                .build();
        landTenureRepository.saveAndFlush(landTenure);
        entityManager.clear();

        // Then
        assertThat(landTenureRepository.existsByPlot_PlotIdentifierAndTenantId(testPlot.getPlotIdentifier(),
                testPlot.getTenantId())).isTrue();
    }

    @Test
    void shouldEnforceUniquePlotIdentifierConstraintForLandTenure() {
        // Given: Create one LandTenure for the plot
        LandTenure tenure1 = LandTenure.builder()
                .plot(testPlot)
                .tenureType(LandTenureType.OWNED)
                .tenantId(testPlot.getTenantId())
                .build();
        landTenureRepository.saveAndFlush(tenure1);
        entityManager.clear();

        // When: Try to create another LandTenure for the SAME plot
        LandTenure tenure2 = LandTenure.builder()
                .plot(testPlot) // Same plot
                .tenureType(LandTenureType.LEASED)
                .tenantId(testPlot.getTenantId())
                .build();

        // Then: Should throw DataIntegrityViolationException due to unique constraint
        // on plot_identifier
        assertThrows(DataIntegrityViolationException.class, () -> {
            landTenureRepository.saveAndFlush(tenure2);
        });
    }

    @Test
    void deleteByPlot_PlotIdentifierAndTenantId_shouldDeleteCorrectRecord() {
        // Given
        LandTenure landTenure = LandTenure.builder()
                .plot(testPlot)
                .tenureType(LandTenureType.OWNED)
                .tenantId(testPlot.getTenantId())
                .build();
        landTenureRepository.saveAndFlush(landTenure);
        assertThat(landTenureRepository.existsByPlot_PlotIdentifierAndTenantId(testPlot.getPlotIdentifier(),
                testPlot.getTenantId())).isTrue();
        entityManager.clear();

        // When
        landTenureRepository.deleteByPlot_PlotIdentifierAndTenantId(testPlot.getPlotIdentifier(),
                testPlot.getTenantId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(landTenureRepository.existsByPlot_PlotIdentifierAndTenantId(testPlot.getPlotIdentifier(),
                testPlot.getTenantId())).isFalse();
    }
}