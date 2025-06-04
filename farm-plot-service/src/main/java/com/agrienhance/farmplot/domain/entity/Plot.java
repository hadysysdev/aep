package com.agrienhance.farmplot.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*; // Import Lombok annotations
import org.hibernate.annotations.UuidGenerator;
import org.locationtech.jts.geom.Polygon;

import com.agrienhance.farmplot.domain.enums.LandTenureType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor // JPA requirement
@AllArgsConstructor // Optional, useful with Builder
@Builder // To enable the Builder pattern
@EqualsAndHashCode(of = "plotIdentifier") // Important for JPA entities
@ToString // Customize, exclude 'farm' to avoid recursion if Farm has Plots
@Entity
@Table(name = "plots", indexes = {
        @Index(name = "idx_plots_farm_identifier", columnList = "farm_identifier"),
        @Index(name = "idx_plots_cultivator_reference_id", columnList = "cultivator_reference_id"),
        @Index(name = "idx_plots_tenant_id", columnList = "tenant_id")
})
public class Plot {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "plot_identifier", updatable = false, nullable = false)
    private UUID plotIdentifier;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_identifier", nullable = false)
    @ToString.Exclude // Important if Farm entity might also print Plots
    private Farm farm;

    @Size(max = 255)
    @Column(name = "plot_name")
    private String plotName;

    @Column(name = "cultivator_reference_id")
    private UUID cultivatorReferenceId;

    @NotNull
    @Column(name = "plot_geometry", nullable = false, columnDefinition = "geometry(Polygon,4326)")
    @ToString.Exclude
    private Polygon plotGeometry;

    @Column(name = "calculated_area_hectares", insertable = false, updatable = false, precision = 10, scale = 4)
    private BigDecimal calculatedAreaHectares;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "land_tenure_type", length = 50) // Nullable if a plot might not have it initially
    private LandTenureType landTenureType;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        updatedAt = createdAt = OffsetDateTime.now();
        // setCalculatedAreaPlaceholder();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
        // setCalculatedAreaPlaceholder();
    }

    private void setCalculatedAreaPlaceholder() {
        if (this.plotGeometry != null && !this.plotGeometry.isEmpty()) {
            // TODO: Replace with actual PostGIS generated column reading.
            // This current value is in SQUARE DEGREES from JTS and is NOT METRIC HECTARES.
            // For production, this field should be populated by a PostgreSQL generated
            // column:
            // e.g., calculated_area_hectares تقريبا AS (ST_Area(plot_geometry::geography) /
            // 10000.0) STORED;
            // For now, we can set it to the raw JTS area or null to indicate it's not yet
            // accurate.
            // Setting it to null might be safer to avoid confusion with the placeholder
            // value.
            this.calculatedAreaHectares = null; // Or this.plotGeometry.getArea() with a strong warning.
                                                // Let's use null to explicitly indicate it's not calculated by app.
        } else {
            this.calculatedAreaHectares = null;
        }
    }
}