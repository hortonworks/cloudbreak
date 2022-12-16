package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.ActionTest;

@ExtendWith(MockitoExtension.class)
class ProxyConfigModificationFreeipaStateActionTest extends ActionTest {

    @Mock
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @InjectMocks
    private ProxyConfigModificationFreeipaStateAction underTest;

    @Mock
    private EnvProxyModificationContext context;

    @Mock
    private EnvProxyModificationDefaultEvent payload;

    @Mock
    private EnvironmentDto environmentDto;

    @BeforeEach
    void setUp() {
        super.setUp(context);
        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_IN_PROGRESS, ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_ON_FREEIPA_STARTED,
                EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_START_STATE)).thenReturn(environmentDto);
    }

    @Test
    void doExecute() throws Exception {
        underTest.doExecute(context, payload, Map.of());

        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(context, payload,
                EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_IN_PROGRESS, ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_ON_FREEIPA_STARTED,
                EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_START_STATE);
        String selector = EnvProxyModificationHandlerSelectors.TRACK_FREEIPA_PROXY_MODIFICATION_EVENT.selector();
        verifySendEvent(context, selector, new EnvProxyModificationDefaultEvent(selector, environmentDto, payload.getProxyConfig()));
    }
}
