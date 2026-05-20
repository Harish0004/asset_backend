package com.example.asset.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class AssetAssignmentRequest {
    @NotNull(message = "Asset ID is required")
    private Long assetId;

    @NotNull(message = "Employee User ID is required")
    private Long employeeId;
}