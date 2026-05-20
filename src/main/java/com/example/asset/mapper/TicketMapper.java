package com.example.asset.mapper;

import com.example.asset.dto.TicketResponseDTO;
import com.example.asset.entity.Ticket;
import com.example.asset.util.TicketSlaPolicy;

public final class TicketMapper {

    private TicketMapper() {
    }

    public static TicketResponseDTO toDto(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        ensureDeadline(ticket);
        return TicketResponseDTO.builder()
                .id(ticket.getId())
                .assetId(ticket.getAsset() != null ? ticket.getAsset().getId() : null)
                .assetName(ticket.getAsset() != null ? ticket.getAsset().getName() : null)
                .serialNumber(ticket.getAsset() != null ? ticket.getAsset().getSerialNumber() : null)
                .raisedByUsername(ticket.getRaisedBy() != null ? ticket.getRaisedBy().getUsername() : null)
                .technicianUsername(ticket.getTechnician() != null ? ticket.getTechnician().getUsername() : null)
                .issueDescription(ticket.getIssueDescription())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .deadlineAt(ticket.getDeadlineAt())
                .resolvedAt(ticket.getResolvedAt())
                .slaBreached(TicketSlaPolicy.isBreached(ticket))
                .build();
    }

    private static void ensureDeadline(Ticket ticket) {
        if (ticket.getDeadlineAt() == null && ticket.getCreatedAt() != null) {
            ticket.setDeadlineAt(TicketSlaPolicy.deadlineFrom(ticket.getCreatedAt(), ticket.getPriority()));
        }
    }
}
