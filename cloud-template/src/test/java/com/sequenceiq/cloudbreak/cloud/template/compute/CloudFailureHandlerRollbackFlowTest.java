package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.template.compute.CloudFailureHandler.ScaleContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.RolledbackResourcesException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Wires the real rollback path together (CloudFailureHandler -> ComputeResourceService -> ResourceActionFactory -> ResourceDeletionCallable -> builder.delete)
 * to prove that when a node's instance creation fails, the volume set created for that node is actually deleted instead of being preserved for reattachment,
 * which is what would otherwise leave an orphaned persistent disk behind (CB-33458). The GCP-specific honouring of the flag is covered by
 * GcpAttachedDiskResourceBuilderTest; here we use a fake builder that mirrors the same preserve-vs-delete contract.
 */
@ExtendWith(MockitoExtension.class)
class CloudFailureHandlerRollbackFlowTest {

    private static final Variant VARIANT = Variant.variant("MOCK");

    private final CloudFailureHandler cloudFailureHandler = new CloudFailureHandler();

    private final ComputeResourceService computeResourceService = new ComputeResourceService();

    private final ResourceActionFactory resourceActionFactory = new ResourceActionFactory();

    private final RecordingVolumeSetBuilder volumeSetBuilder = new RecordingVolumeSetBuilder();

    @Mock
    private ResourceBuilders resourceBuilders;

    @Mock
    private ResourcePollTaskFactory resourcePollTaskFactory;

