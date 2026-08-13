package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors.FAILED_ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors.UPDATE_CM_POLICY_HANDLER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class UpdateClouderaManagerPolicyHandlerTest {

    @Mock
    private ClusterBuilderService clusterBuilderService;

    @InjectMocks
    private UpdateClouderaManagerPolicyHandler underTest;

    private EnableEncryptionProfileOnClusterEvent event;

    @BeforeEach
    void setUp() {
        event = new EnableEncryptionProfileOnClusterEvent(UPDATE_CM_POLICY_HANDLER_EVENT.name(), 1L, "epCrn");
    }

    @Test
    void testSelector() {
        assertEquals(UPDATE_CM_POLICY_HANDLER_EVENT.name(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("failed"), new Event<>(event));
        assertEquals(FAILED_ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.selector(), response.getSelector());
        assertEquals("failed", response.getException().getMessage());
    }

    @Test
    void testSetEncryptionProfileHandlerSuccess() {
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(1L, response.getResourceId());
        verify(clusterBuilderService, times(1)).configurePolicy(event.getResourceId());
        assertEquals(event.getResourceId(), response.getResourceId());
        assertEquals(GENERATE_ALTERNATIVE_CERTIFICATE_EVENT.selector(), response.getSelector());
    }

    @Test
    void testSetEncryptionProfileHandlerFailurePropagatesToFramework() {
        doThrow(new CloudbreakServiceException("failed"))
                .when(clusterBuilderService).configurePolicy(event.getResourceId());

        CloudbreakServiceException thrown = assertThrows(CloudbreakServiceException.class,
                () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        assertEquals("failed", thrown.getMessage());
    }
}