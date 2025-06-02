package com.agrienhance.farmplot.api.dto.landtenure;

import com.agrienhance.farmplot.domain.enums.LandTenureType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload representing land tenure information for a plot.")
public class LandTenureResponse {

    @Schema(description = "Unique identifier of the land tenure record.")
    private UUID landTenureIdentifier;

    @Schema(description = "Identifier of the plot this tenure information belongs to.")
    private UUID plotIdentifier;

    @Schema(description = "Type of land tenure.")
    private LandTenureType tenureType;

    @Schema(description = "Lease start date, if applicable (YYYY-MM-DD).")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate leaseStartDate;

    @Schema(description = "Lease end date, if applicable (YYYY-MM-DD).")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate leaseEndDate;

    @Schema(description = "Details of the owner/lessor, if applicable.")
    private String ownerDetails;

    @Schema(description = "Reference to an agreement document (e.g., ID or path).")
    private String agreementDocumentReference;

    @Schema(description = "UUID of the tenant this land tenure record (and its plot) belongs to.")
    private UUID tenantId;

    @Schema(description = "Timestamp of when the record was created.", format = "date-time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;

    @Schema(description = "Timestamp of when the record was last updated.", format = "date-time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime updatedAt;

    @Schema(description = "Version number for optimistic locking.")
    private Long version;
}