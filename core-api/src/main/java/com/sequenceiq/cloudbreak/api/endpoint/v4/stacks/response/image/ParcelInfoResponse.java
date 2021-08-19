package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image;

import java.util.Objects;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class ParcelInfoResponse implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ImageModelDescription.PARCEL_NAME)
    private String name;

    @ApiModelProperty(ModelDescriptions.ImageModelDescription.PARCEL_VERSION)
    private String version;

    @ApiModelProperty(ModelDescriptions.ImageModelDescription.PARCEL_BUILD_NUMBER)
    private String buildNumber;

    public ParcelInfoResponse() {
    }

    public ParcelInfoResponse(String name, String version, String buildNumber) {
        this.name = name;
        this.version = version;
        this.buildNumber = buildNumber;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParcelInfoResponse that = (ParcelInfoResponse) o;
        return name.equals(that.name) && version.equals(that.version) && Objects.equals(buildNumber, that.buildNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, buildNumber);
    }

    @Override
    public String toString() {
        return "ParcelInfoResponse{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", buildNumber='" + buildNumber + '\'' +
                '}';
    }
}
