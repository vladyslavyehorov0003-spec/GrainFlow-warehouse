package com.grainflow.warehouse.entity;

// Result status of a lab analysis for a vehicle
public enum LabStatus {
    PENDING,        // created automatically when vehicle starts processing, not yet started by lab
    IN_PROGRESS,    // samples taken, measurements in progress
    ANALYSIS_DONE,  // moisture/impurity/protein recorded; drying is next
    DRYING,         // grain sent to dryer
    DRYING_DONE,    // drying finished, results recorded
    STORED,          // grain moved to a silo (terminal state)
    CANCELED
}
