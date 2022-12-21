package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import static org.mockito.Mockito.verify;

import java.util.List;
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
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.core.ActionTest;

class ProxyConfigModificationStartStateActionTest extends ActionTest {

    private static final String PROXY_NAME = "proxy-name";

    @Mock
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @InjectMocks
    private ProxyConfigModificationStartStateAction underTest;

    @Mock
    private EnvProxyModificationContext context;

    @Mock
    private EnvProxyModificationDefaultEvent payload;

    @BeforeEach
    void setUp() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName(PROXY_NAME);
        context = new EnvProxyModificationContext(flowParameters, proxyConfig, null);
    }

    @Test
    void doExecute() {
        underTest.doExecute(context, payload, Map.of());

        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(context, payload,
                EnvironmentStatus.PROXY_CONFIG_MODIFICATION_IN_PROGRESS,
                ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_STARTED, List.of(PROXY_NAME),
                EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_START_STATE);

        verifySendEvent(EnvProxyModificationHandlerSelectors.SAVE_NEW_PROXY_ASSOCIATION_HANDLER_EVENT.selector());
    }

}