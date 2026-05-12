package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.SET_ENCRYPTION_PROFILE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.UPDATE_CM_POLICY_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.encryptionprofile.EncryptionProfileService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class SetEncryptionProfileHandlerTest {

    @Mock
    private StackService stackService;

    @Mock
    private EncryptionProfileService encryptionProfileService;

    @InjectMocks
    private SetEncryptionProfileHandler underTest;

    @Mock
    private Stack stack;

    private UpdateSslConfigEvent event;

    @BeforeEach
    void setUp() {
        event = new UpdateSslConfigEvent(SET_ENCRYPTION_PROFILE_HANDLER_EVENT.name(), 1L, "epCrn");
    }

    @Test
    void testSelector() {
        assertEquals(SET_ENCRYPTION_PROFILE_HANDLER_EVENT.name(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("failed"), new Event<>(event));
        assertEquals(FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.selector(), response.getSelector());
        assertEquals("failed", response.getException().getMessage());
    }

    @Test
    void testSetEncryptionProfileHandlerSuccess() {
        when(stackService.getByIdWithListsInTransaction(event.getResourceId())).thenReturn(stack);


        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(1L, response.getResourceId());
        verify(encryptionProfileService, times(1)).setEncryptionProfile(event.getEncryptionProfileCrn(), stack);
        assertEquals(event.getResourceId(), response.getResourceId());
        assertEquals(UPDATE_CM_POLICY_EVENT.selector(), response.getSelector());
    }

    @Test
    void testSetEncryptionProfileHandlerFailure() {
        when(stackService.getByIdWithListsInTransaction(event.getResourceId())).thenReturn(stack);
        doThrow(new CloudbreakServiceException("failed"))
                .when(encryptionProfileService).setEncryptionProfile(event.getEncryptionProfileCrn(), stack);

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(event.getResourceId(), selectable.getResourceId());
        assertEquals(FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.name(), selectable.selector());
        assertEquals("failed", selectable.getException().getMessage());
    }
}
