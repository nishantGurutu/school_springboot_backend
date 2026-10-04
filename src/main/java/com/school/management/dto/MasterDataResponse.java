package com.school.management.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Master Data API response containing complete userDetails and future settings")
public class MasterDataResponse {

    @Builder.Default
    @Schema(description = "Indicates whether the request was successful", example = "true")
    private boolean success = true;

    @Builder.Default
    @Schema(description = "Status message", example = "Master data retrieved successfully")
    private String message = "Master data retrieved successfully";

    @Schema(description = "Comprehensive user details including all role-specific attributes")
    private Map<String, Object> userDetails;

    @Builder.Default
    @Schema(description = "System/App settings section (future expansion)")
    private Map<String, Object> settings = new LinkedHashMap<>();

    @Schema(description = "Nested data container matching standard enterprise API conventions")
    private Map<String, Object> data;
}
