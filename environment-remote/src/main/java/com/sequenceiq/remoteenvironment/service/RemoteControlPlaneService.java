package com.sequenceiq.remoteenvironment.service;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.RemoteControlPlaneDeRegistrationResponse;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.RemoteControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.RemoteControlPlaneRegistrationResponse;
import com.sequenceiq.remoteenvironment.controller.v1.converter.RemoteControlPlaneDeRegistrationRequestToRemoteControlPlaneDeRegistrationResponseConverter;
import com.sequenceiq.remoteenvironment.controller.v1.converter.RemoteControlPlaneRegistrationRequestToRemoteControlPlaneRegistrationResponseConverter;
import com.sequenceiq.remoteenvironment.repository.PrivateControlPlaneRepository;

@Service
public class RemoteControlPlaneService {

    @Inject
    private PrivateControlPlaneRepository privateControlPlaneRepository;

    @Inject
    private RemoteControlPlaneRegistrationRequestToRemoteControlPlaneRegistrationResponseConverter remoteControlPlaneRegistrationRequestConverter;

    @Inject
    private RemoteControlPlaneDeRegistrationRequestToRemoteControlPlaneDeRegistrationResponseConverter remoteControlPlaneDeRegistrationRequestConverter;

    public RemoteControlPlaneRegistrationResponse register(RemoteControlPlaneRegistrationRequest request) {
        // TODO here comes the registration logic with privateControlPlaneRepository

        return remoteControlPlaneRegistrationRequestConverter.convert(request);
    }

    public RemoteControlPlaneDeRegistrationResponse deregister(String crn) {
        privateControlPlaneRepository.deleteByResourceCrn(crn);
        return remoteControlPlaneDeRegistrationRequestConverter.convert(crn);
    }

}
