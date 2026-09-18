package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors.FAILED_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors.UNSET_ENCRYPTION_PROFILE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors.UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.DisableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class UnsetEncryptionProfileHandlerTest {

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 42L;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private UnsetEncryptionProfileHandler underTest;

    private DisableEncryptionProfileOnClusterEvent event;

    @BeforeEach
    void setUp() {
        event = new DisableEncryptionProfileOnClusterEvent(UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT.name(), STACK_ID);
    }

    @Test
    void testSelector() {
        assertEquals(UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT.selector(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(STACK_ID, new Exception("failed"), new Event<>(event));
        assertEquals(FAILED_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.selector(), response.getSelector());
        assertEquals("failed", response.getException().getMessage());
    }

    @Test
    void testUnsetEncryptionProfileHandlerSuccess() {
        when(clusterService.findClusterIdByStackId(STACK_ID)).thenReturn(Optional.of(CLUSTER_ID));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(clusterService, times(1)).disableEncryptionProfile(CLUSTER_ID);
        assertEquals(STACK_ID, response.getResourceId());
        assertEquals(UNSET_ENCRYPTION_PROFILE_FINISHED_EVENT.selector(), response.getSelector());
    }

    @Test
    void testUnsetEncryptionProfileHandlerClusterNotFound() {
        when(clusterService.findClusterIdByStackId(STACK_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        verify(clusterService, never()).disableEncryptionProfile(anyLong());
    }

    @Test
    void testUnsetEncryptionProfileHandlerFailurePropagatesException() {
        when(clusterService.findClusterIdByStackId(STACK_ID)).thenReturn(Optional.of(CLUSTER_ID));
        doThrow(new CloudbreakServiceException("failed"))
                .when(clusterService).disableEncryptionProfile(CLUSTER_ID);

        CloudbreakServiceException thrown = assertThrows(CloudbreakServiceException.class,
                () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        assertEquals("failed", thrown.getMessage());
    }
}
