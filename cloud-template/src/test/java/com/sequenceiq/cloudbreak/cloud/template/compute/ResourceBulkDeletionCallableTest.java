package com.sequenceiq.cloudbreak.cloud.template.compute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class ResourceBulkDeletionCallableTest {

    @Mock
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Mock
    private ResourcePollTaskFactory resourcePollTaskFactory;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private ResourceBuilderContext context;

    @Mock
    private AuthenticatedContext auth;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private ComputeResourceBuilder<ResourceBuilderContext> builder;

    @Mock
    private PollTask<List<CloudResourceStatus>> pollTask;

    @BeforeEach
    void setUp() {
        lenient().when(auth.getCloudContext()).thenReturn(cloudContext);
    }

    @Test
    void testCallPollsOnlyTheResourcesWithRequestedDeletion() throws Exception {
        CloudResource inProgress = newResource("in-progress");
        CloudResource nothingToDelete = newResource("nothing-to-delete");
        CloudResource failed = newResource("failed");
        when(resourcePollTaskFactory.newPollResourceTask(builder, auth, List.of(inProgress), context, true)).thenReturn(pollTask);
        when(syncPollingScheduler.schedule(pollTask)).thenReturn(List.of(new CloudResourceStatus(inProgress, ResourceStatus.DELETED)));

        List<CloudResourceStatus> actual = underTest(List.of(
                new CloudResourceStatus(inProgress, ResourceStatus.IN_PROGRESS),
                new CloudResourceStatus(nothingToDelete, ResourceStatus.DELETED),
                new CloudResourceStatus(failed, ResourceStatus.FAILED, "no permission"))).call();

        assertThat(actual).extracting(CloudResourceStatus::getCloudResource, CloudResourceStatus::getStatus).containsExactlyInAnyOrder(
                tuple(inProgress, ResourceStatus.DELETED),
                tuple(nothingToDelete, ResourceStatus.DELETED),
                tuple(failed, ResourceStatus.FAILED));
        verify(persistenceNotifier).notifyDeletion(inProgress, cloudContext);
        verify(persistenceNotifier).notifyDeletion(nothingToDelete, cloudContext);
        verify(persistenceNotifier, never()).notifyDeletion(eq(failed), any());
    }

    @Test
    void testCallDoesNotPollWhenNothingIsInProgress() throws Exception {
        CloudResource nothingToDelete = newResource("nothing-to-delete");

        List<CloudResourceStatus> actual = underTest(List.of(new CloudResourceStatus(nothingToDelete, ResourceStatus.DELETED))).call();

        assertThat(actual).extracting(CloudResourceStatus::getStatus).containsExactly(ResourceStatus.DELETED);
        verify(persistenceNotifier).notifyDeletion(nothingToDelete, cloudContext);
        verifyNoInteractions(resourcePollTaskFactory, syncPollingScheduler);
    }

    @Test
    void testCallKeepsThePolledResourcesWhenThePollingFails() throws Exception {
        CloudResource inProgress = newResource("in-progress");
        CloudResource nothingToDelete = newResource("nothing-to-delete");
        CloudConnectorException pollingFailure = new CloudConnectorException("polling timed out");
        when(resourcePollTaskFactory.newPollResourceTask(builder, auth, List.of(inProgress), context, true)).thenReturn(pollTask);
        when(syncPollingScheduler.schedule(pollTask)).thenThrow(pollingFailure);

        ResourceBulkDeletionCallable callable = underTest(List.of(
                new CloudResourceStatus(inProgress, ResourceStatus.IN_PROGRESS),
                new CloudResourceStatus(nothingToDelete, ResourceStatus.DELETED)));

        assertThatThrownBy(callable::call).isSameAs(pollingFailure);
        verify(persistenceNotifier).notifyDeletion(nothingToDelete, cloudContext);
        verify(persistenceNotifier, never()).notifyDeletion(eq(inProgress), any());
    }

    private ResourceBulkDeletionCallable underTest(List<CloudResourceStatus> deletionStatuses) {
        return new ResourceBulkDeletionCallable(new ResourceBulkDeletionCallablePayload(context, auth, deletionStatuses, builder, true),
                syncPollingScheduler, resourcePollTaskFactory, persistenceNotifier);
    }

    private CloudResource newResource(String name) {
        return CloudResource.builder()
                .withName(name)
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .build();
    }
}
