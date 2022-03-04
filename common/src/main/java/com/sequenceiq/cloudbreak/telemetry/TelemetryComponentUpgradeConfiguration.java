package com.sequenceiq.cloudbreak.telemetry;

public class TelemetryComponentUpgradeConfiguration {

    private String desiredVersion;

    private String desiredDate;

    public String getDesiredVersion() {
        return desiredVersion;
    }

    public void setDesiredVersion(String desiredVersion) {
        this.desiredVersion = desiredVersion;
    }

    public String getDesiredDate() {
        return desiredDate;
    }

    public void setDesiredDate(String desiredDate) {
        this.desiredDate = desiredDate;
    }
}
