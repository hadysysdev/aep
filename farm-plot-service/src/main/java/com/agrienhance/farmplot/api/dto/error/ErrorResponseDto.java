package com.agrienhance.farmplot.api.dto.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List; // For multiple error messages, e.g., validation errors

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response payload.")
public class ErrorResponseDto {

    @Schema(description = "Timestamp of when the error occurred.", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime timestamp;

    @Schema(description = "HTTP Status code.", example = "404", requiredMode = Schema.RequiredMode.REQUIRED)
    private int status;

    @Schema(description = "A short, human-readable summary of the problem.", example = "Not Found", requiredMode = Schema.RequiredMode.REQUIRED)
    private String error; // e.g., "Not Found", "Bad Request"

    @Schema(description = "A human-readable explanation specific to this occurrence of the problem.", example = "Farm with identifier [xyz] not found.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "The path of the request that resulted in the error.", example = "/v1/farms/xyz", requiredMode = Schema.RequiredMode.REQUIRED)
    private String path;

    @Schema(description = "List of validation errors, if applicable.")
    private List<String> validationErrors; // For handling multiple validation errors

    public ErrorResponseDto(int status, String error, String message, String path) {
        this.timestamp = OffsetDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}