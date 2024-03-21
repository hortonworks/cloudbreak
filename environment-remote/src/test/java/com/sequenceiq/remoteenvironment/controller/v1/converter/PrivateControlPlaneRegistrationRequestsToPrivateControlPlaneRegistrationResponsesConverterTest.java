package com.sequenceiq.remoteenvironment.controller.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequests;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationResponses;

class PrivateControlPlaneRegistrationRequestsToPrivateControlPlaneRegistrationResponsesConverterTest {

    @Test
    public void testConvert() {
        PrivateControlPlaneRegistrationRequests request = new PrivateControlPlaneRegistrationRequests();
        PrivateControlPlaneRegistrationRequest item1 = new PrivateControlPlaneRegistrationRequest();
        item1.setCrn("CRN1");
        PrivateControlPlaneRegistrationRequest item2 = new PrivateControlPlaneRegistrationRequest();
        item2.setCrn("CRN2");
        request.setItems(Set.of(item1, item2));

        PrivateControlPlaneRegistrationRequestsToPrivateControlPlaneRegistrationResponsesConverter converter =
                new PrivateControlPlaneRegistrationRequestsToPrivateControlPlaneRegistrationResponsesConverter();

        PrivateControlPlaneRegistrationResponses responses = converter.convert(request);

        assertEquals(2, responses.getItems().size());
        assertTrue(responses.getItems().stream().anyMatch(response -> response.getCrn().equals("CRN1")));
        assertTrue(responses.getItems().stream().anyMatch(response -> response.getCrn().equals("CRN2")));
    }

}