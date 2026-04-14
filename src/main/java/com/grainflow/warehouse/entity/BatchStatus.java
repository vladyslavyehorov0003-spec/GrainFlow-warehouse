package com.grainflow.warehouse.entity;

// Lifecycle stages of a batch contract
public enum BatchStatus {
    PLANNED,   // contract created, loading not yet started
    ACTIVE,    // vehicles are being dispatched and accepted
    CLOSED     // contract fulfilled or cancelled
}
