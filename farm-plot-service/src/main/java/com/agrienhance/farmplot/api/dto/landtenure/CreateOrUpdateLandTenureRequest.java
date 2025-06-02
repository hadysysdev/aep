package com.agrienhance.farmplot.api.dto.landtenure;

import com.agrienhance.farmplot.domain.enums.LandTenureType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
// We might not need plotIdentifier in the body if it's in the path
// We might not need tenantId in the body if it's derived from plot or security context

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating or updating land tenure information for a plot.")
public class CreateOrUpdateLandTenureRequest {

    @NotNull(message = "Tenure type cannot be null.")
    @Schema(description = "Type of land tenure.", requiredMode = Schema.RequiredMode.REQUIRED, example = "LEASED")
    private LandTenureType tenureType;

    @Schema(description = "Lease start date (YYYY-MM-DD). Required if tenure type is LEASED.", example = "2025-01-01")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate leaseStartDate;

    @Schema(description = "Lease end date (YYYY-MM-DD). Required if tenure type is LEASED.", example = "2026-12-31")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate leaseEndDate;

    @Schema(description = "Details of the owner/lessor, if tenure type implies an external owner (e.g., LEASED).", example = "Mr. John Doe, +2547XXXXXXXX")
    private String ownerDetails;

    @Schema(description = "Reference to an agreement document.", example = "docs/lease_agreement_plotA1.pdf")
    private String agreementDocumentReference;

    // Validations for lease dates (e.g., endDate after startDate, required if
    // LEASED)
    // would typically be handled by custom validators or in the service layer.
}