package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import static org.mockito.ArgumentMatchers.any;
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
class ProxyConfigModificationDatalakeStateActionTest extends ActionTest {

    @Mock
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @InjectMocks
    private ProxyConfigModificationDatalakeStateAction underTest;

    @Mock
    private EnvironmentDto environmentDto;

    private EnvProxyModificationContext context;

    private EnvProxyModificationDefaultEvent payload;

    @BeforeEach
    void setUp() {
        context = new EnvProxyModificationContext(flowParameters, null);
        payload = EnvProxyModificationDefaultEvent.builder()
                .withPreviousProxyConfig(null)
                .withEnvironmentDto(environmentDto)
                .build();
        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any())).thenReturn(environmentDto);
    }

    @Test
    void doExecute() throws Exception {
        underTest.doExecute(context, payload, Map.of());

        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(context, payload,
                EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATALAKE_IN_PROGRESS, ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_ON_DATALAKE_STARTED,
                EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_DATALAKE_STATE);
        String selector = EnvProxyModificationHandlerSelectors.TRACK_DATALAKE_PROXY_MODIFICATION_EVENT.selector();
        verifySendEvent(context, selector,
                new EnvProxyModificationDefaultEvent(selector, environmentDto, payload.getProxyConfig(), payload.getPreviousProxyConfig(), payload.accepted()));
    }

}
