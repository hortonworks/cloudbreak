package com.sequenceiq.cloudbreak.cloud.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@ExtendWith(MockitoExtension.class)
class InstanceStateHandlerTest {

    @Mock
    private InstanceStateQuery instanceStateQuery;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private InstanceStateHandler subject;

    private Event<GetInstancesStateRequest> event;

    private GetInstancesStateRequest<GetInstancesStateResult> request;

    private List<CloudInstance> instances;

    @BeforeEach
    void init() {
        instances = Arrays.asList(
                new CloudInstance("host1", null, null, "subnet-1", "az1"),
                new CloudInstance("host2", null, null, "subnet-1", "az1")
        );
        request = new GetInstancesStateRequest<>(null, null, instances);
        event = new Event<>(request);
    }

    @Test
    void handlesInstanceStatusWhenAvailable() {
        List<CloudVmInstanceStatus> allStarted = allInstancesInStatus(InstanceStatus.STARTED);
        when(instanceStateQuery.getCloudVmInstanceStatuses(any(), any(), eq(instances)))
                .thenReturn(allStarted);

        subject.accept(event);

        verifyResult(new GetInstancesStateResult(request.getResourceId(), allStarted));
    }

    @Test
    void returnsUnknownStatusWhenUnsupported() {
        when(instanceStateQuery.getCloudVmInstanceStatuses(any(), any(), eq(instances)))
                .thenThrow(new CloudOperationNotSupportedException("No check on mock cloud"));

        subject.accept(event);

        verifyResult(new GetInstancesStateResult(request.getResourceId(), allInstancesInStatus(InstanceStatus.UNKNOWN)));
    }

    @Test
    void returnsErrorWhenFailsToSync() {
        String message = "Some error happened";
        CloudConnectorException exception = new CloudConnectorException(message);
        when(instanceStateQuery.getCloudVmInstanceStatuses(any(), any(), eq(instances)))
                .thenThrow(exception);

        subject.accept(event);

        verifyResult(new GetInstancesStateResult("some message", exception, request.getResourceId()));
    }

    private List<CloudVmInstanceStatus> allInstancesInStatus(InstanceStatus status) {
        return instances.stream()
                .map(instance -> new CloudVmInstanceStatus(instance, status))
                .collect(Collectors.toList());
    }

    private void verifyResult(GetInstancesStateResult expectedResult) {
        verify(eventBus).notify(eq(expectedResult.selector()), argThat(resultMatcher(event, expectedResult)));
    }

    private static ArgumentMatcher<Event<GetInstancesStateResult>> resultMatcher(
            Event<GetInstancesStateRequest> sourceEvent,
            GetInstancesStateResult expectedResult
    ) {
        return event -> Objects.equals(sourceEvent.getHeaders().asMap(), event.getHeaders().asMap())
                && Objects.equals(expectedResult.getStatuses(), event.getData().getStatuses())
                && Objects.equals(expectedResult.getStatus(), event.getData().getStatus())
                && Objects.equals(expectedResult.getErrorDetails(), event.getData().getErrorDetails());
    }

}
