package com.example.gcerestarter.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

public class GceRequest {

    @NotBlank(message = "项目ID不能为空")
    private String projectId;
    @NotBlank(message = "项目ID不能为空")
    private String instanceId;

    @NotBlank(message = "区域不能为空")
    private String zone;

    @NotBlank(message = "实例名称不能为空")
    private String instanceName;

    @Positive(message = "健康检查端口必须为正数")
    private int healthCheckPort = 22; // 默认检查SSH端口

    @Positive(message = "最大重试次数必须为正数")
    private int maxRetries = 5;

    @PositiveOrZero(message = "初始等待时间不能为负数")
    private int initialWaitSeconds = 30;

    @Positive(message = "重试延迟必须为正数")
    private int retryDelaySeconds = 10;

    // Getters and Setters
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public int getHealthCheckPort() {
        return healthCheckPort;
    }

    public void setHealthCheckPort(int healthCheckPort) {
        this.healthCheckPort = healthCheckPort;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getInitialWaitSeconds() {
        return initialWaitSeconds;
    }

    public void setInitialWaitSeconds(int initialWaitSeconds) {
        this.initialWaitSeconds = initialWaitSeconds;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public String toString() {
        return "GceRequest{" +
                "projectId='" + projectId + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", zone='" + zone + '\'' +
                ", instanceName='" + instanceName + '\'' +
                ", healthCheckPort=" + healthCheckPort +
                ", maxRetries=" + maxRetries +
                ", initialWaitSeconds=" + initialWaitSeconds +
                ", retryDelaySeconds=" + retryDelaySeconds +
                '}';
    }
}
    