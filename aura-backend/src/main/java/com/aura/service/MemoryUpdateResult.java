package com.aura.service;

public record MemoryUpdateResult(String memoryJson, boolean updated) {
    public static MemoryUpdateResult noChange(String memoryJson) {
        return new MemoryUpdateResult(memoryJson, false);
    }

    public static MemoryUpdateResult updated(String memoryJson) {
        return new MemoryUpdateResult(memoryJson, true);
    }
}
