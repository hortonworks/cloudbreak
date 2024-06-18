package com.sequenceiq.cloudbreak.service.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
public class EnvironmentConfigProviderTest {

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @InjectMocks
    private EnvironmentConfigProvider underTest;

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {true, false})
    void testIsSecretEncryptionEnabled(boolean secretEncryptionEnabled) {
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.isEnableSecretEncryption()).thenReturn(secretEncryptionEnabled);
        boolean result = underTest.isSecretEncryptionEnabled(ENVIRONMENT_CRN);
        assertEquals(secretEncryptionEnabled, result);
    }
}
