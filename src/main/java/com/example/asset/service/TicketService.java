package com.example.asset.service;

import com.example.asset.dto.TicketRequest;
import com.example.asset.dto.TicketResponseDTO;
import com.example.asset.entity.Asset;
import com.example.asset.entity.Ticket;
import com.example.asset.entity.User;
import com.example.asset.exception.InvalidWorkflowException;
import com.example.asset.exception.ResourceNotFoundException;
import com.example.asset.exception.TicketAccessDeniedException;
import com.example.asset.mapper.TicketMapper;
import com.example.asset.repository.AssetRepository;
import com.example.asset.repository.TicketRepository;
import com.example.asset.repository.UserRepository;
import com.example.asset.util.TicketSlaPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;

    /**
     * Admin workflow operation: Fetches all corporate platform tickets.
     */
    public List<TicketResponseDTO> getAllTickets() {
        return ticketRepository.findAllWithDetailsOrderByCreatedAtDesc().stream()
                .map(TicketMapper::toDto)
                .toList();
    }

    /**
     * Employee workflow operation: Fetches tickets raised by a specific user profile context.
     */
    public List<TicketResponseDTO> getTicketsByEmployee(Long employeeId) {
        return ticketRepository.findByRaisedByIdWithDetailsOrderByCreatedAtDesc(employeeId).stream()
                .map(TicketMapper::toDto)
                .toList();
    }

    /**
     * Technician workflow operation: Fetches tickets dispatched to a specific technician account.
     */
    public List<TicketResponseDTO> getTicketsByTechnician(Long techId) {
        return ticketRepository.findByTechnicianIdWithDetails(techId).stream()
                .map(TicketMapper::toDto)
                .toList();
    }

    /**
     * Handles ticket creation for Admins and Employees.
     * AUTOMATION RULE: If ticket priority is CRITICAL or HIGH,
     * the underlying asset condition shifts instantly to 'REPAIR_NEEDED'.
     */
    @Transactional
    public TicketResponseDTO createTicket(TicketRequest request, User currentUser) {
        // Enforce basic request data validations
        if (request.getAssetId() == null) {
            throw new InvalidWorkflowException("Validation failed: Asset selection is mandatory to raise a ticket.");
        }
        if (request.getIssueDescription() == null || request.getIssueDescription().trim().isEmpty()) {
            throw new InvalidWorkflowException("Validation failed: Issue description cannot be null or empty.");
        }

        // Verify that target asset exists in the database
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset context missing: No asset found with ID: " + request.getAssetId()));

        // Parse incoming priority value safely with dynamic string evaluation fallbacks
        Ticket.Priority priorityValue;
        try {
            priorityValue = Ticket.Priority.valueOf(request.getPriority().toUpperCase());
        } catch (Exception e) {
            priorityValue = Ticket.Priority.MEDIUM; // Default fallback assignment
        }

        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = Ticket.builder()
                .asset(asset)
                .raisedBy(currentUser)
                .issueDescription(request.getIssueDescription().trim())
                .priority(priorityValue)
                .status(Ticket.TicketStatus.OPEN)
                .createdAt(now)
                .deadlineAt(TicketSlaPolicy.deadlineFrom(now, priorityValue))
                .build();

        // [AUTOMATION CHECK 1] Check priority boundary thresholds to auto-flag hardware health states
        if (priorityValue == Ticket.Priority.CRITICAL || priorityValue == Ticket.Priority.HIGH) {
            asset.setConditionState(Asset.ConditionState.valueOf("REPAIR_NEEDED"));
            assetRepository.save(asset);
        }

        ticketRepository.save(ticket);
        return ticketRepository.findByIdWithDetails(ticket.getId())
                .map(TicketMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket could not be loaded after creation"));
    }

    /**
     * Handles ticket assignment logic managed exclusively by Admin accounts.
     * Enforces strict role verification and duplicate dispatch checks.
     */
    @Transactional
    public TicketResponseDTO dispatchToTechnician(Long ticketId, Long technicianId) {
        // 1. Fetch and validate the ticket existence
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment aborted: Ticket record not found with ID: " + ticketId));

        // 2. Fetch and validate the target technician profile
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment aborted: Assigned technician record not found with ID: " + technicianId));

        // 3. Enforce Role Validation Check
        if (!"TECHNICIAN".equalsIgnoreCase(technician.getRole().name())) {
            throw new InvalidWorkflowException("Assignment rejected: The designated user account (" + technician.getUsername() + ") is not registered as a TECHNICIAN.");
        }

        // 4. Guard against re-assignment conflicts
        if (ticket.getTechnician() != null) {
            throw new InvalidWorkflowException("Conflict: This ticket has already been dispatched to technician: " + ticket.getTechnician().getUsername());
        }

        ticket.setTechnician(technician);
        ticketRepository.save(ticket);
        return ticketRepository.findByIdWithDetails(ticketId)
                .map(TicketMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found after dispatch"));
    }

    /**
     * Handles progressive lifecycle transformations driven explicitly by the assigned technician context.
     * AUTOMATION RULE: Moving status to RESOLVED auto-reverts the asset state parameters back to AVAILABLE and GOOD.
     */
    @Transactional
    public TicketResponseDTO updateTicketStatus(Long ticketId, String statusString, User currentTech) {
        // 1. Locate the tracking ticket record
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow tracking failed: Ticket not found with ID: " + ticketId));

        // 2. Enforce structural security check: Ensure only the assigned technician can change its status
        if (ticket.getTechnician() == null || !ticket.getTechnician().getId().equals(currentTech.getId())) {
            throw new TicketAccessDeniedException("Access Denied: You are not authorized to modify work logs on this ticket.");
        }

        // 3. Validate incoming status values matching the target Enum layout bounds
        Ticket.TicketStatus newStatus;
        try {
            newStatus = Ticket.TicketStatus.valueOf(statusString.toUpperCase());
        } catch (Exception e) {
            throw new InvalidWorkflowException("Invalid state transition error: '" + statusString + "' is not a valid ticket status configuration.");
        }

        ticket.setStatus(newStatus);
        Asset asset = ticket.getAsset();

        // [AUTOMATION CHECK 2] Progressive hardware operational state adjustments
        if (newStatus == Ticket.TicketStatus.IN_PROGRESS) {
            asset.setStatus(Asset.AssetStatus.valueOf("UNDER_MAINTENANCE"));
        } else if (newStatus == Ticket.TicketStatus.RESOLVED) {
            // Keep the asset assigned if it still has an assignee; otherwise return it to inventory
            asset.setStatus(asset.getAssignedTo() != null
                    ? Asset.AssetStatus.ASSIGNED
                    : Asset.AssetStatus.AVAILABLE);
            asset.setConditionState(Asset.ConditionState.valueOf("GOOD")); // Hardware is verified fixed and returned to stockpools automatically
            ticket.setResolvedAt(LocalDateTime.now());
        }

        assetRepository.save(asset);
        ticketRepository.save(ticket);
        return ticketRepository.findByIdWithDetails(ticketId)
                .map(TicketMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found after status update"));
    }
}