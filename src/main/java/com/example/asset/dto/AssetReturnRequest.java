package com.example.asset.dto;


import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class AssetReturnRequest {
    @NotNull(message = "Asset ID is required")
    private Long assetId;

    @NotNull(message = "Condition on return is required")
    private String conditionState; // GOOD, REPAIR_NEEDED, DAMAGED
}
