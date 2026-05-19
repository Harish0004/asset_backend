package com.example.asset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetRequestDTO {

    @NotBlank(message = "Asset name is required")
    private String name;

    @NotBlank(message = "Asset type is required")
    private String type;

    @NotBlank(message = "Serial number is required")
    private String serialNumber;

    private Map<String, String> metadata;
}