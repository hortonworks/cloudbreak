package com.sequenceiq.environment.network.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.Set;
import java.util.UUID;

import jakarta.ws.rs.BadRequestException;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

@ExtendWith(MockitoExtension.class)
class LoadBalancerEntitlementServiceTest {

    private static final String ENV_NAME = "myEnvironment";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private LoadBalancerEntitlementService underTest;

    // @formatter:off
    // CHECKSTYLE:OFF
    public static Object[][] scenarios() {
        return new Object[][] {
            // cloudConstant        AzureGwEnabled GcpGwEnabled  GwSubnets                     valid
            { CloudConstants.AWS,   false,         false,        Set.of("subnet1", "subnet2"), true },
            { CloudConstants.AWS,   false,         false,        Set.of(),                     true },
            { CloudConstants.AWS,   false,         false,        null,                         true },
            { CloudConstants.AZURE, false,         false,        Set.of("subnet1", "subnet2"), false },
            { CloudConstants.AZURE, false,         false,        Set.of(),                     true },
            { CloudConstants.AZURE, false,         false,        null,                         true },
            { CloudConstants.AZURE, true,          false,        Set.of("subnet1", "subnet2"), true },
            { CloudConstants.AZURE, true,          false,        Set.of(),                     true },
            { CloudConstants.AZURE, true,          false,        null,                         true },
            { CloudConstants.GCP,   false,         false,        Set.of("subnet1", "subnet2"), false },
            { CloudConstants.GCP,   false,         false,        Set.of(),                     true },
            { CloudConstants.GCP,   false,         false,        null,                         true },
            { CloudConstants.GCP,   false,         true,         Set.of("subnet1", "subnet2"), true },
            { CloudConstants.GCP,   false,         true,         Set.of(),                     true },
            { CloudConstants.GCP,   false,         true,         null,                         true },
            { CloudConstants.MOCK,  false,         false,        Set.of("subnet1", "subnet2"), false },
            { CloudConstants.MOCK,  false,         false,        Set.of(),                     true },
            { CloudConstants.MOCK,  false,         false,        null,                         true },
            { CloudConstants.YARN,  false,         false,        Set.of("subnet1", "subnet2"), false },
            { CloudConstants.YARN,  false,         false,        Set.of(),                     true },
            { CloudConstants.YARN,  false,         false,        null,                         true },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "provider = {0} azuregw = {1} gcpgw = {2} subnets = {3} valid = {4}")
    @MethodSource("scenarios")
    void testEndpointGatewayEnabled(String cloud, boolean azureGwEnabled, boolean gcpGwEnabled, Set<String> subnets, boolean valid) {
        lenient().when(entitlementService.azureEndpointGatewayEnabled(any())).thenReturn(azureGwEnabled);
        lenient().when(entitlementService.gcpEndpointGatewayEnabled(any())).thenReturn(gcpGwEnabled);

        ThrowingCallable callable = () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.validateNetworkForEndpointGateway(cloud, ENV_NAME, subnets);
        });
        if (valid) {
            assertThatNoException().isThrownBy(callable);
        } else {
            assertThatThrownBy(callable).isInstanceOf(BadRequestException.class);
        }
    }
}
