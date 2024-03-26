package com.sequenceiq.remoteenvironment.controller.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.clusterproxy.remoteenvironment.RemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;

class PrivateControlPlaneEnvironmentToRemoteEnvironmentConverterTest {

    @Test
    public void testConvert() {
        RemoteEnvironmentResponse source = new RemoteEnvironmentResponse();
        source.setStatus("ACTIVE");
        source.setCloudPlatform("AWS");
        source.setCrn("SAMPLE_CRN");
        source.setEnvironmentName("Sample Environment");

        PrivateControlPlane privateControlPlane = new PrivateControlPlane();
        privateControlPlane.setName("Test Control Plane");
        privateControlPlane.setUrl("https://example.com");

        PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter converter = new PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter();

        SimpleRemoteEnvironmentResponse response = converter.convert(source, privateControlPlane);

        assertEquals("ACTIVE", response.getStatus());
        assertEquals("PRIVATE_CLOUD", response.getCloudPlatform());
        assertNotNull(response.getCreated());
        assertEquals("SAMPLE_CRN", response.getCrn());
        assertEquals("Test Control Plane", response.getPrivateControlPlaneName());
        assertEquals("Sample Environment", response.getName());
        assertEquals("PRIVATE_CLOUD", response.getRegion());
        assertEquals("https://example.com", response.getUrl());
    }
}