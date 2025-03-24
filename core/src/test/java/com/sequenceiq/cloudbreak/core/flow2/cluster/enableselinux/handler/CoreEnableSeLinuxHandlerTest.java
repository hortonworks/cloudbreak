package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.FINISH_ENABLE_SELINUX_CORE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxHandlerEvent;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.SeLinuxEnablementService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class CoreEnableSeLinuxHandlerTest {

    @Mock
    private StackService stackService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private SeLinuxEnablementService seLinuxEnablementService;

    @InjectMocks
    private CoreEnableSeLinuxHandler underTest;

    @Mock
    private Stack stack;

    @Mock
    private StackDto stackDto;

    private CoreEnableSeLinuxHandlerEvent event;

    @BeforeEach
    void setUp() {
        event = new CoreEnableSeLinuxHandlerEvent(1L);
    }

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(CoreEnableSeLinuxHandlerEvent.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("test"), new Event<>(event));
        assertEquals(FAILED_ENABLE_SELINUX_CORE_EVENT.selector(), response.getSelector());
        assertEquals("test", response.getException().getMessage());
    }

    @Test
    void testFreeIpaEnableSeLinuxHandlerSuccess() throws CloudbreakOrchestratorException {
        when(stack.getId()).thenReturn(1L);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        securityConfig.setId(11L);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(stackService.getStackProxyById(1L)).thenReturn(stackDto);
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        assertEquals(1L, response.getResourceId());
        assertEquals(FINISH_ENABLE_SELINUX_CORE_EVENT.selector(), response.getSelector());
        verify(securityConfigService).updateSeLinuxSecurityConfig(11L, SeLinux.ENFORCING);
        verify(clusterHostServiceRunner).updateClusterConfigs(stackDto, true);
        verify(seLinuxEnablementService).enableSeLinuxOnAllNodes(stack);
    }

    @Test
    void testFreeIpaEnableSeLinuxHandlerFailure() {
        when(stack.getId()).thenReturn(1L);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        securityConfig.setId(11L);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(stackService.getStackProxyById(1L)).thenReturn(stackDto);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        doThrow(new CloudbreakRuntimeException("test")).when(clusterHostServiceRunner).updateClusterConfigs(stackDto, true);
        CloudbreakRuntimeException exception = assertThrows(CloudbreakRuntimeException.class,
                () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        assertEquals("test", exception.getMessage());
        verify(securityConfigService).updateSeLinuxSecurityConfig(11L, SeLinux.ENFORCING);
        verify(clusterHostServiceRunner).updateClusterConfigs(stackDto, true);
        verifyNoInteractions(seLinuxEnablementService);
    }
}
