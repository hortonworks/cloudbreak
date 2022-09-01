package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradePushSaltStatesRequest;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeOrchestratorService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class PushSaltStatesHandlerTest {

    private static final Long STACK_ID = 42L;

    @Mock
    private UpgradeOrchestratorService upgradeOrchestratorService;

    @Mock
    private HandlerEvent<ValidateRdsUpgradePushSaltStatesRequest> event;

    @InjectMocks
    private PushSaltStatesHandler underTest;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("VALIDATERDSUPGRADEPUSHSALTSTATESREQUEST");
    }

    @Test
    void doAccept() {
        ValidateRdsUpgradePushSaltStatesRequest request = new ValidateRdsUpgradePushSaltStatesRequest(STACK_ID);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);
        verify(upgradeOrchestratorService).pushSaltState(STACK_ID);
        assertThat(result.selector()).isEqualTo("VALIDATERDSUPGRADEPUSHSALTSTATESRESULT");
    }
}