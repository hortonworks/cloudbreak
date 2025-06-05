package com.sequenceiq.cloudbreak.service.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
public class EnvironmentConfigProviderTest {

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @InjectMocks
    private EnvironmentConfigProvider underTest;

    @Test
    void testGetEnvironmentByCrn() {
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(detailedEnvironmentResponse);
        DetailedEnvironmentResponse result = underTest.getEnvironmentByCrn(ENVIRONMENT_CRN);
        assertEquals(detailedEnvironmentResponse, result);
    }
}
