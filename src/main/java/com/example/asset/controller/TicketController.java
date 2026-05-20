package com.example.asset.controller;

import com.example.asset.dto.TicketDispatchDTO;
import com.example.asset.dto.TicketRequest;
import com.example.asset.entity.Ticket;
import com.example.asset.entity.User;
import com.example.asset.security.UserPrincipal;
import com.example.asset.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'EMPLOYEE')")
    public ResponseEntity<List<Ticket>> getTickets(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User currentUser = userPrincipal.getUser();
        String role = currentUser.getRole().name();

        if ("ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.ok(ticketService.getAllTickets());
        } else if ("TECHNICIAN".equalsIgnoreCase(role)) {
            return ResponseEntity.ok(ticketService.getTicketsByTechnician(currentUser.getId()));
        } else {
            return ResponseEntity.ok(ticketService.getTicketsByEmployee(currentUser.getId()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Ticket> raiseTicket(
            @RequestBody TicketRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        User currentUser = userPrincipal.getUser();
        return ResponseEntity.ok(ticketService.createTicket(request, currentUser));
    }

    @PutMapping("/{id}/dispatch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Ticket> dispatchTicket(
            @PathVariable Long id,
            @RequestBody TicketDispatchDTO dispatchDto) {

        return ResponseEntity.ok(ticketService.dispatchToTechnician(id, dispatchDto.getTechnicianId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<Ticket> updateTicketStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        User currentTech = userPrincipal.getUser();
        return ResponseEntity.ok(ticketService.updateTicketStatus(id, status, currentTech));
    }
}