    @Mock
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Mock
    private SyncPollingScheduler<List<CloudVmInstanceStatus>> syncVMPollingScheduler;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resourceActionFactory, "resourceBuilders", resourceBuilders);
        ReflectionTestUtils.setField(resourceActionFactory, "syncPollingScheduler", syncPollingScheduler);
        ReflectionTestUtils.setField(resourceActionFactory, "resourcePollTaskFactory", resourcePollTaskFactory);
        ReflectionTestUtils.setField(resourceActionFactory, "persistenceNotifier", persistenceNotifier);

        ReflectionTestUtils.setField(computeResourceService, "resourceBuilders", resourceBuilders);
        ReflectionTestUtils.setField(computeResourceService, "cloudFailureHandler", cloudFailureHandler);
        ReflectionTestUtils.setField(computeResourceService, "syncPollingScheduler", syncPollingScheduler);
        ReflectionTestUtils.setField(computeResourceService, "syncVMPollingScheduler", syncVMPollingScheduler);
        ReflectionTestUtils.setField(computeResourceService, "resourcePollTaskFactory", resourcePollTaskFactory);
        ReflectionTestUtils.setField(computeResourceService, "resourceActionFactory", resourceActionFactory);

        ReflectionTestUtils.setField(cloudFailureHandler, "computeResourceService", computeResourceService);
        ReflectionTestUtils.setField(cloudFailureHandler, "cloudbreakEventService", Optional.of(cloudbreakEventService));
        ReflectionTestUtils.setField(cloudFailureHandler, "persistenceNotifier", persistenceNotifier);

        lenient().when(resourceBuilders.compute(VARIANT)).thenReturn(List.of(volumeSetBuilder));
    }

    @Test
    void volumeSetOfFailedNodeIsDeletedNotPreservedDuringRollback() {
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getVariant()).thenReturn(VARIANT);
        AuthenticatedContext auth = new AuthenticatedContext(cloudContext, mock(CloudCredential.class));
        ResourceBuilderContext ctx = new ResourceBuilderContext("stack", Location.location(Region.region("region")), 8, true);
        Group group = mock(Group.class);

        CloudResource failedInstance = CloudResource.builder().withName("instance-1").withGroup("group")
                .withType(ResourceType.MOCK_INSTANCE).withStatus(CommonStatus.FAILED).withParameters(new HashMap<>()).build();
        CloudResource volumeSet = volumeSetResource("vol-1", false);

        Map<CloudResourceStatus, Group> failedResources = new HashMap<>();
        failedResources.put(new CloudResourceStatus(failedInstance, ResourceStatus.FAILED, "stockout", 1L), group);
        Map<CloudResourceStatus, Group> allResources = new HashMap<>(failedResources);
        allResources.put(new CloudResourceStatus(volumeSet, ResourceStatus.CREATED, 1L), group);

        CloudFailureContext failureContext = new CloudFailureContext(auth, new ScaleContext(true, AdjustmentType.EXACT, 1L), ctx);

        // 0 successful nodes below the threshold => everything is rolled back and the exception is rethrown
        assertThrows(RolledbackResourcesException.class,
                () -> cloudFailureHandler.rollbackIfNecessary(failureContext, failedResources, allResources, 1));

        assertThat(volumeSetBuilder.deleted).as("volume set must be deleted, not preserved for reattachment").isTrue();
        assertThat(volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getDeleteOnTermination()).isTrue();
    }

    private CloudResource volumeSetResource(String name, boolean deleteOnTermination) {
        VolumeSetAttributes attributes = new VolumeSetAttributes("az", deleteOnTermination, "", "", List.of(), null);
        Map<String, Object> params = new HashMap<>();
        params.put(CloudResource.ATTRIBUTES, attributes);
        return CloudResource.builder().withName(name).withGroup("group").withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withStatus(CommonStatus.CREATED).withParameters(params).build();
    }

    /**
     * Minimal stand-in for a provider volume-set builder that reproduces the real preserve-vs-delete contract: it refuses to delete (preserves the disk for
     * later reattachment) while deleteOnTermination is false, and only really deletes once the flag has been flipped.
     */
    private static final class RecordingVolumeSetBuilder implements ComputeResourceBuilder<ResourceBuilderContext> {

        private boolean deleted;

        @Override
        public CloudResource delete(ResourceBuilderContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
            VolumeSetAttributes attributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            if (!Boolean.TRUE.equals(attributes.getDeleteOnTermination())) {
                resource.setStatus(CommonStatus.DETACHED);
                throw new PreserveResourceException("Resource will be preserved for later reattachment.");
            }
            deleted = true;
            return null;
        }

        @Override
        public ResourceType resourceType() {
            return ResourceType.GCP_ATTACHED_DISKSET;
        }

        @Override
        public int order() {
            return 1;
        }

        @Override
        public Platform platform() {
            return Platform.platform("MOCK");
        }

        @Override
        public Variant variant() {
            return VARIANT;
        }

        @Override
        public List<CloudResource> create(ResourceBuilderContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
                Image image) {
            throw new UnsupportedOperationException("not needed for rollback flow test");
        }

        @Override
        public List<CloudResource> build(ResourceBuilderContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
                List<CloudResource> buildableResource, com.sequenceiq.cloudbreak.cloud.model.CloudStack cloudStack) {
            throw new UnsupportedOperationException("not needed for rollback flow test");
        }

        @Override
        public List<CloudVmInstanceStatus> checkInstances(ResourceBuilderContext context, AuthenticatedContext auth, List<CloudInstance> instances) {
            throw new UnsupportedOperationException("not needed for rollback flow test");
        }

        @Override
        public CloudVmInstanceStatus start(ResourceBuilderContext context, AuthenticatedContext auth, CloudInstance instance) {
            throw new UnsupportedOperationException("not needed for rollback flow test");
        }

        @Override
        public CloudVmInstanceStatus stop(ResourceBuilderContext context, AuthenticatedContext auth, CloudInstance instance) {
            throw new UnsupportedOperationException("not needed for rollback flow test");
        }

        @Override
        public List<CloudResourceStatus> checkResources(ResourceBuilderContext context, AuthenticatedContext auth, List<CloudResource> resources) {
            throw new UnsupportedOperationException("not needed for rollback flow test");
        }
    }
}
