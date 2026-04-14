package com.grainflow.warehouse.entity;

// Lifecycle of a single truck delivery
public enum VehicleStatus {
    ARRIVED,     // truck arrived at the elevator, waiting to be processed
    IN_PROCESS,  // unloading and lab analysis in progress
    ACCEPTED,    // passed analysis, grain moved to silo
    REJECTED     // failed analysis, grain returned to supplier
}
