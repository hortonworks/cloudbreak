package com.sequenceiq.freeipa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
public class EnvironmentServiceTest {

    private static final String ENV_CRN = "envCrn";

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private EncryptionProfileEndpoint encryptionProfileEndpoint;

    @InjectMocks
    private EnvironmentService underTest;

    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    void testIsSecretEncryptionEnabled(boolean secretEncryptionEnabled) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        when(detailedEnvironmentResponse.isEnableSecretEncryption()).thenReturn(secretEncryptionEnabled);
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenReturn(detailedEnvironmentResponse);
        boolean result = underTest.isSecretEncryptionEnabled(ENV_CRN);
        assertEquals(secretEncryptionEnabled, result);
    }
}