package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClouderaManagerRepo {

    private Boolean predefined = Boolean.FALSE;

    private String version;

    private String baseUrl;

    private String gpgKeyUrl;

    private String buildNumber;

    public Boolean getPredefined() {
        return predefined;
    }

    public void setPredefined(Boolean predefined) {
        this.predefined = predefined;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getGpgKeyUrl() {
        return gpgKeyUrl;
    }

    public void setGpgKeyUrl(String gpgKeyUrl) {
        this.gpgKeyUrl = gpgKeyUrl;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    @JsonIgnore
    public String getFullVersion() {
        if (Objects.isNull(buildNumber)) {
            return this.version;
        } else {
            return String.format("%s-%s", this.version, this.buildNumber);
        }
    }

    public ClouderaManagerRepo withPredefined(Boolean predefined) {
        setPredefined(predefined);
        return this;
    }

    public ClouderaManagerRepo withVersion(String version) {
        setVersion(version);
        return this;
    }

    public ClouderaManagerRepo withBaseUrl(String baseUrl) {
        setBaseUrl(baseUrl);
        return this;
    }

    public ClouderaManagerRepo withGpgKeyUrl(String gpgKeyUrl) {
        setGpgKeyUrl(gpgKeyUrl);
        return this;
    }

    public ClouderaManagerRepo withBuildNumber(String buildNumber) {
        setBuildNumber(buildNumber);
        return this;
    }

    @Override
    public String toString() {
        return "ClouderaManagerRepo{" +
                "predefined=" + predefined +
                ", version='" + version + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", gpgKeyUrl='" + gpgKeyUrl + '\'' +
                ", buildNumber='" + buildNumber + '\'' +
                '}';
    }
}
