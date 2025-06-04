package com.agrienhance.farmplot.domain.repository;

import com.agrienhance.farmplot.config.AbstractIntegrationTest;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.enums.LandTenureType; // If you set this on Plot

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
public class PlotRepositoryIT extends AbstractIntegrationTest {

    // Inject EntityManager if you use entityManager.flush() / .clear()
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlotRepository plotRepository;

    @Autowired
    private FarmRepository farmRepository; // To create a parent Farm

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326); // WGS84
    private Farm testFarm;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        // Clean up before each test to ensure independence
        plotRepository.deleteAllInBatch();
        farmRepository.deleteAllInBatch(); // Delete farms too, as plots depend on them
        entityManager.flush(); // Ensure deletes are executed
        entityManager.clear(); // Detach all entities

        tenantId = UUID.randomUUID();
        // testFarm = Farm.builder()
        // .farmName("Integration Test Farm for Plots")
        // .ownerReferenceId(UUID.randomUUID())
        // .countryCode("IT") // Integration Test country
        // .tenantId(tenantId)
        // .build();
        // farmRepository.save(testFarm); // Save the farm so plots can reference it

        // Create a NEW testFarm instance for each test method run
        Farm freshTestFarm = Farm.builder()
                .farmName("Integration Test Farm for Plots")
                .ownerReferenceId(UUID.randomUUID())
                .countryCode("IT")
                .tenantId(tenantId)
                .build();
        testFarm = farmRepository.saveAndFlush(freshTestFarm);

    }

    // @AfterEach
    // void tearDown() {
    // plotRepository.deleteAllInBatch(); // More efficient delete
    // farmRepository.deleteAllInBatch(); // More efficient delete
    // }

    private Polygon createSimpleSquarePolygon(double sideLength, double originX, double originY) {
        return geometryFactory.createPolygon(new Coordinate[] {
                new Coordinate(originX, originY),
                new Coordinate(originX + sideLength, originY),
                new Coordinate(originX + sideLength, originY + sideLength),
                new Coordinate(originX, originY + sideLength),
                new Coordinate(originX, originY) // Close the ring
        });
    }

    // Helper method to create a plot with a specific geometry and save it
    private Plot createAndSavePlotForFarm(String name, Polygon geometry, Farm farm) {
        Plot plot = Plot.builder()
                .farm(farm)
                .plotName(name)
                .plotGeometry(geometry)
                .landTenureType(LandTenureType.OWNED)
                .tenantId(farm.getTenantId())
                .build();
        return plotRepository.saveAndFlush(plot);
    }

    // Helper method to create a plot with a specific geometry and save it
    private Plot createAndSavePlotForFarm(String name, Polygon geometry) {
        return createAndSavePlotForFarm(name, geometry, testFarm);
    }

    @Test
    void shouldSaveAndRetrievePlot_andVerifyGeneratedArea() {
        // Given
        // Create a square polygon. For WGS84, 0.01 degrees is roughly 1.11km at the
        // equator.
        // Let's use a small square, e.g., 0.001 x 0.001 degrees.
        // The area calculation in hectares for WGS84 is complex to do by hand for
        // verification,
        // but we can check if a non-null, plausible positive value is generated.
        Polygon plotGeom = createSimpleSquarePolygon(0.001, 30.0, 10.0);

        Plot plot = Plot.builder()
                .farm(testFarm)
                .plotName("Test Plot Alpha")
                .plotGeometry(plotGeom)
                .landTenureType(LandTenureType.OWNED) // Assuming this field exists on Plot
                .tenantId(testFarm.getTenantId())
                .build();

        // When
        Plot savedPlot = plotRepository.save(plot);
        entityManager.flush(); // Ensure all SQL is executed
        entityManager.clear(); // Detach all entities so we fetch fresh from DB

        // Then
        assertThat(savedPlot).isNotNull();
        assertThat(savedPlot.getPlotIdentifier()).isNotNull();
        assertThat(savedPlot.getPlotName()).isEqualTo("Test Plot Alpha");
        assertThat(savedPlot.getTenantId()).isEqualTo(testFarm.getTenantId());
        assertThat(savedPlot.getFarm().getFarmIdentifier()).isEqualTo(testFarm.getFarmIdentifier());
        assertThat(savedPlot.getPlotGeometry()).isNotNull();
        assertThat(savedPlot.getPlotGeometry().equalsExact(plotGeom, 0.00001)).isTrue();

        // Verify the generated area
        // Fetch fresh from DB to ensure generated column value is loaded
        Optional<Plot> foundPlotOpt = plotRepository.findById(savedPlot.getPlotIdentifier());
        assertThat(foundPlotOpt).isPresent();
        Plot foundPlot = foundPlotOpt.get();

        System.out.println("Calculated Area (Hectares) from DB: " + foundPlot.getCalculatedAreaHectares()); // For
                                                                                                            // debugging

        assertThat(foundPlot.getCalculatedAreaHectares()).isNotNull();
        assertThat(foundPlot.getCalculatedAreaHectares().doubleValue()).isGreaterThan(0.0);
        // A 0.001 degree x 0.001 degree square near equator (approx 111m x 111m) is
        // roughly 1.23 hectares.
        // This is a sanity check. Actual value depends on PostGIS ST_Area(geography)
        // precision.
        // For example, near lat 10, lon 30, a 0.001x0.001 deg square is ~1.21 hectares.
        // We can assert a plausible range if needed, or just that it's positive.
        // For more precise area verification, you might need a known geometry with a
        // pre-calculated PostGIS area.

        assertThat(savedPlot.getCreatedAt()).isNotNull();
        assertThat(savedPlot.getUpdatedAt()).isNotNull();
        assertThat(savedPlot.getVersion()).isNotNull().isEqualTo(0L);
    }

    @Test
    void findByPlotIdentifierAndTenantId_whenExists_shouldReturnPlot() {
        // Given
        Plot plot = Plot.builder()
                .farm(testFarm)
                .plotName("Specific Plot")
                .plotGeometry(createSimpleSquarePolygon(0.0005, 30.0, 10.0))
                .tenantId(testFarm.getTenantId())
                .build();
        Plot savedPlot = plotRepository.save(plot);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Plot> foundOpt = plotRepository.findByPlotIdentifierAndTenantId(savedPlot.getPlotIdentifier(),
                testFarm.getTenantId());

        // Then
        assertThat(foundOpt).isPresent();
        assertThat(foundOpt.get().getPlotName()).isEqualTo("Specific Plot");
        assertThat(foundOpt.get().getCalculatedAreaHectares()).isNotNull();
    }

    @Test
    void shouldFailToSavePlot_whenFarmIsNull() {
        // Given
        Plot plot = Plot.builder()
                .farm(null) // Farm is @NotNull in Plot entity's @ManyToOne
                .plotName("Plot Without Farm")
                .plotGeometry(createSimpleSquarePolygon(0.001, 30.0, 10.0))
                .tenantId(tenantId)
                .build();

        // When & Then
        assertThrows(ConstraintViolationException.class, () -> {
            plotRepository.saveAndFlush(plot); // saveAndFlush to trigger constraints
        });
    }

    // TODO: Add a test for findPlotsIntersecting if you have it in your
    // PlotRepository
    // @Test
    // void findPlotsIntersecting_shouldReturnCorrectPlots() {
    // // Given
    // Plot plot1 =
    // plotRepository.save(Plot.builder().farm(testFarm).plotName("Plot1")
    // .plotGeometry(createSimpleSquarePolygon(0.01, 10.0, 10.0)) // 10,10 to
    // 10.01,10.01
    // .tenantId(tenantId).build());
    // Plot plot2 =
    // plotRepository.save(Plot.builder().farm(testFarm).plotName("Plot2")
    // .plotGeometry(createSimpleSquarePolygon(0.01, 10.005, 10.005)) // Overlaps
    // plot1: 10.005,10.005 to 10.015,10.015
    // .tenantId(tenantId).build());
    // Plot plot3 =
    // plotRepository.save(Plot.builder().farm(testFarm).plotName("Plot3")
    // .plotGeometry(createSimpleSquarePolygon(0.01, 11.0, 11.0)) // No overlap
    // .tenantId(tenantId).build());
    // entityManager.flush();
    // entityManager.clear();

    // Polygon queryPolygon = createSimpleSquarePolygon(0.008, 10.001, 10.001); //
    // Polygon that should intersect plot1 and plot2

    // // When
    // List<Plot> intersectingPlots = plotRepository.findPlotsIntersecting(tenantId,
    // queryPolygon);

    // // Then
    // assertThat(intersectingPlots).hasSize(2)
    // .extracting(Plot::getPlotName)
    // .containsExactlyInAnyOrder("Plot1", "Plot2");
    // }

    @Test
    void findPlotsIntersecting_shouldReturnCorrectlyIntersectingPlots() {
        // Arrange
        double side = 0.01;
        // Define geometries for our plots
        // Plot 1: A square from (0,0) to (10,10)
        Polygon geom1 = createSimpleSquarePolygon(side, 10.0, 10.0);
        Plot plot1 = createAndSavePlotForFarm("Plot Intersecting A", geom1);

        // Plot 2: A square from (5,5) to (15,15) - This will intersect plot1 and the
        // queryPolygon
        Polygon geom2 = createSimpleSquarePolygon(side, 10.005, 10.005);
        Plot plot2 = createAndSavePlotForFarm("Plot Intersecting B", geom2);

        // Plot 3: A square from (20,20) to (30,30) - This will NOT intersect the
        // queryPolygon
        Polygon geom3 = createSimpleSquarePolygon(side, 10.02, 10.02);
        Plot plot3 = createAndSavePlotForFarm("Plot Non-Intersecting", geom3);

        // Plot 4: Belongs to a different tenant, but would intersect queryPolygon
        // This tests tenant isolation.
        UUID otherTenantId = UUID.randomUUID();
        Farm otherFarm = farmRepository.saveAndFlush(Farm.builder()
                .farmName("Other Tenant Farm")
                .ownerReferenceId(UUID.randomUUID())
                .countryCode("OT")
                .tenantId(otherTenantId)
                .build());
        Polygon geom4 = createSimpleSquarePolygon(side, 10.0, 10.0); // Same geometry as plot1 but different tenant
        Plot plot4 = createAndSavePlotForFarm("Plot Other Tenant", geom4, otherFarm); // Re-using helper, but it uses
                                                                                      // 'testFarm'. Let's fix

        // Define the query polygon that should intersect plot1 and plot2
        // A polygon from (2,2) to (12,12)
        Polygon queryPolygon = createSimpleSquarePolygon(side, 10.002, 10.002);
        entityManager.clear(); // Clear persistence context to ensure fresh data from DB if needed, though
                               // findPlotsIntersecting should hit DB.

        // Act
        // We are querying for plots belonging to 'testFarm.getTenantId()'
        List<Plot> intersectingPlots = plotRepository.findPlotsIntersecting(testFarm.getTenantId(), queryPolygon);

        // Assert
        assertThat(intersectingPlots).isNotNull();
        assertThat(intersectingPlots).hasSize(2) // Should find plot1 and plot2
                .extracting(Plot::getPlotName)
                .containsExactlyInAnyOrder("Plot Intersecting A", "Plot Intersecting B");

        // Double-check that plot3 (non-intersecting) and plot4 (other tenant) are not
        // included
        assertThat(intersectingPlots).noneMatch(p -> p.getPlotName().equals("Plot Non-Intersecting"));
        assertThat(intersectingPlots).noneMatch(p -> p.getPlotName().equals("Plot Other Tenant"));
    }

}