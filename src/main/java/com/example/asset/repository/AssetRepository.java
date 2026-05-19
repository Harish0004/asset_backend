package com.example.asset.repository;

import com.example.asset.entity.Asset;
import com.example.asset.entity.Asset.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    boolean existsBySerialNumber(String serialNumber);

    Optional<Asset> findBySerialNumber(String serialNumber);

    @Query("SELECT a FROM Asset a WHERE " +
            "(:type IS NULL OR LOWER(a.type) = LOWER(:type)) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.serialNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Asset> findAssetsWithFilters(
            @Param("type") String type,
            @Param("status") AssetStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}