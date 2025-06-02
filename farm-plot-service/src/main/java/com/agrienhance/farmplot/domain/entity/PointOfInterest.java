package com.agrienhance.farmplot.domain.entity;

import com.agrienhance.farmplot.domain.enums.POIType;
import com.agrienhance.farmplot.domain.enums.ParentEntityType;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "poiIdentifier")
@ToString(of = { "poiIdentifier", "poiName", "poiType", "parentEntityIdentifier", "parentEntityType" })
@Entity
@Table(name = "points_of_interest", indexes = {
        @Index(name = "idx_poi_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_poi_parent_entity", columnList = "parent_entity_identifier, parent_entity_type")
})
public class PointOfInterest {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "poi_identifier", updatable = false, nullable = false)
    private UUID poiIdentifier;

    @NotNull
    @Column(name = "parent_entity_identifier", nullable = false)
    private UUID parentEntityIdentifier; // Identifier of the Farm or Plot

    @NotNull
    @Enumerated(EnumType.STRING) // Could also use a simple String if types are dynamic or many
    @Column(name = "parent_entity_type", nullable = false, length = 10) // e.g., "FARM", "PLOT"
    private ParentEntityType parentEntityType; // Custom enum for type safety

    @Size(max = 255)
    @Column(name = "poi_name")
    private String poiName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "poi_type", nullable = false, length = 50)
    private POIType poiType; // Using the POIType enum we defined

    @NotNull
    @Column(name = "coordinates", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point coordinates; // JTS Point for the POI's location

    @Lob
    @Column(name = "notes")
    private String notes;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        updatedAt = createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}