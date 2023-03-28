package com.sequenceiq.cloudbreak.common.model;

import java.util.Objects;

public class PackageInfo {

    private String name;

    private String version;

    private String buildNumber;

    public PackageInfo() {
    }

    public PackageInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public PackageInfo(String name, String version, String buildNumber) {
        this.name = name;
        this.version = version;
        this.buildNumber = buildNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getFullVersion() {
        if (Objects.isNull(buildNumber)) {
            return this.version;
        } else {
            return String.format("%s.%s", this.version, this.buildNumber);
        }
    }

    public String getFullVersionPrettyPrinted() {
        if (Objects.isNull(buildNumber)) {
            return this.version;
        } else {
            return String.format("%s-%s", this.version, this.buildNumber);
        }
    }

    public String getPackageNameAndFullVersion() {
        if (Objects.isNull(buildNumber)) {
            return String.format("%s (%s)", this.name, this.version);
        } else {
            return String.format("%s (%s-%s)", this.name, this.version, this.buildNumber);
        }
    }

    public boolean isInvalid() {
        return "false".equals(version) || "null".equals(buildNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PackageInfo that = (PackageInfo) o;
        return Objects.equals(name, that.name) && Objects.equals(version, that.version) && Objects.equals(buildNumber, that.buildNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, buildNumber);
    }

    @Override
    public String toString() {
        return "PackageInfo{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", buildNumber='" + buildNumber + '\'' +
                '}';
    }
}
