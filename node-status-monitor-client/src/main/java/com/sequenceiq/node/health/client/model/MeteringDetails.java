package com.sequenceiq.node.health.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.node.health.client.model.deserialize.DatabusDetailsDeserializer;
import com.sequenceiq.node.health.client.model.deserialize.EventDetailsDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MeteringDetails {

    @JsonDeserialize(using = DatabusDetailsDeserializer.class)
    private DatabusDetails databusDetails;

    private HealthStatus databusReachable;

    private HealthStatus databusTestResponse;

    @JsonDeserialize(using = EventDetailsDeserializer.class)
    private EventDetails eventDetails;

    private HealthStatus heartbeatAgentRunning;

    private HealthStatus heartbeatConfig;

    private HealthStatus loggingAgentConfig;

    private HealthStatus loggingServiceRunning;

    private Long systemBootTimestamp;

    private Long firstHeartbeatEventTimestamp;

    private Integer heartbeatEventCount;

    private Long timestamp;

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

    public Integer getHeartbeatEventCount() {
        return heartbeatEventCount;
    }

    public void setHeartbeatEventCount(Integer heartbeatEventCount) {
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

    public Long getFirstHeartbeatEventTimestamp() {
        return firstHeartbeatEventTimestamp;
    }

    public void setFirstHeartbeatEventTimestamp(Long firstHeartbeatEventTimestamp) {
        this.firstHeartbeatEventTimestamp = firstHeartbeatEventTimestamp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
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
                ", timestamp=" + timestamp +
                '}';
    }
}
