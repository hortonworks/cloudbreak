package com.sequenceiq.cloudbreak.reactor.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ValidateDiskRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ValidateDiskResult;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ValidateDiskHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String DELETE_VOLUMES_MESSAGE_PREFIX = "Cannot repair the nodes because one or more disks are missing from node(s): ";

    private static final String DELETE_VOLUMES_MESSAGE_SUFFIX = ", please repair the node(s) with the 'Delete volumes' option, this will create " +
            "new disks for the instance. Please note that 'Delete volumes' will remove all the attached disks and can cause data loss.";

    private static final String CM_SERVER_DISK_MISSING_MESSAGE = "Cannot repair the nodes because one or more disks are missing from the Cloudera " +
            "Manager server node(s): i-cm. The embedded Cloudera Manager database is stored on an attached disk of this node, so the 'Delete " +
            "volumes' option cannot be used for it, because it would delete the database disk too. Please restore the missing disks from a " +
            "volume snapshot or recreate the cluster.";

    private static final String DATABASE_DISK_MISSING_MESSAGE = "Cannot repair the nodes because the embedded Cloudera Manager database disk is " +
            "missing from the Cloudera Manager server node(s): i-cm, so the Cloudera Manager database is lost. Repair cannot restore it, not even " +
            "with the 'Delete volumes' option, which would start Cloudera Manager with a new, empty database. The cluster can only be restored from " +
            "a volume snapshot of the missing disk or recreated.";

    @InjectMocks
    private ValidateDiskHandler underTest;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    void setUp() {
        CloudPlatformVariant variant = mock(CloudPlatformVariant.class);
        when(cloudContext.getPlatformVariant()).thenReturn(variant);
        when(cloudContext.getId()).thenReturn(STACK_ID);
        when(cloudPlatformConnectors.get(variant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
    }

    @Test
    void testNoDiskMissing() {
        CloudResource volumeSet = volumeSet("worker-volumes", "i-worker");
        CloudResource cmVolumeSet = volumeSet("cm-volumes", "i-cm");
        CloudResource databaseVolumeSet = databaseVolumeSet("cm-volumes", "i-cm");
        List<CloudResource> volumeSets = List.of(volumeSet, cmVolumeSet);
        when(resourceConnector.checkForSyncer(authenticatedContext, volumeSets))
                .thenReturn(List.of(new CloudResourceStatus(volumeSet, ResourceStatus.ATTACHED),
                        new CloudResourceStatus(cmVolumeSet, ResourceStatus.ATTACHED)));
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of(databaseVolumeSet)))
                .thenReturn(List.of(new CloudResourceStatus(databaseVolumeSet, ResourceStatus.ATTACHED)));

        ValidateDiskResult result = (ValidateDiskResult) underTest.doAccept(handlerEvent(volumeSets, List.of(databaseVolumeSet), Set.of("i-cm")));

        assertNull(result.getErrorDetails());
    }

    @Test
    void testDiskMissingFromNodeWithoutEmbeddedDatabase() {
        CloudResource volumeSet = volumeSet("worker-volumes", "i-worker");
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of(volumeSet)))
                .thenReturn(List.of(new CloudResourceStatus(volumeSet, ResourceStatus.DELETED)));

        Selectable result = underTest.doAccept(handlerEvent(List.of(volumeSet), List.of(), Set.of()));

        assertEquals(DELETE_VOLUMES_MESSAGE_PREFIX + "i-worker" + DELETE_VOLUMES_MESSAGE_SUFFIX, ((ValidateDiskResult) result).getErrorDetails().getMessage());
        verify(resourceConnector, times(1)).checkForSyncer(any(), any());
    }

    @Test
    void testNonDatabaseDiskMissingFromEmbeddedDatabaseNode() {
        CloudResource cmVolumeSet = volumeSet("cm-volumes", "i-cm");
        CloudResource databaseVolumeSet = databaseVolumeSet("cm-volumes", "i-cm");
        CloudResource workerVolumeSet = volumeSet("worker-volumes", "i-worker");
        List<CloudResource> volumeSets = List.of(cmVolumeSet, workerVolumeSet);
        when(resourceConnector.checkForSyncer(authenticatedContext, volumeSets))
                .thenReturn(List.of(new CloudResourceStatus(cmVolumeSet, ResourceStatus.DELETED),
                        new CloudResourceStatus(workerVolumeSet, ResourceStatus.DELETED)));
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of(databaseVolumeSet)))
                .thenReturn(List.of(new CloudResourceStatus(databaseVolumeSet, ResourceStatus.ATTACHED)));

        Selectable result = underTest.doAccept(handlerEvent(volumeSets, List.of(databaseVolumeSet), Set.of("i-cm")));

        assertEquals(CM_SERVER_DISK_MISSING_MESSAGE, ((ValidateDiskResult) result).getErrorDetails().getMessage());
    }

    @Test
    void testDatabaseDiskMissingFromEmbeddedDatabaseNode() {
        CloudResource cmVolumeSet = volumeSet("cm-volumes", "i-cm");
        CloudResource databaseVolumeSet = databaseVolumeSet("cm-volumes", "i-cm");
        CloudResource workerVolumeSet = volumeSet("worker-volumes", "i-worker");
        List<CloudResource> volumeSets = List.of(cmVolumeSet, workerVolumeSet);
        when(resourceConnector.checkForSyncer(authenticatedContext, volumeSets))
                .thenReturn(List.of(new CloudResourceStatus(cmVolumeSet, ResourceStatus.DELETED),
                        new CloudResourceStatus(workerVolumeSet, ResourceStatus.DELETED)));
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of(databaseVolumeSet)))
                .thenReturn(List.of(new CloudResourceStatus(databaseVolumeSet, ResourceStatus.DELETED)));

        Selectable result = underTest.doAccept(handlerEvent(volumeSets, List.of(databaseVolumeSet), Set.of("i-cm")));

        assertEquals(DATABASE_DISK_MISSING_MESSAGE, ((ValidateDiskResult) result).getErrorDetails().getMessage());
    }

    @Test
    void testDatabaseDiskMissingWhenSyncerReturnsCopyOfTheResource() {
        CloudResource databaseVolumeSet = databaseVolumeSet("cm-volumes", "i-cm");
        CloudResource copyWithoutParameters = CloudResource.builder().cloudResource(databaseVolumeSet).build();
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of()))
                .thenReturn(List.of());
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of(databaseVolumeSet)))
                .thenReturn(List.of(new CloudResourceStatus(copyWithoutParameters, ResourceStatus.DELETED)));

        Selectable result = underTest.doAccept(handlerEvent(List.of(), List.of(databaseVolumeSet), Set.of("i-cm")));

        assertEquals(DATABASE_DISK_MISSING_MESSAGE, ((ValidateDiskResult) result).getErrorDetails().getMessage());
    }

    @Test
    void testCheckForSyncerNotSupported() {
        when(cloudContext.getPlatform()).thenReturn(Platform.platform("MOCK"));
        when(cloudContext.getVariant()).thenReturn(Variant.variant("MOCK"));
        when(resourceConnector.checkForSyncer(authenticatedContext, List.of())).thenThrow(new UnsupportedOperationException("not supported"));

        ValidateDiskResult result = (ValidateDiskResult) underTest.doAccept(handlerEvent(List.of(), List.of(), Set.of()));

        assertNull(result.getErrorDetails());
    }

    private CloudResource volumeSet(String name, String instanceId) {
        return CloudResource.builder()
                .withType(ResourceType.AWS_VOLUMESET)
                .withName(name)
                .withInstanceId(instanceId)
                .build();
    }

    private CloudResource databaseVolumeSet(String name, String instanceId) {
        CloudResource databaseVolumeSet = volumeSet(name, instanceId);
        databaseVolumeSet.setTypedAttributes(new VolumeSetAttributes("az", true, "", List.of(
                new VolumeSetAttributes.Volume("vol-db", "/dev/xvdc", 100, "gp3", CloudVolumeUsageType.DATABASE)), 100, "gp3"));
        return databaseVolumeSet;
    }

    private HandlerEvent<ValidateDiskRequest> handlerEvent(List<CloudResource> cloudResources, List<CloudResource> embeddedDatabaseVolumes,
            Set<String> clusterManagerServerWithEmbeddedDbInstanceIds) {
        HandlerEvent<ValidateDiskRequest> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(
                new ValidateDiskRequest<>(cloudContext, cloudCredential, null, cloudResources, embeddedDatabaseVolumes,
                        clusterManagerServerWithEmbeddedDbInstanceIds));
        return handlerEvent;
    }
}
