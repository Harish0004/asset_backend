package com.example.asset.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRequest {
    private Long assetId;
    private String issueDescription;
    private String priority; // Must map to Ticket.Priority string values
}