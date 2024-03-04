package com.sequenceiq.remoteenvironment.controller.v1.converter;

import java.util.Date;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;

@Component
public class PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter {

    public SimpleRemoteEnvironmentResponse convert(Objects source, int i) {
        SimpleRemoteEnvironmentResponse response = new SimpleRemoteEnvironmentResponse();
        response.setStatus("AVAILABLE");
        response.setCloudPlatform("DATACENTER");
        response.setCreated(new Date().getTime());
        response.setCrn("crn:cdp:remote:1234:1234");
        response.setPrivateControlPlaneName("test-private-control-plane-" + i);
        response.setName("test-remote-environment-" + i);
        response.setRegion("DATACENTER");
        response.setUrl("http://localhost");
        return response;
    }

}
