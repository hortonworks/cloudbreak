package com.sequenceiq.remoteenvironment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
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

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private RemoteEnvironmentService remoteEnvironmentService;

    @Test
    public void testListRemoteEnvironmentWhenEntitlementGrantedShouldReturnEnvs() {
        PrivateControlPlane privateControlPlane1 = new PrivateControlPlane();
        privateControlPlane1.setResourceCrn("CRN1");
        privateControlPlane1.setName("NAME1");
        privateControlPlane1.setUrl("URL1");
        privateControlPlane1.setAccountId("ACCOUNT1");
        privateControlPlane1.setId(1L);
        PrivateControlPlane privateControlPlane2 = new PrivateControlPlane();
        privateControlPlane2.setResourceCrn("CRN2");
        privateControlPlane2.setName("NAME2");
        privateControlPlane2.setUrl("URL2");
        privateControlPlane2.setAccountId("ACCOUNT2");
        privateControlPlane2.setId(2L);
        when(privateControlPlaneServiceMock.listByAccountId(anyString()))
                .thenReturn(new HashSet<>(Arrays.asList(privateControlPlane1, privateControlPlane2)));

        RemoteEnvironmentResponse environment1 = new RemoteEnvironmentResponse();
        environment1.setEnvironmentName("NAME1");
        environment1.setCrn("CRN1");
        environment1.setStatus("AVAILABLE");
        environment1.setCloudPlatform("HYBRID");
        RemoteEnvironmentResponse environment2 = new RemoteEnvironmentResponse();
        environment2.setEnvironmentName("NAME2");
        environment2.setCrn("CRN2");
        environment2.setStatus("AVAILABLE");
        environment2.setCloudPlatform("HYBRID");

        RemoteEnvironmentResponses environmentResponses1 = new RemoteEnvironmentResponses();
        environmentResponses1.setEnvironments(Set.of(environment1));

        RemoteEnvironmentResponses environmentResponses2 = new RemoteEnvironmentResponses();
        environmentResponses2.setEnvironments(Set.of(environment2));

        when(clusterProxyHybridClientMock.readConfig(eq("CRN1"))).thenReturn(environmentResponses1);
        when(clusterProxyHybridClientMock.readConfig(eq("CRN2"))).thenReturn(environmentResponses2);

        when(entitlementService.hybridCloudEnabled(anyString())).thenReturn(true);

        SimpleRemoteEnvironmentResponse response1 = new SimpleRemoteEnvironmentResponse();
        response1.setName("NAME1");
        response1.setUrl("URL1");
        response1.setRegion("REGION1");
        response1.setStatus("STATUS1");
        response1.setPrivateControlPlaneName("PVC1");
        response1.setCreated(new Date().getTime());
        response1.setCloudPlatform("HYBRID");
        SimpleRemoteEnvironmentResponse response2 = new SimpleRemoteEnvironmentResponse();
        response2.setName("NAME2");
        response2.setUrl("URL2");
        response2.setRegion("REGION2");
        response2.setStatus("STATUS2");
        response2.setPrivateControlPlaneName("PVC2");
        response2.setCreated(new Date().getTime());
        response2.setCloudPlatform("HYBRID");
        when(converterMock.convert(eq(environment1), eq(privateControlPlane1))).thenReturn(response1);
        when(converterMock.convert(eq(environment2), eq(privateControlPlane2))).thenReturn(response2);

        Set<SimpleRemoteEnvironmentResponse> result = remoteEnvironmentService.listRemoteEnvironment("sampleAccountId");

        assertEquals(2, result.size());
        assertTrue(result.stream()
                .map(e -> e.getName())
                .filter(e -> e.equalsIgnoreCase(response2.getName()))
                .findFirst()
                .isPresent());
        assertTrue(result.stream()
                .map(e -> e.getName())
                .filter(e -> e.equalsIgnoreCase(response1.getName()))
                .findFirst()
                .isPresent());
    }

    @Test
    public void testListRemoteEnvironmentWhenEntitlementNotGrantedShouldThrowBadRequest() {
        when(entitlementService.hybridCloudEnabled(anyString())).thenReturn(false);

        Set<SimpleRemoteEnvironmentResponse> responses = remoteEnvironmentService.listRemoteEnvironment("sampleAccountId");

        assertEquals(responses.size(), 0);
    }
}