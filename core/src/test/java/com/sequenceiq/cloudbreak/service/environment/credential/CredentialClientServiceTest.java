package com.sequenceiq.cloudbreak.service.environment.credential;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

@ExtendWith(MockitoExtension.class)
class CredentialClientServiceTest {

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private CredentialEndpoint credentialEndpoint;

    @Mock
    private CredentialConverter credentialConverter;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Spy
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @InjectMocks
    private CredentialClientService underTest;

    @Test
    void testGetCloudCredential() {
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("internalCrn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        CredentialResponse credentialResponse = new CredentialResponse();
        when(credentialEndpoint.getByEnvironmentCrn(eq(ENVIRONMENT_CRN))).thenReturn(credentialResponse);
        Credential credential = Credential.builder().crn("crn").name("name").account("account").attributes(new Json(new HashMap<>())).build();
        when(credentialConverter.convert(eq(credentialResponse))).thenReturn(credential);

        CloudCredential cloudCredential = underTest.getCloudCredential(ENVIRONMENT_CRN);

        verify(regionAwareInternalCrnGeneratorFactory, times(1)).iam();
        verify(regionAwareInternalCrnGenerator, times(1)).getInternalCrnForServiceAsString();
        verify(credentialEndpoint, times(1)).getByEnvironmentCrn(eq(ENVIRONMENT_CRN));
        Assertions.assertEquals("crn", cloudCredential.getId());
        Assertions.assertEquals("name", cloudCredential.getName());
        Assertions.assertEquals("account", cloudCredential.getAccountId());
    }

}