package com.sequenceiq.environment.expressonboarding.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.api.v1.expressonboarding.model.response.DeploymentInformationResponse;

@Service
public class DeploymentInformationResponseConverter {

    @Value("${crn.region:}")
    private String region;

    public DeploymentInformationResponse deploymentInformationResponse() {
        DeploymentInformationResponse deploymentInformationResponse = new DeploymentInformationResponse();
        deploymentInformationResponse.setControlPlaneRegion(region);
        return deploymentInformationResponse;
    }
}
