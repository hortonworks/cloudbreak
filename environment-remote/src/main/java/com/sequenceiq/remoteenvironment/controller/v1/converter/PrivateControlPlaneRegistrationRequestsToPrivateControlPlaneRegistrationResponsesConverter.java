package com.sequenceiq.remoteenvironment.controller.v1.converter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequests;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationResponse;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationResponses;

@Component
public class PrivateControlPlaneRegistrationRequestsToPrivateControlPlaneRegistrationResponsesConverter {

    public PrivateControlPlaneRegistrationResponses convert(PrivateControlPlaneRegistrationRequests request) {
        Set<PrivateControlPlaneRegistrationResponse> items = new HashSet<>();
        for (PrivateControlPlaneRegistrationRequest item : request.getItems()) {
            PrivateControlPlaneRegistrationResponse response = new PrivateControlPlaneRegistrationResponse();
            response.setCrn(item.getCrn());
            items.add(response);
        }
        PrivateControlPlaneRegistrationResponses responses = new PrivateControlPlaneRegistrationResponses();
        responses.setItems(items);
        return responses;
    }

}
