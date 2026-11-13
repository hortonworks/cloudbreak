package com.sequenceiq.freeipa.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.service.stack.FreeIpaUpgradeDefaultOutboundService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaInternalV1ControllerEnvironmentCrnTest {

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:acc-1:freeipa:fi-1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";

    @Mock
    private FreeIpaUpgradeDefaultOutboundService upgradeDefaultOutboundService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private FreeIpaInternalV1Controller controller;

    @Test
    void getEnvironmentCrnByResourceCrnReturnsParentEnvironment() {
        when(stackService.getEnvironmentCrnByResourceCrn(FREEIPA_CRN)).thenReturn(ENV_CRN);

        assertThat(controller.getEnvironmentCrnByResourceCrn(FREEIPA_CRN).getEnvironmentCrn()).isEqualTo(ENV_CRN);
    }
}
