package com.example.asset.controller;

import com.example.asset.dto.AssetAssignmentRequest;
import com.example.asset.dto.AssetReturnRequest;
import com.example.asset.entity.AssetAssignmentHistory;
import com.example.asset.service.AssetAssignmentService;
import com.example.asset.security.UserPrincipal; // <-- Import your principal class
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssetAssignmentController {

    private final AssetAssignmentService assignmentService;

    @PutMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> issueAsset(
            @Valid @RequestBody AssetAssignmentRequest request,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {

        assignmentService.assignAsset(request, adminPrincipal.getUser().getId());

        return ResponseEntity.ok(Map.of("message", "Asset allocated and checked-out successfully"));
    }

    @PutMapping("/return")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> returnAsset(
            @Valid @RequestBody AssetReturnRequest request,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) { // <-- Swapped here

        assignmentService.returnAsset(request, adminPrincipal.getUser().getId());

        return ResponseEntity.ok(Map.of("message", "Asset check-in processed and condition logged"));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    public ResponseEntity<List<AssetAssignmentHistory>> getHistoricalAuditTrail() {
        List<AssetAssignmentHistory> history = assignmentService.getTransactionHistory();
        return ResponseEntity.ok(history);
    }
}