package com.sequenceiq.cloudbreak.node.status.response;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdpDoctorMeteringStatusResponse {

    private CdpDoctorCheckStatus heartbeatAgentRunning = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus heartbeatConfig = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus loggingServiceRunning = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus loggingAgentConfig = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus databusReachable = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus databusTestResponse = CdpDoctorCheckStatus.UNKNOWN;

    public CdpDoctorCheckStatus getHeartbeatAgentRunning() {
        return heartbeatAgentRunning;
    }

    public void setHeartbeatAgentRunning(CdpDoctorCheckStatus heartbeatAgentRunning) {
        if (heartbeatAgentRunning != null) {
            this.heartbeatAgentRunning = heartbeatAgentRunning;
        }
    }

    public CdpDoctorCheckStatus getHeartbeatConfig() {
        return heartbeatConfig;
    }

    public void setHeartbeatConfig(CdpDoctorCheckStatus heartbeatConfig) {
        if (heartbeatConfig != null) {
            this.heartbeatConfig = heartbeatConfig;
        }
    }

    public CdpDoctorCheckStatus getLoggingServiceRunning() {
        return loggingServiceRunning;
    }

    public void setLoggingServiceRunning(CdpDoctorCheckStatus loggingServiceRunning) {
        if (loggingServiceRunning != null) {
            this.loggingServiceRunning = loggingServiceRunning;
        }
    }

    public CdpDoctorCheckStatus getLoggingAgentConfig() {
        return loggingAgentConfig;
    }

    public void setLoggingAgentConfig(CdpDoctorCheckStatus loggingAgentConfig) {
        if (loggingAgentConfig != null) {
            this.loggingAgentConfig = loggingAgentConfig;
        }
    }

    public CdpDoctorCheckStatus getDatabusReachable() {
        return databusReachable;
    }

    public void setDatabusReachable(CdpDoctorCheckStatus databusReachable) {
        if (databusReachable != null) {
            this.databusReachable = databusReachable;
        }
    }

    public CdpDoctorCheckStatus getDatabusTestResponse() {
        return databusTestResponse;
    }

    public void setDatabusTestResponse(CdpDoctorCheckStatus databusTestResponse) {
        if (databusTestResponse != null) {
            this.databusTestResponse = databusTestResponse;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpDoctorMeteringStatusResponse that = (CdpDoctorMeteringStatusResponse) o;
        return getHeartbeatAgentRunning() == that.getHeartbeatAgentRunning() &&
                getHeartbeatConfig() == that.getHeartbeatConfig() &&
                getLoggingServiceRunning() == that.getLoggingServiceRunning() &&
                getLoggingAgentConfig() == that.getLoggingAgentConfig() &&
                getDatabusReachable() == that.getDatabusReachable() &&
                getDatabusTestResponse() == that.getDatabusTestResponse();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHeartbeatAgentRunning(), getHeartbeatConfig(), getLoggingServiceRunning(),
                getLoggingAgentConfig(), getDatabusReachable(), getDatabusTestResponse());
    }

    @Override
    public String toString() {
        return "CdpDoctorMeteringStatusResponse{" +
                "heartbeatAgentRunning=" + heartbeatAgentRunning +
                ", heartbeatConfig=" + heartbeatConfig +
                ", loggingServiceRunning=" + loggingServiceRunning +
                ", loggingAgentConfig=" + loggingAgentConfig +
                ", databusReachable=" + databusReachable +
                ", databusTestResponse=" + databusTestResponse +
                '}';
    }
}
