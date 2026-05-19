package com.example.asset.controller;

import com.example.asset.dto.AssetRequestDTO;
import com.example.asset.dto.AssetResponseDTO;
import com.example.asset.entity.Asset.AssetStatus;
import com.example.asset.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetResponseDTO> addAsset(@Valid @RequestBody AssetRequestDTO requestDTO) {
        AssetResponseDTO savedAsset = assetService.addAsset(requestDTO);
        return new ResponseEntity<>(savedAsset, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<AssetResponseDTO>> getAllAssets(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        // Handle incoming comma separated sorting properties (e.g. name,asc)
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<AssetResponseDTO> assets = assetService.getAllAssets(type, status, search, pageable);

        return ResponseEntity.ok(assets);
    }
}