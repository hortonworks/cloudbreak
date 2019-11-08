package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.util.TestConstants.USER;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.environment.api.v1.environment.model.request.CredentialAwareEnvRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(SpringExtension.class)
class EnvironmentResourceServiceTest {

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private NetworkService networkService;

    @Inject
    private EnvironmentResourceService environmentResourceServiceUnderTest;

    private CredentialAwareEnvRequest request;

    @BeforeEach
    void setup() {
        request = new EnvironmentRequest();
        request.setCredentialName(ENVIRONMENT_NAME);
    }

    @Test
    void getCredentialFromRequest() {
        Credential value = new Credential();
        when(credentialService.getByNameForAccountId(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(value);
        assertEquals(value, environmentResourceServiceUnderTest.getCredentialFromRequest(request, ACCOUNT_ID, USER));
    }

    @Test
    void getCredentialFromRequestNotFound() {
        request.setCredentialName(ENVIRONMENT_NAME);
        when(credentialService.getByNameForAccountId(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenThrow(NotFoundException.class);
        assertThrows(BadRequestException.class, () -> environmentResourceServiceUnderTest.getCredentialFromRequest(request, ACCOUNT_ID, USER));
    }

    @Test
    void getCredentialFromRequestInproperRequest() {
        request.setCredentialName(null);
        assertThrows(BadRequestException.class, () -> environmentResourceServiceUnderTest.getCredentialFromRequest(request, ACCOUNT_ID, USER));
    }

    @Test
    void createAndSetNetwork() {
        Environment environment = new Environment();
        NetworkDto networkDto = null;
        BaseNetwork network = new AwsNetwork();
        when(networkService.saveNetwork(eq(environment), eq(networkDto), eq(ACCOUNT_ID), any())).thenReturn(network);
        assertEquals(network, environmentResourceServiceUnderTest.createAndSetNetwork(environment, networkDto, ACCOUNT_ID, Map.of()));
    }

    @Configuration
    @Import(EnvironmentResourceService.class)
    static class Config {
    }
}