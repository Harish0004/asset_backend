package com.example.asset.repository;



import com.example.asset.entity.AssetAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetAssignmentHistoryRepository extends JpaRepository<AssetAssignmentHistory, Long> {

    // Finds the active assignment record to process a return
    Optional<AssetAssignmentHistory> findByAssetIdAndReturnedAtIsNull(Long assetId);

    // Fetches history sorted by latest timestamp to populate the Historical Audit Trail grid
    @Query("SELECT h FROM AssetAssignmentHistory h ORDER BY h.assignedAt DESC")
    List<AssetAssignmentHistory> findAllOrderByLatest();
}