package com.aura.error;

import lombok.Getter;

@Getter
public class AuraException extends RuntimeException {
    private final AuraErrorCode code;

    public AuraException(AuraErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
