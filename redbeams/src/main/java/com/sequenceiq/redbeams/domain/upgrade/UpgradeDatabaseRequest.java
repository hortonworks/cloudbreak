package com.sequenceiq.redbeams.domain.upgrade;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class UpgradeDatabaseRequest {

    private TargetMajorVersion targetMajorVersion;

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }

    public void setTargetMajorVersion(TargetMajorVersion targetMajorVersion) {
        this.targetMajorVersion = targetMajorVersion;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseRequest{" +
                "targetMajorVersion=" + targetMajorVersion +
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
        UpgradeDatabaseRequest that = (UpgradeDatabaseRequest) o;
        return targetMajorVersion == that.targetMajorVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetMajorVersion);
    }
}
