package com.sequenceiq.remoteenvironment.controller.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationRequests;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationResponses;

class PrivateControlPlaneDeRegistrationRequestsToPrivateControlPlaneDeRegistrationResponsesConverterTest {

    @Test
    public void testConvert() {
        PrivateControlPlaneDeRegistrationRequests request = new PrivateControlPlaneDeRegistrationRequests();
        PrivateControlPlaneDeRegistrationRequest item1 = new PrivateControlPlaneDeRegistrationRequest();
        item1.setCrn("CRN1");
        PrivateControlPlaneDeRegistrationRequest item2 = new PrivateControlPlaneDeRegistrationRequest();
        item2.setCrn("CRN2");
        request.setItems(Set.of(item1, item2));

        PrivateControlPlaneDeRegistrationRequestsToPrivateControlPlaneDeRegistrationResponsesConverter converter =
                new PrivateControlPlaneDeRegistrationRequestsToPrivateControlPlaneDeRegistrationResponsesConverter();

        PrivateControlPlaneDeRegistrationResponses responses = converter.convert(request);

        assertEquals(2, responses.getItems().size());
        assertTrue(responses.getItems().stream().anyMatch(response -> response.getCrn().equals("CRN1")));
        assertTrue(responses.getItems().stream().anyMatch(response -> response.getCrn().equals("CRN2")));
    }

}