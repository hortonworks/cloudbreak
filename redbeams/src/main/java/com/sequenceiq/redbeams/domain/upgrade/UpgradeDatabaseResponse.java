package com.sequenceiq.redbeams.domain.upgrade;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class UpgradeDatabaseResponse {

    private String reason;

    private FlowIdentifier flowIdentifier;

    private MajorVersion currentVersion;

    public UpgradeDatabaseResponse() {
    }

    public UpgradeDatabaseResponse(MajorVersion currentVersion) {
        this(null, null, currentVersion);
    }

    public UpgradeDatabaseResponse(String reason, MajorVersion currentVersion) {
        this(reason, null, currentVersion);
    }

    public UpgradeDatabaseResponse(FlowIdentifier flowIdentifier, MajorVersion currentVersion) {
        this(null, flowIdentifier, currentVersion);
    }

    public UpgradeDatabaseResponse(String reason, FlowIdentifier flowIdentifier, MajorVersion currentVersion) {
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
        this.currentVersion = currentVersion;
    }

    public MajorVersion getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(MajorVersion currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseResponse{" +
                "reason='" + reason + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                ", currentVersion=" + currentVersion +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpgradeDatabaseResponse that = (UpgradeDatabaseResponse) o;
        return Objects.equals(reason, that.reason) && Objects.equals(flowIdentifier, that.flowIdentifier) && currentVersion == that.currentVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reason, flowIdentifier, currentVersion);
    }
}
