package com.sequenceiq.cloudbreak.sdx.pdl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;

@ExtendWith(MockitoExtension.class)
class AbstractPdlSdxServiceTest {

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private RemoteEnvironmentEndpoint remoteEnvironmentEndpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private MockPdlSdxService underTest;

    @Mock
    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @Mock
    private DescribeEnvironmentResponse describeResponse;

    @Mock
    private Environment environment;

    @BeforeEach
    void setUp() {
        when(environmentEndpoint.getByCrn(any())).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getRemoteEnvironmentCrn()).thenReturn("remoteCrn");
        lenient().when(describeResponse.getEnvironment()).thenReturn(environment);
        lenient().when(remoteEnvironmentEndpoint.getByCrn(any())).thenReturn(describeResponse);
    }

    @Test
    void getPrivateEnvForPublicEnv() {
        Environment result = underTest.getPrivateEnvForPublicEnv("publicEnvCrn");

        assertThat(result).isEqualTo(environment);
    }

    @Test
    void getPrivateEnvForPublicEnvException() {
        RuntimeException exception = new RuntimeException("exception");
        when(remoteEnvironmentEndpoint.getByCrn(any())).thenThrow(exception);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(exception)).thenReturn("extractedMessage");

        assertThatThrownBy(() -> underTest.getPrivateEnvForPublicEnv("publicEnvCrn"))
                .hasMessage("Failed to get PDL by crn remoteCrn: extractedMessage");
    }

    static class MockPdlSdxService extends AbstractPdlSdxService {

    }

}
