package com.example.asset.service;

import com.example.asset.dto.AssetAssignmentRequest;
import com.example.asset.dto.AssetReturnRequest;
import com.example.asset.entity.Asset;
import com.example.asset.entity.AssetAssignmentHistory;
import com.example.asset.entity.User;
import com.example.asset.repository.AssetAssignmentHistoryRepository;
import com.example.asset.repository.AssetRepository;
import com.example.asset.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetAssignmentService {

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final AssetAssignmentHistoryRepository historyRepository;

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void assignAsset(AssetAssignmentRequest request, Long adminId) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        if (Asset.AssetStatus.AVAILABLE != asset.getStatus()) {
            throw new IllegalStateException("Asset is not available for assignment");
        }

        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin context invalid"));

        // Update Asset State
        asset.setStatus(Asset.AssetStatus.valueOf("ASSIGNED"));
        asset.setAssignedTo(employee);
        assetRepository.save(asset);

        // Record Transaction Audit Log
        AssetAssignmentHistory history = new AssetAssignmentHistory();
        history.setAsset(asset);
        history.setUser(employee);
        history.setActionByAdmin(admin);
        history.setAssignedAt(LocalDateTime.now());
       history.setDetail("Issued to employee");

        historyRepository.save(history);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void returnAsset(AssetReturnRequest request, Long adminId) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        if (Asset.AssetStatus.ASSIGNED != asset.getStatus()) {
            throw new IllegalStateException("Asset cannot be returned as it is not currently assigned");
        }

        // Fetch active audit record
        AssetAssignmentHistory history = historyRepository.findByAssetIdAndReturnedAtIsNull(request.getAssetId())
                .orElseThrow(() -> new IllegalStateException("No active assignment record found for this asset"));

        // Determine next status based on UI condition selection
        String incomingCondition = request.getConditionState().toUpperCase();
        if ("GOOD".equals(incomingCondition)) {
            asset.setStatus(Asset.AssetStatus.valueOf("AVAILABLE"));
        } else {
            asset.setStatus(Asset.AssetStatus.valueOf("UNDER_MAINTENANCE")); // Lock asset from standard deployment pools
        }

        asset.setConditionState(Asset.ConditionState.valueOf(incomingCondition));
        asset.setAssignedTo(null); // Clear active holder reference
        assetRepository.save(asset);

        // Update History Audit Entry
        history.setReturnedAt(LocalDateTime.now());
      //  history.setDetail("Returned in " + incomingCondition.toLowerCase() + " condition");
        historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<AssetAssignmentHistory> getTransactionHistory() {
        return historyRepository.findAllOrderByLatest();
    }
}
