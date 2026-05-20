package com.example.asset.service;

import com.example.asset.dto.*;
import com.example.asset.entity.Asset;
import com.example.asset.entity.Ticket;
import com.example.asset.mapper.TicketMapper;
import com.example.asset.repository.AssetRepository;
import com.example.asset.repository.TicketRepository;
import com.example.asset.util.TicketSlaPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AssetRepository assetRepository;
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public ReportsDashboardDTO getDashboard() {
        List<Asset> assets = assetRepository.findAllWithAssignedUser();
        List<Ticket> tickets = ticketRepository.findAllWithDetailsOrderByCreatedAtDesc();

        int totalAssets = assets.size();
        int assignedAssets = (int) assets.stream()
                .filter(a -> a.getStatus() == Asset.AssetStatus.ASSIGNED)
                .count();
        double utilization = totalAssets == 0 ? 0.0 : (assignedAssets * 100.0) / totalAssets;

        int maintenanceDue = (int) assets.stream()
                .filter(this::isMaintenanceDue)
                .count();
        double maintenanceRate = totalAssets == 0 ? 0.0 : (maintenanceDue * 100.0) / totalAssets;

        int activeTickets = (int) tickets.stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.OPEN
                        || t.getStatus() == Ticket.TicketStatus.IN_PROGRESS)
                .count();

        double avgResolutionDays = calculateAvgResolutionDays(tickets);

        List<TicketResponseDTO> breachedTickets = tickets.stream()
                .filter(TicketSlaPolicy::isBreached)
                .map(TicketMapper::toDto)
                .toList();

        ReportsSummaryDTO summary = ReportsSummaryDTO.builder()
                .totalAssets(totalAssets)
                .assignedAssets(assignedAssets)
                .assetUtilizationPercent(round1(utilization))
                .avgResolutionDays(round1(avgResolutionDays))
                .activeTickets(activeTickets)
                .maintenanceDueCount(maintenanceDue)
                .maintenanceRatePercent(round1(maintenanceRate))
                .slaBreachedTickets(breachedTickets.size())
                .build();

        return ReportsDashboardDTO.builder()
                .summary(summary)
                .utilizationByCategory(buildCategoryUtilization(assets))
                .breachedTickets(breachedTickets)
                .assetExportCount(totalAssets)
                .ticketExportCount(tickets.size())
                .build();
    }

    @Transactional(readOnly = true)
    public String exportAssetsCsv() {
        List<Asset> assets = assetRepository.findAllWithAssignedUser();
        StringBuilder sb = new StringBuilder();
        sb.append("id,name,type,status,condition,serialNumber,assignedTo\n");
        for (Asset a : assets) {
            sb.append(a.getId()).append(',');
            sb.append(csvEscape(a.getName())).append(',');
            sb.append(csvEscape(a.getType())).append(',');
            sb.append(a.getStatus()).append(',');
            sb.append(a.getConditionState()).append(',');
            sb.append(csvEscape(a.getSerialNumber())).append(',');
            sb.append(csvEscape(a.getAssignedTo() != null ? a.getAssignedTo().getUsername() : "")).append('\n');
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportTicketsCsv() {
        List<Ticket> tickets = ticketRepository.findAllWithDetailsOrderByCreatedAtDesc();
        StringBuilder sb = new StringBuilder();
        sb.append("id,asset,priority,status,raisedBy,technician,createdAt,deadlineAt,resolvedAt,slaBreached,description\n");
        for (Ticket t : tickets) {
            TicketResponseDTO dto = TicketMapper.toDto(t);
            sb.append(dto.getId()).append(',');
            sb.append(csvEscape(dto.getAssetName())).append(',');
            sb.append(dto.getPriority()).append(',');
            sb.append(dto.getStatus()).append(',');
            sb.append(csvEscape(dto.getRaisedByUsername())).append(',');
            sb.append(csvEscape(dto.getTechnicianUsername() != null ? dto.getTechnicianUsername() : "")).append(',');
            sb.append(dto.getCreatedAt()).append(',');
            sb.append(dto.getDeadlineAt()).append(',');
            sb.append(dto.getResolvedAt() != null ? dto.getResolvedAt() : "").append(',');
            sb.append(dto.isSlaBreached()).append(',');
            sb.append(csvEscape(dto.getIssueDescription())).append('\n');
        }
        return sb.toString();
    }

    private boolean isMaintenanceDue(Asset asset) {
        Asset.ConditionState state = asset.getConditionState();
        return state == Asset.ConditionState.REPAIR_NEEDED || state == Asset.ConditionState.DAMAGED;
    }

    private double calculateAvgResolutionDays(List<Ticket> tickets) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Long> resolutionHours = tickets.stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.RESOLVED)
                .filter(t -> t.getResolvedAt() != null && t.getResolvedAt().isAfter(thirtyDaysAgo))
                .filter(t -> t.getCreatedAt() != null)
                .map(t -> ChronoUnit.HOURS.between(t.getCreatedAt(), t.getResolvedAt()))
                .toList();

        if (resolutionHours.isEmpty()) {
            return 0.0;
        }
        double avgHours = resolutionHours.stream().mapToLong(Long::longValue).average().orElse(0.0);
        return avgHours / 24.0;
    }

    private List<CategoryUtilizationDTO> buildCategoryUtilization(List<Asset> assets) {
        Map<String, List<Asset>> byType = assets.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getType() != null ? a.getType() : "OTHER",
                        TreeMap::new,
                        Collectors.toList()));

        return byType.entrySet().stream()
                .map(entry -> {
                    List<Asset> group = entry.getValue();
                    int total = group.size();
                    int assigned = (int) group.stream()
                            .filter(a -> a.getStatus() == Asset.AssetStatus.ASSIGNED)
                            .count();
                    double percent = total == 0 ? 0.0 : (assigned * 100.0) / total;
                    String key = entry.getKey();
                    String label = key.length() <= 1
                            ? key.toUpperCase()
                            : key.substring(0, 1).toUpperCase() + key.substring(1).toLowerCase();
                    return CategoryUtilizationDTO.builder()
                            .category(label)
                            .assigned(assigned)
                            .total(total)
                            .percent(round1(percent))
                            .build();
                })
                .toList();
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
