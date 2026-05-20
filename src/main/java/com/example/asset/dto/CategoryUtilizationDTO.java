package com.example.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUtilizationDTO {
    private String category;
    private int assigned;
    private int total;
    private double percent;
}
