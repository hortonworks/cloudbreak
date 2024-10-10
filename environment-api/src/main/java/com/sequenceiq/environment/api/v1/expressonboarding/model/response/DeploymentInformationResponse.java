package com.sequenceiq.environment.api.v1.expressonboarding.model.response;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentInformationResponse {

    private String controlPlaneRegion;

    public String getControlPlaneRegion() {
        return controlPlaneRegion;
    }

    public void setControlPlaneRegion(String controlPlaneRegion) {
        this.controlPlaneRegion = controlPlaneRegion;
    }

    @Override
    public String toString() {
        return "DeploymentInformationResponse{" +
                "controlPlaneRegion='" + controlPlaneRegion + '\'' +
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
        DeploymentInformationResponse that = (DeploymentInformationResponse) o;
        return Objects.equals(controlPlaneRegion, that.controlPlaneRegion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(controlPlaneRegion);
    }
}
