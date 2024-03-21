package com.sequenceiq.remoteenvironment.controller.v1.converter;

import java.util.Date;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.remoteenvironment.RemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;

@Component
public class PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter {

    public SimpleRemoteEnvironmentResponse convert(RemoteEnvironmentResponse source, PrivateControlPlane privateControlPlane) {
        SimpleRemoteEnvironmentResponse response = new SimpleRemoteEnvironmentResponse();
        response.setStatus(source.getStatus());
        response.setCloudPlatform(source.getCloudPlatform());
        response.setCreated(new Date().getTime());
        response.setCrn(source.getCrn());
        response.setPrivateControlPlaneName(privateControlPlane.getName());
        response.setName(source.getEnvironmentName());
        response.setRegion("PRIVATE_CONTROL_PLANE");
        response.setUrl(privateControlPlane.getUrl());
        return response;
    }

}
