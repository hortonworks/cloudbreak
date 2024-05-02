package com.sequenceiq.remoteenvironment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationRequests;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationResponses;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequests;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationResponses;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneDeRegistrationRequestsToPrivateControlPlaneDeRegistrationResponsesConverter;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneRegistrationRequestsToPrivateControlPlaneRegistrationResponsesConverter;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;
import com.sequenceiq.remoteenvironment.repository.PrivateControlPlaneRepository;

@ExtendWith(MockitoExtension.class)
class PrivateControlPlaneServiceTest {

    @Mock
    private PrivateControlPlaneRepository privateControlPlaneRepositoryMock;

    @Mock
    private PrivateControlPlaneRegistrationRequestsToPrivateControlPlaneRegistrationResponsesConverter registrationConverterMock;

    @Mock
    private PrivateControlPlaneDeRegistrationRequestsToPrivateControlPlaneDeRegistrationResponsesConverter deRegistrationConverterMock;

    @InjectMocks
    private PrivateControlPlaneService privateControlPlaneService;

    @Test
    void testRegister() {
        PrivateControlPlaneRegistrationRequest requestItem = new PrivateControlPlaneRegistrationRequest();
        requestItem.setCrn("crn:cdp:iam:us-west-1:1234:user:234123");
        PrivateControlPlaneRegistrationRequests request = new PrivateControlPlaneRegistrationRequests();
        request.setItems(Set.of(requestItem));

        PrivateControlPlane privateControlPlane = new PrivateControlPlane();
        privateControlPlane.setAccountId("testAccountId");

        when(privateControlPlaneRepositoryMock.save(any(PrivateControlPlane.class))).thenReturn(privateControlPlane);
        when(registrationConverterMock.convert(eq(request))).thenReturn(new PrivateControlPlaneRegistrationResponses());

        PrivateControlPlaneRegistrationResponses result = privateControlPlaneService.register(request);

        verify(privateControlPlaneRepositoryMock, times(1)).save(any(PrivateControlPlane.class));
        verify(registrationConverterMock, times(1)).convert(eq(request));
        assertEquals(0, result.getItems().size());
    }

    @Test
    void testDeregister() {
        PrivateControlPlaneDeRegistrationRequest requestItem = new PrivateControlPlaneDeRegistrationRequest();
        requestItem.setCrn("crn:cdp:iam:us-west-1:1234:user:234123");
        PrivateControlPlaneDeRegistrationRequests request = new PrivateControlPlaneDeRegistrationRequests();
        request.setItems(Set.of(requestItem));

        when(deRegistrationConverterMock.convert(eq(request))).thenReturn(new PrivateControlPlaneDeRegistrationResponses());

        PrivateControlPlaneDeRegistrationResponses result = privateControlPlaneService.deregister(request);

        verify(privateControlPlaneRepositoryMock, times(1)).deleteByResourceCrns(any(Set.class));
        verify(deRegistrationConverterMock, times(1)).convert(eq(request));
        assertEquals(0, result.getItems().size());
    }

    @Test
    void testGetByPrivateCloudAccountIdAndPublicCloudAccountIdWhenExistsReturnsOptionalWithPrivateControlPlane() {
        String privateCloudAccountId = "privateAccountId";
        String publicCloudAccountId = "publicAccountId";
        PrivateControlPlane expectedControlPlane = new PrivateControlPlane();
        when(privateControlPlaneRepositoryMock.findByPvcAccountAndPbcAccountId(privateCloudAccountId, publicCloudAccountId))
                .thenReturn(Optional.of(expectedControlPlane));

        Optional<PrivateControlPlane> result = privateControlPlaneService
                .getByPrivateCloudAccountIdAndPublicCloudAccountId(privateCloudAccountId, publicCloudAccountId);

        assertTrue(result.isPresent());
        assertEquals(expectedControlPlane, result.get());
    }

    @Test
    void testGetByPrivateCloudAccountIdAndPublicCloudAccountIdWhenNotExistsReturnsEmptyOptional() {
        String privateCloudAccountId = "privateAccountId";
        String publicCloudAccountId = "publicAccountId";
        when(privateControlPlaneRepositoryMock.findByPvcAccountAndPbcAccountId(privateCloudAccountId, publicCloudAccountId))
                .thenReturn(Optional.empty());

        Optional<PrivateControlPlane> result = privateControlPlaneService
                .getByPrivateCloudAccountIdAndPublicCloudAccountId(privateCloudAccountId, publicCloudAccountId);

        assertTrue(result.isEmpty());
    }

}