package com.sequenceiq.environment.environment.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.v2.EnvironmentV2Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.environment.EnvironmentControllerBaseTest;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;

@ExtendWith(MockitoExtension.class)
class EnvironmentV2ControllerTest extends EnvironmentControllerBaseTest {

    @InjectMocks
    private EnvironmentV2Controller underTest;

    @Test
    void testEndpointGatewayOptionsPreserved() {
        EnvironmentNetworkRequest networkRequest = setupNetworkRequestWithEndpointGatway();
        EnvironmentV2Request environmentRequest = new EnvironmentV2Request();
        environmentRequest.setNetwork(networkRequest);

        setupServiceResponses();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.post(environmentRequest));

        assertEquals(PublicEndpointAccessGateway.ENABLED, networkRequest.getPublicEndpointAccessGateway());
        assertEquals(SUBNETS, networkRequest.getEndpointGatewaySubnetIds());
    }

    @Test
    void testUpgradeCcmByNameCallsService() {
        underTest.upgradeCcmByName("name123");
        verify(getMockUpgradeCcmService()).upgradeCcmByName("name123");
    }

    @Test
    void testUpgradeCcmByCrnCallsService() {
        underTest.upgradeCcmByCrn("crn123");
        verify(getMockUpgradeCcmService()).upgradeCcmByCrn("crn123");
    }

    @ParameterizedTest
    @MethodSource("ccmScenarios")
    void isUpgradeCcmAvailable(Tunnel tunnel, int notUpgradedCount, boolean expectedResult) {
        EnvironmentDto env = new EnvironmentDto();
        ExperimentalFeatures experimentalFeatures = new ExperimentalFeatures();
        experimentalFeatures.setTunnel(tunnel);
        env.setExperimentalFeatures(experimentalFeatures);
        when(getMockEnvironmentService().internalGetByCrn(ENV_CRN)).thenReturn(env);
        lenient().when(getMockInternalCrnGeneratorFactory().iam()).thenReturn(getMockIam());
        lenient().when(getMockStackV4Endpoint().getNotCcmUpgradedStackCount(anyLong(), eq(ENV_CRN), any())).thenReturn(notUpgradedCount);
        boolean result = underTest.isUpgradeCcmAvailable(ENV_CRN);
        assertThat(result).isEqualTo(expectedResult);
    }

    @Override
    protected void setupServiceResponses() {
        when(getMockEnvironmentApiConverter().initCreationDto(any(EnvironmentV2Request.class))).thenReturn(EnvironmentCreationDto.builder().build());
        when(getMockEnvironmentCreationService().create(any())).thenReturn(EnvironmentDto.builder().build());
        when(getMockEnvironmentResponseConverter().dtoToDetailedResponse(any())).thenReturn(new DetailedEnvironmentResponse());
    }

}
