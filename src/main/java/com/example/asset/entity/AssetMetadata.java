package com.example.asset.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "asset_metadata",
        uniqueConstraints = @UniqueConstraint(columnNames = {"asset_id", "attribute_key"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "attribute_key", nullable = false, length = 100)
    private String attributeKey;

    @Column(name = "attribute_value", nullable = false, length = 255)
    private String attributeValue;
}
