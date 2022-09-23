package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStackHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmServiceTest {

    private static final long STACK_ID = 2L;

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENV_CRN = "envCrn";

    @Mock
    private StackService stackService;

    @Mock
    private ClusterProxyService clusterProxyService;

    @Mock
    private FreeIpaStackHealthDetailsService healthService;

    @InjectMocks
    private UpgradeCcmService underTest;

    @BeforeEach
    void setUp() {
        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setEnvironmentCrn(ENV_CRN);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = "AVAILABLE", mode = EXCLUDE)
    void testHealthCheckerUnhealthy(Status healthStatus) {
        HealthDetailsFreeIpaResponse healthDetails = new HealthDetailsFreeIpaResponse();
        healthDetails.setStatus(healthStatus);
        when(healthService.getHealthDetails(ENV_CRN, ACCOUNT_ID)).thenReturn(healthDetails);
        assertThatThrownBy(() -> underTest.registerClusterProxyAndCheckHealth(STACK_ID))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessage("One or more FreeIPA instance is not available. Need to roll back CCM upgrade to previous version.");
    }

    @Test
    void testHealthCheckerHealthy() {
        HealthDetailsFreeIpaResponse healthDetails = new HealthDetailsFreeIpaResponse();
        healthDetails.setStatus(Status.AVAILABLE);
        when(healthService.getHealthDetails(ENV_CRN, ACCOUNT_ID)).thenReturn(healthDetails);
        underTest.registerClusterProxyAndCheckHealth(STACK_ID);
    }

}
