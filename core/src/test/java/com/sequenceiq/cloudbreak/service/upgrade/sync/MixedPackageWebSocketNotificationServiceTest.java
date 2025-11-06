package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
class MixedPackageWebSocketNotificationServiceTest {

    private static final long STACK_ID = 1L;

    private static final ResourceEvent RESOURCE_EVENT = ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED;

    private static final List<String> ARGS = List.of("args");

    @InjectMocks
    private MixedPackageNotificationService underTest;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private StackStatusService stackStatusService;

    @Test
    void testSendNotificationShouldSendUpdateInProgressEvent() {
        when(stackStatusService.findFirstByStackIdOrderByCreatedDesc(STACK_ID)).thenReturn(createStackStatus(UPDATE_IN_PROGRESS));

        underTest.sendNotification(STACK_ID, RESOURCE_EVENT, ARGS);

        verify(eventService).fireCloudbreakEvent(STACK_ID, UPDATE_IN_PROGRESS.name(), RESOURCE_EVENT, ARGS);
    }

    @Test
    void testSendNotificationShouldSendUpdateInProgressEventWhenTheStackStatusIsNotPresent() {
        when(stackStatusService.findFirstByStackIdOrderByCreatedDesc(STACK_ID)).thenReturn(Optional.empty());

        underTest.sendNotification(STACK_ID, RESOURCE_EVENT, ARGS);

        verify(eventService).fireCloudbreakEvent(STACK_ID, UPDATE_IN_PROGRESS.name(), RESOURCE_EVENT, ARGS);
    }

    @Test
    void testSendNotificationShouldSendUpdateFailedEventWhenTheStackStatusUpdateFailed() {
        when(stackStatusService.findFirstByStackIdOrderByCreatedDesc(STACK_ID)).thenReturn(createStackStatus(UPDATE_FAILED));

        underTest.sendNotification(STACK_ID, RESOURCE_EVENT, ARGS);

        verify(eventService).fireCloudbreakEvent(STACK_ID, UPDATE_FAILED.name(), RESOURCE_EVENT, ARGS);
    }

    private Optional<StackStatus> createStackStatus(Status status) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        return Optional.of(stackStatus);
    }

}