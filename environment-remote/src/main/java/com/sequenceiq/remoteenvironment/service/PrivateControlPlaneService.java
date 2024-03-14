package com.sequenceiq.remoteenvironment.service;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneDeRegistrationResponse;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneRegistrationResponse;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneDeRegistrationRequestToPrivateControlPlaneDeRegistrationResponseConverter;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneRegistrationRequestToPrivateControlPlaneRegistrationResponseConverter;
import com.sequenceiq.remoteenvironment.repository.PrivateControlPlaneRepository;

@Service
public class PrivateControlPlaneService {

    @Inject
    private PrivateControlPlaneRepository privateControlPlaneRepository;

    @Inject
    private PrivateControlPlaneRegistrationRequestToPrivateControlPlaneRegistrationResponseConverter privateControlPlaneRegistrationRequestConverter;

    @Inject
    private PrivateControlPlaneDeRegistrationRequestToPrivateControlPlaneDeRegistrationResponseConverter privateControlPlaneDeRegistrationRequestConverter;

    public PrivateControlPlaneRegistrationResponse register(PrivateControlPlaneRegistrationRequest request) {
        // TODO here comes the registration logic with privateControlPlaneRepository

        return privateControlPlaneRegistrationRequestConverter.convert(request);
    }

    public PrivateControlPlaneDeRegistrationResponse deregister(String crn) {
        privateControlPlaneRepository.deleteByResourceCrn(crn);
        return privateControlPlaneDeRegistrationRequestConverter.convert(crn);
    }

}
