package com.sequenceiq.remoteenvironment.controller.v1.converter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationRequests;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationResponse;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationResponses;

@Component
public class PrivateControlPlaneDeRegistrationRequestsToPrivateControlPlaneDeRegistrationResponsesConverter {

    public PrivateControlPlaneDeRegistrationResponses convert(PrivateControlPlaneDeRegistrationRequests request) {
        Set<PrivateControlPlaneDeRegistrationResponse> items = new HashSet<>();
        for (PrivateControlPlaneDeRegistrationRequest item : request.getItems()) {
            PrivateControlPlaneDeRegistrationResponse response = new PrivateControlPlaneDeRegistrationResponse();
            response.setCrn(item.getCrn());
            items.add(response);
        }
        PrivateControlPlaneDeRegistrationResponses responses = new PrivateControlPlaneDeRegistrationResponses();
        responses.setItems(items);
        return responses;
    }

}
