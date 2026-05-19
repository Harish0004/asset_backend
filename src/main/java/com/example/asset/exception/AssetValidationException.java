package com.example.asset.exception;

import lombok.Getter;
import java.util.Map;

@Getter
public class AssetValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public AssetValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }
}