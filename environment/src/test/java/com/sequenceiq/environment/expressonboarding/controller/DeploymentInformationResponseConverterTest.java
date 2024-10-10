package com.sequenceiq.environment.expressonboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.client.util.Value;
import com.sequenceiq.environment.api.v1.expressonboarding.model.response.DeploymentInformationResponse;

@SpringJUnitConfig
class DeploymentInformationResponseConverterTest {

    @InjectMocks
    private DeploymentInformationResponseConverter converter;

    @Value("${crn.region:us-east-1}")
    private String region = "us-west-2";

    @BeforeEach
    public void setUp() {
        converter = new DeploymentInformationResponseConverter();
        ReflectionTestUtils.setField(converter, "region", region);
    }

    @Test
    public void testDeploymentInformationResponse() {
        // Act
        DeploymentInformationResponse response = converter.deploymentInformationResponse();

        // Assert
        assertEquals(region, response.getControlPlaneRegion());
    }
}