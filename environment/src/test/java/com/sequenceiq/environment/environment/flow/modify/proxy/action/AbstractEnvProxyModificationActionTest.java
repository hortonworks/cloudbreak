package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

@ExtendWith(MockitoExtension.class)
class AbstractEnvProxyModificationActionTest {

    private static final Long RESOURCE_ID = 1L;

    private static final String ENV_CRN = "env-crn";

    private static final String PROXY_CRN = "proxy-crn";

    private static final String PREVIOUS_PROXY_CRN = "prev-proxy-crn";

    @Mock
    private ProxyConfigService proxyConfigService;

    @InjectMocks
    private DummyEnvProxyModificationAction underTest;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<EnvProxyModificationState, EnvProxyModificationStateSelectors> stateContext;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private ProxyConfig proxyConfig;

    @Mock
    private ProxyConfig previousProxyConfig;

    private EnvProxyModificationDefaultEvent payload;

    @BeforeEach
    void setUp() {
        payload = EnvProxyModificationDefaultEvent.builder()
                .withResourceId(RESOURCE_ID)
                .withResourceName("env")
                .withResourceCrn(ENV_CRN)
                .withProxyConfigCrn(PROXY_CRN)
                .withPreviousProxyConfigCrn(PREVIOUS_PROXY_CRN)
                .build();
        lenient().when(stateContext.getExtendedState()).thenReturn(extendedState);
        lenient().when(proxyConfig.getResourceCrn()).thenReturn(PROXY_CRN);
        lenient().when(previousProxyConfig.getResourceCrn()).thenReturn(PREVIOUS_PROXY_CRN);
        lenient().when(proxyConfigService.getByCrn(PROXY_CRN)).thenReturn(proxyConfig);
        lenient().when(proxyConfigService.getOptionalByEnvironmentCrn(ENV_CRN)).thenReturn(Optional.of(previousProxyConfig));
    }

    @Test
    void createFlowContextWithoutVariable() {
        EnvProxyModificationContext result = underTest.createFlowContext(flowParameters, stateContext, payload);

        assertThat(result)
                .returns(flowParameters, CommonContext::getFlowParameters)
                .returns(proxyConfig, EnvProxyModificationContext::getProxyConfig)
                .returns(previousProxyConfig, EnvProxyModificationContext::getPreviousProxyConfig);
    }

    @Test
    void createFlowContextWithoutVariableNoProxy() {
        payload = EnvProxyModificationDefaultEvent.builder()
                .withResourceId(RESOURCE_ID)
                .withResourceName("env")
                .withResourceCrn(ENV_CRN)
                .withProxyConfigCrn(null)
                .withPreviousProxyConfigCrn(PREVIOUS_PROXY_CRN)
                .build();
        EnvProxyModificationContext result = underTest.createFlowContext(flowParameters, stateContext, payload);

        assertThat(result)
                .returns(flowParameters, CommonContext::getFlowParameters)
                .returns(null, EnvProxyModificationContext::getProxyConfig)
                .returns(previousProxyConfig, EnvProxyModificationContext::getPreviousProxyConfig);
        verify(proxyConfigService, never()).getByCrn(any());
    }

    @Test
    void createFlowContextWithoutVariableNoPreviousProxy() {
        when(proxyConfigService.getOptionalByEnvironmentCrn(ENV_CRN)).thenReturn(Optional.empty());
        EnvProxyModificationContext result = underTest.createFlowContext(flowParameters, stateContext, payload);

        assertThat(result)
                .returns(flowParameters, CommonContext::getFlowParameters)
                .returns(proxyConfig, EnvProxyModificationContext::getProxyConfig)
                .returns(null, EnvProxyModificationContext::getPreviousProxyConfig);
    }

    @Test
    void createFlowContextWithVariable() {
        ProxyConfig proxyConfig = mock(ProxyConfig.class);
        when(extendedState.getVariables())
                .thenReturn(new HashMap<>(Map.of(AbstractEnvProxyModificationAction.PREVIOUS_PROXY_CONFIG, proxyConfig)));

        EnvProxyModificationContext result = underTest.createFlowContext(flowParameters, stateContext, payload);

        assertThat(result)
                .returns(flowParameters, CommonContext::getFlowParameters)
                .returns(proxyConfig, EnvProxyModificationContext::getPreviousProxyConfig);
    }

    @Test
    void getFailurePayload() {
        RuntimeException exception = new RuntimeException("message");

        Object result = underTest.getFailurePayload(payload, Optional.empty(), exception);

        assertThat(result)
                .isInstanceOf(EnvProxyModificationFailedEvent.class)
                .extracting(EnvProxyModificationFailedEvent.class::cast)
                .returns(ENV_CRN, EnvProxyModificationFailedEvent::getResourceCrn)
                .returns(payload.getResourceId(), EnvProxyModificationFailedEvent::getResourceId)
                .returns(payload.getResourceName(), EnvProxyModificationFailedEvent::getResourceName)
                .returns(PROXY_CRN, EnvProxyModificationFailedEvent::getProxyConfigCrn)
                .returns(PREVIOUS_PROXY_CRN, EnvProxyModificationFailedEvent::getPreviousProxyConfigCrn)
                .returns(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_FAILED, EnvProxyModificationFailedEvent::getEnvironmentStatus)
                .returns(exception, EnvProxyModificationFailedEvent::getException);
    }

    private static class DummyEnvProxyModificationAction extends AbstractEnvProxyModificationAction<EnvProxyModificationDefaultEvent> {

        protected DummyEnvProxyModificationAction() {
            super(EnvProxyModificationDefaultEvent.class);
        }

        @Override
        protected void doExecute(EnvProxyModificationContext context, EnvProxyModificationDefaultEvent payload, Map<Object, Object> variables) {
            // do nothing
        }
    }
}