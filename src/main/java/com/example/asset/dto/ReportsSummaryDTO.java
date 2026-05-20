package com.example.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportsSummaryDTO {
    private int totalAssets;
    private int assignedAssets;
    private double assetUtilizationPercent;
    private double avgResolutionDays;
    private int activeTickets;
    private int maintenanceDueCount;
    private double maintenanceRatePercent;
    private int slaBreachedTickets;
}
