package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.core.ActionTest;

class ProxyConfigModificationFailedStateActionTest extends ActionTest {

    private static final EnvironmentStatus STATUS = EnvironmentStatus.PROXY_CONFIG_MODIFICATION_FAILED;

    private static final String ENV_CRN = "env-crn";

    private static final String PROXY_CRN = "proxy-crn";

    private static final String EXCEPTION_MESSAGE = "exception-message";

    @Mock
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @InjectMocks
    private ProxyConfigModificationFailedStateAction underTest;

    @Mock
    private EnvProxyModificationContext context;

    @Mock
    private EnvProxyModificationFailedEvent payload;

    @Mock
    private UsageReporter usageReporter;

    @Captor
    private ArgumentCaptor<UsageProto.CDPEnvironmentProxyConfigEditEvent> usageEventCaptor;

    @BeforeEach
    void setUp() {
        context = new EnvProxyModificationContext(flowParameters, null);
        when(payload.getResourceCrn()).thenReturn(ENV_CRN);
        ProxyConfig proxyConfig = mock(ProxyConfig.class);
        when(proxyConfig.getResourceCrn()).thenReturn(PROXY_CRN);
        when(payload.getProxyConfig()).thenReturn(proxyConfig);
        when(payload.getEnvironmentStatus()).thenReturn(STATUS);
        when(payload.getException()).thenReturn(new RuntimeException(EXCEPTION_MESSAGE));
    }

    @Test
    void doExecute() {
        underTest.doExecute(context, payload, Map.of());

        verify(environmentStatusUpdateService).updateFailedEnvironmentStatusAndNotify(context, payload, STATUS,
                ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_FAILED, List.of(EXCEPTION_MESSAGE),
                EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FAILED_STATE);
        verifySendEvent(context, EnvProxyModificationStateSelectors.HANDLE_FAILED_MODIFY_PROXY_EVENT.event(), payload);
        verify(usageReporter).cdpEnvironmentProxyConfigEditEvent(usageEventCaptor.capture());
        Assertions.assertThat(usageEventCaptor.getValue())
                .returns(ENV_CRN, UsageProto.CDPEnvironmentProxyConfigEditEvent::getEnvironmentCrn)
                .returns(PROXY_CRN, UsageProto.CDPEnvironmentProxyConfigEditEvent::getProxyConfigCrn)
                .returns("", UsageProto.CDPEnvironmentProxyConfigEditEvent::getPreviousProxyConfigCrn)
                .returns(UsageProto.CDPEnvironmentProxyConfigEditResult.Value.ENVIRONMENT_FAILURE, UsageProto.CDPEnvironmentProxyConfigEditEvent::getResult)
                .returns(EXCEPTION_MESSAGE, UsageProto.CDPEnvironmentProxyConfigEditEvent::getMessage);
    }

}