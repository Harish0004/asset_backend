package com.example.asset.dto;

import com.example.asset.entity.Asset.AssetStatus;
import com.example.asset.entity.Asset.ConditionState;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetResponseDTO {
    private Long id;
    private String name;
    private String type;
    private AssetStatus status;
    private ConditionState conditionState;
    private String serialNumber;
    private String assignedToUsername;
    private Map<String, String> metadata;
    private LocalDateTime createdAt;
}