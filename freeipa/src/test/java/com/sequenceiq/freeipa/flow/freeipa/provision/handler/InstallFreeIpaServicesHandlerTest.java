package com.sequenceiq.freeipa.flow.freeipa.provision.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaInstallService;

@ExtendWith(MockitoExtension.class)
class InstallFreeIpaServicesHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private EventBus eventBus;

    @Mock
    private FreeIpaInstallService freeIpaInstallService;

    @InjectMocks
    private InstallFreeIpaServicesHandler underTest;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAcceptForwardsSkipPreServiceDeploymentRecipesFlag(boolean skipPreServiceDeploymentRecipes) throws Exception {
        underTest.accept(new Event<>(new InstallFreeIpaServicesRequest(STACK_ID, skipPreServiceDeploymentRecipes)));

        verify(freeIpaInstallService).installFreeIpa(STACK_ID, skipPreServiceDeploymentRecipes);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(InstallFreeIpaServicesSuccess.class)), any(Event.class));
    }

    @Test
    void testAcceptSendsFailureWhenInstallFails() throws Exception {
        doThrow(new CloudbreakOrchestratorFailedException("install failed")).when(freeIpaInstallService).installFreeIpa(anyLong(), anyBoolean());

        underTest.accept(new Event<>(new InstallFreeIpaServicesRequest(STACK_ID)));

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(InstallFreeIpaServicesFailed.class)), eventCaptor.capture());
        InstallFreeIpaServicesFailed failure = assertInstanceOf(InstallFreeIpaServicesFailed.class, eventCaptor.getValue().getData());
        assertEquals(STACK_ID, failure.getResourceId());
    }
}
