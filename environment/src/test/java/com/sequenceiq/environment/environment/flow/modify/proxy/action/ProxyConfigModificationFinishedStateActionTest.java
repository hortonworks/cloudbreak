package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.core.ActionTest;

class ProxyConfigModificationFinishedStateActionTest extends ActionTest {

    private static final String ENV_CRN = "env-crn";

    private static final String PROXY_CRN = "proxy-crn";

    @Mock
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @Mock
    private UsageReporter usageReporter;

    @InjectMocks
    private ProxyConfigModificationFinishedStateAction underTest;

    @Mock
    private EnvProxyModificationContext context;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private ProxyConfig proxyConfig;

    @Mock
    private ProxyConfig previousProxyConfig;

    @Captor
    private ArgumentCaptor<UsageProto.CDPEnvironmentProxyConfigEditEvent> usageEventCaptor;

    private EnvProxyModificationDefaultEvent payload;

    @BeforeEach
    void setUp() {
        super.setUp(context);
        when(environmentDto.getResourceCrn()).thenReturn(ENV_CRN);
        when(proxyConfig.getResourceCrn()).thenReturn(PROXY_CRN);
        payload = EnvProxyModificationDefaultEvent.builder()
                .withSelector("selector")
                .withEnvironmentDto(environmentDto)
                .withProxyConfig(proxyConfig)
                .withPreviousProxyConfig(previousProxyConfig)
                .build();
    }

    @Test
    void doExecute() {
        underTest.doExecute(context, payload, Map.of());

        verifySendEvent(context, EnvProxyModificationStateSelectors.FINALIZE_MODIFY_PROXY_EVENT.selector(), payload);
        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_FINISHED, EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FINISHED_STATE);
        verify(usageReporter).cdpEnvironmentProxyConfigEditEvent(usageEventCaptor.capture());
        Assertions.assertThat(usageEventCaptor.getValue())
                .returns(ENV_CRN, UsageProto.CDPEnvironmentProxyConfigEditEvent::getEnvironmentCrn)
                .returns(PROXY_CRN, UsageProto.CDPEnvironmentProxyConfigEditEvent::getProxyConfigCrn)
                .returns("", UsageProto.CDPEnvironmentProxyConfigEditEvent::getPreviousProxyConfigCrn)
                .returns(UsageProto.CDPEnvironmentProxyConfigEditResult.Value.SUCCESS, UsageProto.CDPEnvironmentProxyConfigEditEvent::getResult);
    }

}
