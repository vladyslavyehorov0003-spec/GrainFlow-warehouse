package com.grainflow.warehouse.entity;

// Result status of a lab analysis for a vehicle
public enum LabStatus {
    PENDING,      // vehicle arrived, analysis not yet started
    IN_PROGRESS,  // samples taken, analysis running
    PASSED,       // grain meets quality requirements
    FAILED,       // grain rejected due to quality
    STORED        // grain moved to silo
}
