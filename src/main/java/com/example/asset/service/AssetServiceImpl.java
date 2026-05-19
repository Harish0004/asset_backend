package com.example.asset.service;

import com.example.asset.dto.AssetRequestDTO;
import com.example.asset.dto.AssetResponseDTO;
import com.example.asset.entity.Asset;
import com.example.asset.entity.AssetMetadata;
import com.example.asset.exception.AssetValidationException;
import com.example.asset.exception.ResourceAlreadyExistsException;
import com.example.asset.repository.AssetMetadataRepository;
import com.example.asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final AssetMetadataRepository metadataRepository;

    @Override
    @Transactional
    @CacheEvict(value = "assetsCache", allEntries = true)
    public AssetResponseDTO addAsset(AssetRequestDTO request) {
        // 1. Check for global unique Serial Number constraint
        if (assetRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new ResourceAlreadyExistsException("Asset with Serial Number '" + request.getSerialNumber() + "' already exists");
        }

        // 2. Execute Dynamic Category Type Field Validation Rules
        validateAssetSpecificMetadata(request);

        // 3. Map core request fields to Asset Entity
        Asset asset = Asset.builder()
                .name(request.getName())
                .type(request.getType().toUpperCase())
                .status(Asset.AssetStatus.AVAILABLE)
                .conditionState(Asset.ConditionState.GOOD)
                .serialNumber(request.getSerialNumber())
                .build();

        Asset savedAsset = assetRepository.save(asset);

        // 4. Extract and loop properties map into EAV metadata collection
        Map<String, String> responseMetadata = new HashMap<>();
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            request.getMetadata().forEach((key, value) -> {
                if (value != null && !value.trim().isEmpty()) {
                    AssetMetadata meta = AssetMetadata.builder()
                            .asset(savedAsset)
                            .attributeKey(key.toLowerCase())
                            .attributeValue(value.trim())
                            .build();
                    metadataRepository.save(meta);
                    responseMetadata.put(key.toLowerCase(), value.trim());
                }
            });
        }

        return mapToResponseDTO(savedAsset, responseMetadata);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "assetsCache", key = "{#type, #status, #search, #pageable.pageNumber, #pageable.pageSize}")
    public Page<AssetResponseDTO> getAllAssets(String type, Asset.AssetStatus status, String search, Pageable pageable) {
        Page<Asset> assetPage = assetRepository.findAssetsWithFilters(type, status, search, pageable);

        return assetPage.map(asset -> {
            List<AssetMetadata> metadataList = metadataRepository.findByAssetId(asset.getId());
            Map<String, String> metaMap = metadataList.stream()
                    .collect(Collectors.toMap(AssetMetadata::getAttributeKey, AssetMetadata::getAttributeValue));
            return mapToResponseDTO(asset, metaMap);
        });
    }

    private void validateAssetSpecificMetadata(AssetRequestDTO request) {
        String assetType = request.getType().toUpperCase();
        Map<String, String> metadata = request.getMetadata();
        Map<String, String> errors = new LinkedHashMap<>();

        if ("LAPTOP".equals(assetType)) {
            if (metadata == null || !metadata.containsKey("ram") || metadata.get("ram").trim().isEmpty()) {
                errors.put("metadata.ram", "RAM configuration is strictly required for Laptops");
            }
            if (metadata == null || !metadata.containsKey("storage") || metadata.get("storage").trim().isEmpty()) {
                errors.put("metadata.storage", "Storage size configuration is strictly required for Laptops");
            }
            if (metadata == null || !metadata.containsKey("screensize") || metadata.get("screensize").trim().isEmpty()) {
                errors.put("metadata.screensize", "Screen size display dimension is strictly required for Laptops");
            }
        }

        if (!errors.isEmpty()) {
            throw new AssetValidationException("Dynamic attribute schema check failed for type: " + assetType, errors);
        }
    }

    private AssetResponseDTO mapToResponseDTO(Asset asset, Map<String, String> metadata) {
        return AssetResponseDTO.builder()
                .id(asset.getId())
                .name(asset.getName())
                .type(asset.getType())
                .status(asset.getStatus())
                .conditionState(asset.getConditionState())
                .serialNumber(asset.getSerialNumber())
                .assignedToUsername(asset.getAssignedTo() != null ? asset.getAssignedTo().getUsername() : null)
                .metadata(metadata)
                .createdAt(asset.getCreatedAt())
                .build();
    }
}