package com.sequenceiq.common.api.node.status.response;

public class MeteringDetails {

    private DatabusDetails databusDetails;

    private HealthStatus databusReachable;

    private HealthStatus databusTestResponse;

    private EventDetails eventDetails;

    private HealthStatus firstHeartbeatEventTimestamp;

    private HealthStatus heartbeatAgentRunning;

    private HealthStatus heartbeatConfig;

    private HealthStatus heartbeatEventCount;

    private HealthStatus loggingAgentConfig;

    private HealthStatus loggingServiceRunning;

    private Long systemBootTimestamp;

    public DatabusDetails getDatabusDetails() {
        return databusDetails;
    }

    public void setDatabusDetails(DatabusDetails databusDetails) {
        this.databusDetails = databusDetails;
    }

    public HealthStatus getDatabusReachable() {
        return databusReachable;
    }

    public void setDatabusReachable(HealthStatus databusReachable) {
        this.databusReachable = databusReachable;
    }

    public HealthStatus getDatabusTestResponse() {
        return databusTestResponse;
    }

    public void setDatabusTestResponse(HealthStatus databusTestResponse) {
        this.databusTestResponse = databusTestResponse;
    }

    public EventDetails getEventDetails() {
        return eventDetails;
    }

    public void setEventDetails(EventDetails eventDetails) {
        this.eventDetails = eventDetails;
    }

    public HealthStatus getFirstHeartbeatEventTimestamp() {
        return firstHeartbeatEventTimestamp;
    }

    public void setFirstHeartbeatEventTimestamp(HealthStatus firstHeartbeatEventTimestamp) {
        this.firstHeartbeatEventTimestamp = firstHeartbeatEventTimestamp;
    }

    public HealthStatus getHeartbeatAgentRunning() {
        return heartbeatAgentRunning;
    }

    public void setHeartbeatAgentRunning(HealthStatus heartbeatAgentRunning) {
        this.heartbeatAgentRunning = heartbeatAgentRunning;
    }

    public HealthStatus getHeartbeatConfig() {
        return heartbeatConfig;
    }

    public void setHeartbeatConfig(HealthStatus heartbeatConfig) {
        this.heartbeatConfig = heartbeatConfig;
    }

    public HealthStatus getHeartbeatEventCount() {
        return heartbeatEventCount;
    }

    public void setHeartbeatEventCount(HealthStatus heartbeatEventCount) {
        this.heartbeatEventCount = heartbeatEventCount;
    }

    public HealthStatus getLoggingAgentConfig() {
        return loggingAgentConfig;
    }

    public void setLoggingAgentConfig(HealthStatus loggingAgentConfig) {
        this.loggingAgentConfig = loggingAgentConfig;
    }

    public HealthStatus getLoggingServiceRunning() {
        return loggingServiceRunning;
    }

    public void setLoggingServiceRunning(HealthStatus loggingServiceRunning) {
        this.loggingServiceRunning = loggingServiceRunning;
    }

    public Long getSystemBootTimestamp() {
        return systemBootTimestamp;
    }

    public void setSystemBootTimestamp(Long systemBootTimestamp) {
        this.systemBootTimestamp = systemBootTimestamp;
    }

    @Override
    public String toString() {
        return "MeteringDetails{" +
                "databusDetails=" + databusDetails +
                ", databusReachable=" + databusReachable +
                ", databusTestResponse=" + databusTestResponse +
                ", eventDetails=" + eventDetails +
                ", firstHeartbeatEventTimestamp=" + firstHeartbeatEventTimestamp +
                ", heartbeatAgentRunning=" + heartbeatAgentRunning +
                ", heartbeatConfig=" + heartbeatConfig +
                ", heartbeatEventCount=" + heartbeatEventCount +
                ", loggingAgentConfig=" + loggingAgentConfig +
                ", loggingServiceRunning=" + loggingServiceRunning +
                ", systemBootTimestamp=" + systemBootTimestamp +
                '}';
    }
}
