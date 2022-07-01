package com.sequenceiq.environment.environment.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;

@ExtendWith(MockitoExtension.class)
class EnvironmentInternalV1ControllerTest {

    private static final String ENV_CRN = "envCrn";

    @Mock
    private CredentialService credentialService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentResponseConverter environmentResponseConverter;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator iam;

    @InjectMocks
    private EnvironmentInternalV1Controller underTest;

    public static Stream<Arguments> ccmScenarios() {
        return Stream.of(
                Arguments.of(Tunnel.DIRECT, 0, false),
                Arguments.of(Tunnel.DIRECT, 1, false),
                Arguments.of(Tunnel.DIRECT, 2, false),
                Arguments.of(Tunnel.CLUSTER_PROXY, 0, false),
                Arguments.of(Tunnel.CLUSTER_PROXY, 1, false),
                Arguments.of(Tunnel.CLUSTER_PROXY, 2, false),
                Arguments.of(Tunnel.CCM, 0, true),
                Arguments.of(Tunnel.CCM, 1, true),
                Arguments.of(Tunnel.CCM, 2, true),
                Arguments.of(Tunnel.CCMV2, 0, true),
                Arguments.of(Tunnel.CCMV2, 1, true),
                Arguments.of(Tunnel.CCMV2, 2, true),
                Arguments.of(Tunnel.CCMV2_JUMPGATE, 0, false),
                Arguments.of(Tunnel.CCMV2_JUMPGATE, 1, true),
                Arguments.of(Tunnel.CCMV2_JUMPGATE, 2, true)
        );
    }

    @ParameterizedTest
    @MethodSource("ccmScenarios")
    void isUpgradeCcmAvailable(Tunnel tunnel, int notUpgradedCount, boolean expectedResult) {
        EnvironmentDto env = new EnvironmentDto();
        ExperimentalFeatures experimentalFeatures = new ExperimentalFeatures();
        experimentalFeatures.setTunnel(tunnel);
        env.setExperimentalFeatures(experimentalFeatures);
        when(environmentService.internalGetByCrn(ENV_CRN)).thenReturn(env);
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(iam);
        lenient().when(stackV4Endpoint.getNotCcmUpgradedStackCount(anyLong(), eq(ENV_CRN), any())).thenReturn(notUpgradedCount);
        boolean result = underTest.isUpgradeCcmAvailable(ENV_CRN);
        assertThat(result).isEqualTo(expectedResult);
    }
}
