package com.sequenceiq.cloudbreak.reactor.handler.kerberos;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationRequest;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;

@ExtendWith(MockitoExtension.class)
class KeytabConfigurationHandlerTest {

    private static final Long STACK_ID = 123L;

    private static final String STACK_NAME = "name";

    private static final String CLOUD_PLATFORM = "cloud platform";

    private static final String ENVIRONMENT_CRN = "crn";

    private static final byte[] KEYTAB = new byte[] {1, 2, 3, 4, 5, 6};

    private static final String KEYTABS_IN_BASE64 = Base64Util.encode(KEYTAB);

    @Mock
    private StackService stackService;

    @Mock
    private EventBus eventBus;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private KerberosDetailService kerberosDetailService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private SecretService secretService;

    @Mock
    private KeytabProvider keytabProvider;

    @Mock
    private EnvironmentConfigProvider environmentConfigProvider;

    @InjectMocks
    private KeytabConfigurationHandler victim;

    @Test
    void shouldUpdateKeytabs() throws Exception {
        KeytabConfigurationRequest keytabConfigurationRequest = new KeytabConfigurationRequest(STACK_ID, Boolean.FALSE);
        Stack stack = aStack();
        Optional<KerberosConfig> kerberosConfig = of(mock(KerberosConfig.class));
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(true);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, true, kerberosConfig)).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(List.of(gatewayConfig));
        when(keytabProvider.getServiceKeytabResponse(stack, gatewayConfig, false)).thenReturn(mock(ServiceKeytabResponse.class));
        when(secretService.getByResponse(any())).thenReturn(KEYTABS_IN_BASE64);

        victim.accept(new Event<>(keytabConfigurationRequest));

        verify(hostOrchestrator).uploadKeytabs(any(), any(), any());
        verify(eventBus).notify(anyString(), any(Event.class));
    }

    @Test
    void shouldUploadKeytabsToAllGatewaysInHa() throws Exception {
        KeytabConfigurationRequest keytabConfigurationRequest = new KeytabConfigurationRequest(STACK_ID, Boolean.FALSE);
        Stack stack = aStack();
        Optional<KerberosConfig> kerberosConfig = of(mock(KerberosConfig.class));
        GatewayConfig primaryGatewayConfig = mock(GatewayConfig.class);
        GatewayConfig secondaryGatewayConfig = mock(GatewayConfig.class);
        List<GatewayConfig> allGatewayConfigs = List.of(primaryGatewayConfig, secondaryGatewayConfig);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(true);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, true, kerberosConfig)).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(primaryGatewayConfig);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);
        when(keytabProvider.getServiceKeytabResponse(stack, primaryGatewayConfig, false)).thenReturn(mock(ServiceKeytabResponse.class));
        when(secretService.getByResponse(any())).thenReturn(KEYTABS_IN_BASE64);

        victim.accept(new Event<>(keytabConfigurationRequest));

        ArgumentCaptor<List<GatewayConfig>> gatewayConfigsCaptor = ArgumentCaptor.forClass(List.class);
        verify(hostOrchestrator).uploadKeytabs(gatewayConfigsCaptor.capture(), any(), any());
        assertThat(gatewayConfigsCaptor.getValue()).containsExactlyInAnyOrder(primaryGatewayConfig, secondaryGatewayConfig);
    }

    @Test
    void shouldNotifyFailureWhenKeytabUploadThrows() throws Exception {
        KeytabConfigurationRequest keytabConfigurationRequest = new KeytabConfigurationRequest(STACK_ID, Boolean.FALSE);
        Stack stack = aStack();
        Optional<KerberosConfig> kerberosConfig = of(mock(KerberosConfig.class));
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(true);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, true, kerberosConfig)).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(List.of(gatewayConfig));
        when(keytabProvider.getServiceKeytabResponse(stack, gatewayConfig, false)).thenReturn(mock(ServiceKeytabResponse.class));
        when(secretService.getByResponse(any())).thenReturn(KEYTABS_IN_BASE64);
        doThrow(new CloudbreakOrchestratorFailedException("keytab upload failed"))
                .when(hostOrchestrator).uploadKeytabs(any(), any(), any());

        victim.accept(new Event<>(keytabConfigurationRequest));

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(anyString(), eventCaptor.capture());
        Object payload = eventCaptor.getValue().getData();
        assertThat(payload).isInstanceOf(KeytabConfigurationFailed.class);
        KeytabConfigurationFailed failed = (KeytabConfigurationFailed) payload;
        assertThat(failed.getResourceId()).isEqualTo(STACK_ID);
        assertThat(failed.getException().getMessage()).contains("keytab upload failed");
    }

    @Test
    void shouldNotUpdateKeytabs() throws Exception {
        KeytabConfigurationRequest keytabConfigurationRequest = new KeytabConfigurationRequest(STACK_ID, Boolean.FALSE);
        Stack stack = mock(Stack.class);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        victim.accept(new Event<>(keytabConfigurationRequest));

        verifyNoMoreInteractions(hostOrchestrator);
        verify(eventBus).notify(anyString(), any(Event.class));
    }

    private Stack aStack() {
        Stack stack = new Stack();
        stack.setCloudPlatform(CLOUD_PLATFORM);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setName(STACK_NAME);
        stack.setCluster(mock(Cluster.class));

        return stack;
    }
}
