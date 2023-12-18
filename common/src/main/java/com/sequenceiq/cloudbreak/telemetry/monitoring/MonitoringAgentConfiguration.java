package com.sequenceiq.cloudbreak.telemetry.monitoring;

public class MonitoringAgentConfiguration {

    private String user;

    private Integer port;

    private String maxDiskUsage;

    private String retentionMinTime;

    private String retentionMaxTime;

    private String walTruncateFrequency;

    private String minBackoff;

    private String maxBackoff;

    private String maxShards;

    private String maxSamplesPerSend;

    private String capacity;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMaxDiskUsage() {
        return maxDiskUsage;
    }

    public void setMaxDiskUsage(String maxDiskUsage) {
        this.maxDiskUsage = maxDiskUsage;
    }

    public String getRetentionMinTime() {
        return retentionMinTime;
    }

    public void setRetentionMinTime(String retentionMinTime) {
        this.retentionMinTime = retentionMinTime;
    }

    public String getRetentionMaxTime() {
        return retentionMaxTime;
    }

    public void setRetentionMaxTime(String retentionMaxTime) {
        this.retentionMaxTime = retentionMaxTime;
    }

    public String getWalTruncateFrequency() {
        return walTruncateFrequency;
    }

    public void setWalTruncateFrequency(String walTruncateFrequency) {
        this.walTruncateFrequency = walTruncateFrequency;
    }

    public String getMinBackoff() {
        return minBackoff;
    }

    public void setMinBackoff(String minBackoff) {
        this.minBackoff = minBackoff;
    }

    public String getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(String maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    public String getMaxShards() {
        return maxShards;
    }

    public void setMaxShards(String maxShards) {
        this.maxShards = maxShards;
    }

    public String getMaxSamplesPerSend() {
        return maxSamplesPerSend;
    }

    public void setMaxSamplesPerSend(String maxSamplesPerSend) {
        this.maxSamplesPerSend = maxSamplesPerSend;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }
}
