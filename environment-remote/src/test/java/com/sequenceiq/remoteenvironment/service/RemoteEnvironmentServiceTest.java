package com.sequenceiq.remoteenvironment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyHybridClient;
import com.sequenceiq.cloudbreak.clusterproxy.remoteenvironment.RemoteEnvironmentResponse;
import com.sequenceiq.cloudbreak.clusterproxy.remoteenvironment.RemoteEnvironmentResponses;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;

@ExtendWith(MockitoExtension.class)
class RemoteEnvironmentServiceTest {

    @Mock
    private PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter converterMock;

    @Mock
    private PrivateControlPlaneService privateControlPlaneServiceMock;

    @Mock
    private ClusterProxyHybridClient clusterProxyHybridClientMock;

    @InjectMocks
    private RemoteEnvironmentService remoteEnvironmentService;

    @Test
    public void testListRemoteEnvironment() {
        PrivateControlPlane privateControlPlane1 = new PrivateControlPlane();
        privateControlPlane1.setResourceCrn("CRN1");
        PrivateControlPlane privateControlPlane2 = new PrivateControlPlane();
        privateControlPlane2.setResourceCrn("CRN2");
        when(privateControlPlaneServiceMock.listByAccountId(anyString()))
                .thenReturn(new HashSet<>(Arrays.asList(privateControlPlane1, privateControlPlane2)));

        RemoteEnvironmentResponse environment1 = new RemoteEnvironmentResponse();
        RemoteEnvironmentResponse environment2 = new RemoteEnvironmentResponse();

        RemoteEnvironmentResponses environmentResponses1 = new RemoteEnvironmentResponses();
        environmentResponses1.setEnvironments(Set.of(environment1));

        RemoteEnvironmentResponses environmentResponses2 = new RemoteEnvironmentResponses();
        environmentResponses2.setEnvironments(Set.of(environment2));

        when(clusterProxyHybridClientMock.readConfig(eq("CRN1"))).thenReturn(environmentResponses1);
        when(clusterProxyHybridClientMock.readConfig(eq("CRN2"))).thenReturn(environmentResponses2);

        SimpleRemoteEnvironmentResponse response1 = new SimpleRemoteEnvironmentResponse();
        response1.setName("NAME1");
        SimpleRemoteEnvironmentResponse response2 = new SimpleRemoteEnvironmentResponse();
        response2.setName("NAME2");
        when(converterMock.convert(eq(environment1), eq(privateControlPlane1))).thenReturn(response1);
        when(converterMock.convert(eq(environment2), eq(privateControlPlane2))).thenReturn(response2);

        Set<SimpleRemoteEnvironmentResponse> result = remoteEnvironmentService.listRemoteEnvironment("sampleAccountId");

        assertEquals(2, result.size());
        assertTrue(result.contains(response1));
        assertTrue(result.contains(response2));
    }
}