package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors.FAILED_MODIFY_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors.FINISH_MODIFY_SELINUX_CORE_EVENT;
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
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxHandlerEvent;
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
    private CoreModifySeLinuxHandler underTest;

    @Mock
    private Stack stack;

    @Mock
    private StackDto stackDto;

    private CoreModifySeLinuxHandlerEvent event;

    @BeforeEach
    void setUp() {
        event = new CoreModifySeLinuxHandlerEvent(1L, SeLinux.ENFORCING);
    }

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(CoreModifySeLinuxHandlerEvent.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("test"), new Event<>(event));
        assertEquals(FAILED_MODIFY_SELINUX_CORE_EVENT.selector(), response.getSelector());
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
        assertEquals(FINISH_MODIFY_SELINUX_CORE_EVENT.selector(), response.getSelector());
        verify(securityConfigService).updateSeLinuxSecurityConfig(11L, SeLinux.ENFORCING);
        verify(clusterHostServiceRunner).updateClusterConfigs(stackDto, true);
        verify(seLinuxEnablementService).modifySeLinuxOnAllNodes(stack);
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
