package com.sequenceiq.cloudbreak.reactor.handler.kerberos;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationRequest;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.bus.Event;
import reactor.bus.EventBus;

import java.util.Base64;
import java.util.Optional;

import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KeytabConfigurationHandlerTest {

    private static final Long STACK_ID = 123L;

    private static final String STACK_NAME = "name";

    private static final String CLOUD_PLATFORM = "cloud platform";

    private static final String ENVIRONMENT_CRN = "crn";

    private static final byte[] KEYTAB = new byte[] {1, 2, 3, 4, 5, 6};

    private static final String KEYTABS_IN_BASE64 = Base64.getEncoder().encodeToString(KEYTAB);

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
    public void shouldUpdateKeytabs() throws Exception {
        KeytabConfigurationRequest keytabConfigurationRequest = new KeytabConfigurationRequest(STACK_ID);
        Stack stack = aStack();
        Optional<KerberosConfig> kerberosConfig = of(mock(KerberosConfig.class));
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(true);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, true, kerberosConfig)).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(keytabProvider.getServiceKeytabResponse(stack, gatewayConfig)).thenReturn(mock(ServiceKeytabResponse.class));
        when(secretService.getByResponse(any())).thenReturn(KEYTABS_IN_BASE64);

        victim.accept(new Event<>(keytabConfigurationRequest));

        verify(hostOrchestrator).uploadKeytabs(any(), any(), any());
        verify(eventBus).notify(anyString(), any(Event.class));
    }

    @Test
    public void shouldNotUpdateKeytabs() throws Exception {
        KeytabConfigurationRequest keytabConfigurationRequest = new KeytabConfigurationRequest(STACK_ID);
        Stack stack = mock(Stack.class);
        Optional<KerberosConfig> kerberosConfig = of(mock(KerberosConfig.class));

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(true);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, true, kerberosConfig)).thenReturn(false);

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
