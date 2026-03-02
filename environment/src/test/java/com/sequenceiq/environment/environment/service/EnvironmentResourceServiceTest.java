package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.environment.api.v1.environment.model.request.CredentialAwareEnvRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.encryptionprofile.service.EncryptionProfileService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;

@ExtendWith(SpringExtension.class)
class EnvironmentResourceServiceTest {

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockBean
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @MockBean
    private Clock clock;

    @MockBean
    private ProxyConfigService proxyConfigService;

    @MockBean
    private EncryptionProfileService encryptionProfileService;

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
        when(credentialService.getByNameForAccountId(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID), any())).thenReturn(value);
        assertEquals(value, environmentResourceServiceUnderTest.getCredentialFromRequest(request, ACCOUNT_ID));
    }

    @Test
    void getCredentialFromRequestNotFound() {
        request.setCredentialName(ENVIRONMENT_NAME);
        when(credentialService.getByNameForAccountId(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID), any())).thenThrow(NotFoundException.class);
        assertThrows(BadRequestException.class, () -> environmentResourceServiceUnderTest.getCredentialFromRequest(request, ACCOUNT_ID));
    }

    @Test
    void getCredentialFromRequestInproperRequest() {
        request.setCredentialName(null);
        assertThrows(BadRequestException.class, () -> environmentResourceServiceUnderTest.getCredentialFromRequest(request, ACCOUNT_ID));
    }

    @Test
    void createAndSetNetwork() {
        Environment environment = new Environment();
        NetworkDto networkDto = null;
        BaseNetwork network = new AwsNetwork();
        when(networkService.saveNetwork(eq(environment), eq(networkDto), eq(ACCOUNT_ID), any(), any())).thenReturn(network);
        assertEquals(network, environmentResourceServiceUnderTest.createAndSetNetwork(environment, networkDto, ACCOUNT_ID, Map.of(), Map.of()));
    }

    @Test
    void testCreateAndUpdateSshKeyWhenCreatedAndSet() {
        Environment environment = new Environment();
        environment.setName("env-name");
        environment.setResourceCrn("res-crn");
        environment.setCloudPlatform("CP");
        environment.setLocation("location");
        EnvironmentAuthentication authentication = new EnvironmentAuthentication();
        authentication.setPublicKey("ssh-key");
        environment.setAuthentication(authentication);

        CloudConnector connector = mock(CloudConnector.class);
        PublicKeyConnector publicKeyConnector = mock(PublicKeyConnector.class);

        when(cloudPlatformConnectors.get(new CloudPlatformVariant(Platform.platform("CP"), Variant.variant("CP")))).thenReturn(connector);
        when(connector.publicKey()).thenReturn(publicKeyConnector);
        when(credentialToCloudCredentialConverter.convert(environment.getCredential())).thenReturn(mock(CloudCredential.class));
        when(clock.getCurrentTimeMillis()).thenReturn(1L);

        environmentResourceServiceUnderTest.createAndUpdateSshKey(environment);

        assertEquals("env-name-res-crn-1", environment.getAuthentication().getPublicKeyId());
    }

    @Test
    void testCreateAndUpdateSshKeyWhenTooLongWithoutTimestamp() {
        Environment environment = new Environment();
        environment.setName(StringUtils.repeat("*", 200));
        environment.setResourceCrn(StringUtils.repeat("*", 100));
        environment.setCloudPlatform("CP");
        environment.setLocation("location");
        EnvironmentAuthentication authentication = new EnvironmentAuthentication();
        authentication.setPublicKey("ssh-key");
        environment.setAuthentication(authentication);

        CloudConnector connector = mock(CloudConnector.class);
        PublicKeyConnector publicKeyConnector = mock(PublicKeyConnector.class);

        when(cloudPlatformConnectors.get(new CloudPlatformVariant(Platform.platform("CP"), Variant.variant("CP")))).thenReturn(connector);
        when(connector.publicKey()).thenReturn(publicKeyConnector);
        when(credentialToCloudCredentialConverter.convert(environment.getCredential())).thenReturn(mock(CloudCredential.class));
        when(clock.getCurrentTimeMillis()).thenReturn(1L);

        environmentResourceServiceUnderTest.createAndUpdateSshKey(environment);

        String first = StringUtils.repeat("*", 200);
        String second = StringUtils.repeat("*", 54);
        assertEquals(first + "-" + second, environment.getAuthentication().getPublicKeyId());
        // the split size should be equals 2, because we concat the name and crn only
        assertEquals(2, environment.getAuthentication().getPublicKeyId().split("-").length);
    }

    @Test
    void testCreateAndUpdateSshKeyWhenTooLongWithTimestamp() {
        Environment environment = new Environment();
        environment.setName(StringUtils.repeat("*", 200));
        environment.setResourceCrn(StringUtils.repeat("*", 50));
        environment.setCloudPlatform("CP");
        environment.setLocation("location");
        EnvironmentAuthentication authentication = new EnvironmentAuthentication();
        authentication.setPublicKey("ssh-key");
        environment.setAuthentication(authentication);

        CloudConnector connector = mock(CloudConnector.class);
        PublicKeyConnector publicKeyConnector = mock(PublicKeyConnector.class);

        when(cloudPlatformConnectors.get(new CloudPlatformVariant(Platform.platform("CP"), Variant.variant("CP")))).thenReturn(connector);
        when(connector.publicKey()).thenReturn(publicKeyConnector);
        when(credentialToCloudCredentialConverter.convert(environment.getCredential())).thenReturn(mock(CloudCredential.class));
        when(clock.getCurrentTimeMillis()).thenReturn(1234L);

        environmentResourceServiceUnderTest.createAndUpdateSshKey(environment);

        String first = StringUtils.repeat("*", 200);
        String second = StringUtils.repeat("*", 50);
        assertEquals(first + "-" + second + "-123", environment.getAuthentication().getPublicKeyId());
        // the split size should be equals 3, because we concat the name, crn and timestamp
        assertEquals(3, environment.getAuthentication().getPublicKeyId().split("-").length);
    }

    @Test
    void testCreateAndUpdateSshKeyWhenPublicKeyIdExactMatchWith255() {
        Environment environment = new Environment();
        environment.setName(StringUtils.repeat("*", 200));
        environment.setResourceCrn(StringUtils.repeat("*", 50));
        environment.setCloudPlatform("CP");
        environment.setLocation("location");
        EnvironmentAuthentication authentication = new EnvironmentAuthentication();
        authentication.setPublicKey("ssh-key");
        environment.setAuthentication(authentication);

        CloudConnector connector = mock(CloudConnector.class);
        PublicKeyConnector publicKeyConnector = mock(PublicKeyConnector.class);

        when(cloudPlatformConnectors.get(new CloudPlatformVariant(Platform.platform("CP"), Variant.variant("CP")))).thenReturn(connector);
        when(connector.publicKey()).thenReturn(publicKeyConnector);
        when(credentialToCloudCredentialConverter.convert(environment.getCredential())).thenReturn(mock(CloudCredential.class));
        when(clock.getCurrentTimeMillis()).thenReturn(123L);

        environmentResourceServiceUnderTest.createAndUpdateSshKey(environment);

        String first = StringUtils.repeat("*", 200);
        String second = StringUtils.repeat("*", 50);
        assertEquals(first + "-" + second + "-123", environment.getAuthentication().getPublicKeyId());
        // the split size should be equals 3, because we concat the name, crn and timestamp
        assertEquals(3, environment.getAuthentication().getPublicKeyId().split("-").length);
        assertEquals(255, environment.getAuthentication().getPublicKeyId().length());
    }

    @Test
    void testCreateAndUpdateSshKeyWhenDoesNotCreated() {
        Environment environment = new Environment();
        environment.setName("env-name");
        environment.setResourceCrn("res-crn");
        environment.setCloudPlatform("CP");
        environment.setLocation("location");
        EnvironmentAuthentication authentication = new EnvironmentAuthentication();
        authentication.setPublicKey("ssh-key");
        environment.setAuthentication(authentication);

        CloudConnector connector = mock(CloudConnector.class);

        when(cloudPlatformConnectors.get(new CloudPlatformVariant(Platform.platform("CP"), Variant.variant("CP")))).thenReturn(connector);
        when(connector.publicKey()).thenThrow(UnsupportedOperationException.class);
        when(credentialToCloudCredentialConverter.convert(environment.getCredential())).thenReturn(mock(CloudCredential.class));
        when(clock.getCurrentTimeMillis()).thenReturn(1L);

        environmentResourceServiceUnderTest.createAndUpdateSshKey(environment);

        assertNull(environment.getAuthentication().getPublicKeyId());
    }

    @Configuration
    @Import(EnvironmentResourceService.class)
    static class Config {
    }
}
