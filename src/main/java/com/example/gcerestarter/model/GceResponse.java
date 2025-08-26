package com.example.gcerestarter.model;

import java.time.LocalDateTime;

public class GceResponse {
    private boolean success;
    private String message;
    private String instanceName;
    private String originalIp;
    private String newIp;
    private boolean healthCheckPassed;
    private LocalDateTime timestamp;

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getOriginalIp() {
        return originalIp;
    }

    public void setOriginalIp(String originalIp) {
        this.originalIp = originalIp;
    }

    public String getNewIp() {
        return newIp;
    }

    public void setNewIp(String newIp) {
        this.newIp = newIp;
    }

    public boolean isHealthCheckPassed() {
        return healthCheckPassed;
    }

    public void setHealthCheckPassed(boolean healthCheckPassed) {
        this.healthCheckPassed = healthCheckPassed;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
    