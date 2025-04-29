package com.sequenceiq.freeipa.flow.freeipa.enableselinux.handler;

import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.FAILED_MODIFY_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.FINISH_MODIFY_SELINUX_FREEIPA_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxHandlerEvent;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaOrchestrationConfigService;
import com.sequenceiq.freeipa.service.stack.SeLinuxModificationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaModifySeLinuxHandlerTest {

    private FreeIpaModifySeLinuxHandlerEvent event;

    @Mock
    private StackService stackService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private FreeIpaOrchestrationConfigService freeIpaOrchestrationConfigService;

    @Mock
    private SeLinuxModificationService seLinuxEnablementService;

    @InjectMocks
    private FreeIpaModifySeLinuxHandler underTest;

    @Mock
    private Stack stack;

    @BeforeEach
    void setUp() {
        String selector = EventSelectorUtil.selector(FreeIpaModifySeLinuxHandler.class);
        event = new FreeIpaModifySeLinuxHandlerEvent(1L, "test-op", SeLinux.ENFORCING);
    }

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(FreeIpaModifySeLinuxHandlerEvent.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("test"), new Event<>(event));
        assertEquals(FAILED_MODIFY_SELINUX_FREEIPA_EVENT.selector(), response.getSelector());
        assertEquals("test", response.getException().getMessage());
    }

    @Test
    void testFreeIpaEnableSeLinuxHandlerSuccess() throws CloudbreakOrchestratorException {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        securityConfig.setId(11L);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        assertEquals(1L, response.getResourceId());
        assertEquals(FINISH_MODIFY_SELINUX_FREEIPA_EVENT.selector(), response.getSelector());
        verify(securityConfigService).updateSeLinuxSecurityConfig(11L, SeLinux.ENFORCING);
        verify(freeIpaOrchestrationConfigService).configureOrchestrator(1L);
        verify(seLinuxEnablementService).modifySeLinuxOnAllNodes(stack);
    }

    @Test
    void testFreeIpaEnableSeLinuxHandlerFailure() throws CloudbreakOrchestratorException {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        securityConfig.setId(11L);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        doThrow(new CloudbreakOrchestratorFailedException("test")).when(freeIpaOrchestrationConfigService).configureOrchestrator(1L);
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        assertEquals(1L, response.getResourceId());
        assertEquals(FAILED_MODIFY_SELINUX_FREEIPA_EVENT.selector(), response.getSelector());
        assertEquals("test", response.getException().getMessage());
        verify(securityConfigService).updateSeLinuxSecurityConfig(11L, SeLinux.ENFORCING);
        verify(freeIpaOrchestrationConfigService).configureOrchestrator(1L);
        verifyNoInteractions(seLinuxEnablementService);
    }
}
