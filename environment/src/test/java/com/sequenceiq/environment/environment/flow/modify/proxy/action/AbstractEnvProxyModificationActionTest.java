package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

@ExtendWith(MockitoExtension.class)
class AbstractEnvProxyModificationActionTest {

    private static final Long RESOURCE_ID = 1L;

    private static final String PROXY_CRN = "proxy-crn";

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private DummyEnvProxyModificationAction underTest;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<EnvProxyModificationState, EnvProxyModificationStateSelectors> stateContext;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private EnvProxyModificationDefaultEvent payload;

    @Mock
    private Environment environment;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private ProxyConfig proxyConfig;

    @BeforeEach
    void setUp() {
        lenient().when(stateContext.getExtendedState()).thenReturn(extendedState);
        lenient().when(payload.getResourceId()).thenReturn(RESOURCE_ID);
        lenient().when(payload.getEnvironmentDto()).thenReturn(environmentDto);
        lenient().when(environment.getId()).thenReturn(RESOURCE_ID);
        lenient().when(environment.getProxyConfig()).thenReturn(proxyConfig);
        lenient().when(environmentService.findEnvironmentByIdOrThrow(RESOURCE_ID)).thenReturn(environment);
        lenient().when(proxyConfig.getResourceCrn()).thenReturn(PROXY_CRN);
    }

    @Test
    void createFlowContextWithoutVariable() {
        EnvProxyModificationContext result = underTest.createFlowContext(flowParameters, stateContext, payload);

        assertThat(result)
                .returns(flowParameters, CommonContext::getFlowParameters)
                .returns(proxyConfig, EnvProxyModificationContext::getPreviousProxyConfig);
    }

    @Test
    void createFlowContextWithVariable() {
        ProxyConfig proxyConfig1 = mock(ProxyConfig.class);
        when(extendedState.getVariables()).thenReturn(new HashMap<>(Map.of(AbstractEnvProxyModificationAction.PREVIOUS_PROXY_CONFIG, proxyConfig1)));

        EnvProxyModificationContext result = underTest.createFlowContext(flowParameters, stateContext, payload);

        assertThat(result)
                .returns(flowParameters, CommonContext::getFlowParameters)
                .returns(proxyConfig1, EnvProxyModificationContext::getPreviousProxyConfig);
    }

    @Test
    void getFailurePayload() {
        RuntimeException exception = new RuntimeException("message");

        Object result = underTest.getFailurePayload(payload, Optional.empty(), exception);

        assertThat(result)
                .isInstanceOf(EnvProxyModificationFailedEvent.class)
                .extracting(EnvProxyModificationFailedEvent.class::cast)
                .returns(environmentDto, EnvProxyModificationFailedEvent::getEnvironmentDto)
                .returns(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_FAILED, EnvProxyModificationFailedEvent::getEnvironmentStatus)
                .returns(exception, EnvProxyModificationFailedEvent::getException);
    }

    @Test
    void getProxyConfigCrnWithNull() {
        String result = underTest.getProxyConfigCrn(null);

        assertThat(result).isEqualTo("");
    }

    @Test
    void getProxyConfigCrnWithValue() {
        String result = underTest.getProxyConfigCrn(proxyConfig);

        assertThat(result).isEqualTo(PROXY_CRN);
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