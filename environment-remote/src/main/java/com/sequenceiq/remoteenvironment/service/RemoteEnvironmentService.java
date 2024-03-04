package com.sequenceiq.remoteenvironment.service;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter;

@Service
public class RemoteEnvironmentService implements ResourceIdProvider, PayloadContextProvider {

    @Inject
    private PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter privateControlPlaneEnvironmentToRemoteEnvironmentConverter;

    public Set<SimpleRemoteEnvironmentResponse> listRemoteEnvironment(int environmentCount) {
        //TODO here comes the list environment logic

        //TODO dummy data for the response
        Set<SimpleRemoteEnvironmentResponse> responseSet = new HashSet<>();
        for (int i = 0; i < environmentCount; i++) {
            privateControlPlaneEnvironmentToRemoteEnvironmentConverter.convert(null, i);
        }
        return responseSet;
    }
}
