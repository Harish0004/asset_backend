package com.example.asset.dto;

import com.example.asset.entity.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDTO {
    private Long id;
    private Long assetId;
    private String assetName;
    private String serialNumber;
    private String raisedByUsername;
    private String technicianUsername;
    private String issueDescription;
    private Ticket.Priority priority;
    private Ticket.TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deadlineAt;
    private LocalDateTime resolvedAt;
    private boolean slaBreached;
}
