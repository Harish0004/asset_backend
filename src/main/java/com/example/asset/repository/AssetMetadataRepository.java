package com.example.asset.repository;

import com.example.asset.entity.AssetMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetMetadataRepository extends JpaRepository<AssetMetadata, Long> {
    List<AssetMetadata> findByAssetId(Long assetId);
}