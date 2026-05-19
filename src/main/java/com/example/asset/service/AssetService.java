package com.example.asset.service;

import com.example.asset.dto.AssetRequestDTO;
import com.example.asset.dto.AssetResponseDTO;
import com.example.asset.entity.Asset.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetService {
    AssetResponseDTO addAsset(AssetRequestDTO request);
    Page<AssetResponseDTO> getAllAssets(String type, AssetStatus status, String search, Pageable pageable);
}