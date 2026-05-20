package com.example.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportsDashboardDTO {
    private ReportsSummaryDTO summary;
    private List<CategoryUtilizationDTO> utilizationByCategory;
    private List<TicketResponseDTO> breachedTickets;
    private int assetExportCount;
    private int ticketExportCount;
}
