package com.sequenceiq.remoteenvironment.controller.v1.converter;

import java.util.Date;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.environments2api.model.EnvironmentSummary;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;

@Component
public class PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter {

    public SimpleRemoteEnvironmentResponse convert(EnvironmentSummary source, PrivateControlPlane privateControlPlane) {
        SimpleRemoteEnvironmentResponse response = new SimpleRemoteEnvironmentResponse();
        response.setStatus(source.getStatus());
        response.setCloudPlatform("PRIVATE_CLOUD");
        response.setCreated(source.getCreated() == null ? new Date().getTime() : source.getCreated().toEpochSecond());
        response.setCrn(source.getCrn());
        response.setEnvironmentCrn(source.getCrn());
        response.setPrivateControlPlaneName(privateControlPlane.getName());
        response.setName(source.getEnvironmentName());
        response.setRegion("PRIVATE_CLOUD");
        response.setUrl(privateControlPlane.getUrl());
        return response;
    }

}
