package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.ActionTest;

class ProxyConfigModificationFinishedStateActionTest extends ActionTest {

    @Mock
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @InjectMocks
    private ProxyConfigModificationFinishedStateAction underTest;

    @Mock
    private EnvProxyModificationContext context;

    @Mock
    private EnvProxyModificationDefaultEvent payload;

    @BeforeEach
    void setUp() {
        super.setUp(context);
    }

    @Test
    void doExecute() {
        underTest.doExecute(context, payload, Map.of());

        verifySendEvent(context, EnvProxyModificationStateSelectors.FINALIZE_MODIFY_PROXY_EVENT.selector(), payload);
        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_FINISHED, EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FINISHED_STATE);
    }

}
