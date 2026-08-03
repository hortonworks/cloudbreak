package com.sequenceiq.cloudbreak.cloud.gcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.DisksResizeRequest;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Snapshot;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskInsertOperationService.GcpDiskInsertOutcome;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpOperationUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpDiskUpdateRetryServiceTest {

    private static final String PROJECT_ID = "test-project";

    private static final String ZONE = "us-central1-a";

    private static final String INSTANCE_ID = "instance1";

    private static final String DISK_NAME = "disk1";

    private static final String SNAPSHOT_NAME = "disk1-typechange";

    private static final String NEW_DISK_NAME = "disk1-pdssd";

    private static final String SOURCE_SNAPSHOT_URL = "https://www.googleapis.com/compute/v1/projects/test-project/global/snapshots/disk1-typechange";

    private static final String INSTANCE_URL = "https://www.googleapis.com/compute/v1/projects/test-project/zones/us-central1-a/instances/instance1";

    @InjectMocks
    private GcpDiskUpdateRetryService underTest;

    @Mock
    private ResourcePollTaskFactory statusCheckFactory;

    @Mock
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Mock
    private GcpDiskInsertOperationService gcpDiskInsertOperationService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Compute compute;

    @Mock
    private Compute.Disks disks;

    @Mock
    private GcpContext gcpContext;

    @Mock
    private AuthenticatedContext authenticatedContext;

    private CloudResource cloudResource;

    @BeforeEach
    void setUp() {
        cloudResource = CloudResource.builder()
                .withInstanceId(INSTANCE_ID)
                .withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .build();
    }

    @Test
    void testResizeDiskSubmitsResizeAndWaitsForOperation() throws Exception {
        when(compute.disks()).thenReturn(disks);
        Compute.Disks.Resize resize = mock(Compute.Disks.Resize.class);
        when(disks.resize(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME), any(DisksResizeRequest.class))).thenReturn(resize);
        when(resize.execute()).thenReturn(new Operation().setName("op-1"));

        PollTask<List<CloudResourceStatus>> task = mock(PollTask.class);
        when(statusCheckFactory.newPollResourceTask(eq(underTest), eq(authenticatedContext), any(), eq(gcpContext), eq(true))).thenReturn(task);

        GcpResizeDiskParameters params = new GcpResizeDiskParameters(compute, PROJECT_ID, ZONE, DISK_NAME, 200, createDiskResource(), authenticatedContext);
        underTest.resizeDisk(params, gcpContext);

        ArgumentCaptor<DisksResizeRequest> resizeCaptor = ArgumentCaptor.forClass(DisksResizeRequest.class);
        verify(disks).resize(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME), resizeCaptor.capture());
        assertEquals(200L, resizeCaptor.getValue().getSizeGb());

        ArgumentCaptor<List<CloudResource>> resourceCaptor = ArgumentCaptor.forClass(List.class);
        verify(statusCheckFactory).newPollResourceTask(eq(underTest), eq(authenticatedContext), resourceCaptor.capture(), eq(gcpContext), eq(true));
        CloudResource operationAwareResource = resourceCaptor.getValue().get(0);
        assertEquals("op-1", GcpOperationUtil.getOperationInfo(operationAwareResource).operationId());
        assertEquals("val", operationAwareResource.getStringParameter("key"), "The inherited helper should preserve the original resource parameters");
        verify(syncPollingScheduler).schedule(task);
    }

    @Test
    void testResizeDiskFailsFastWhenOperationReturnsHttpError() throws Exception {
        when(compute.disks()).thenReturn(disks);
        Compute.Disks.Resize resize = mock(Compute.Disks.Resize.class);
        when(disks.resize(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME), any(DisksResizeRequest.class))).thenReturn(resize);
        when(resize.execute()).thenReturn(new Operation().setName("op-1").setHttpErrorStatusCode(400).setHttpErrorMessage("BAD REQUEST"));

        GcpResizeDiskParameters params = new GcpResizeDiskParameters(compute, PROJECT_ID, ZONE, DISK_NAME, 200, createDiskResource(), authenticatedContext);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.resizeDisk(params, gcpContext));
        assertTrue(exception.getMessage().contains("BAD REQUEST"));
        verifyNoInteractions(statusCheckFactory, syncPollingScheduler);
    }

    @Test
    void testInsertDiskReturnsOperationResourceWhenSubmittedAndDoesNotPoll() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        Operation operation = new Operation().setName("op-1");
        when(gcpDiskInsertOperationService.insertDiskIfAbsent(eq(compute), eq(PROJECT_ID), eq(ZONE), eq(disk), eq(DISK_NAME)))
                .thenReturn(new GcpDiskInsertOutcome(Optional.of(operation), Optional.empty()));

        GcpCreateDiskParameters params = new GcpCreateDiskParameters(compute, PROJECT_ID, ZONE, DISK_NAME, disk, createDiskResource(), authenticatedContext);
        Optional<CloudResource> result = underTest.insertDisk(params);

        assertTrue(result.isPresent());
        assertEquals("op-1", GcpOperationUtil.getOperationInfo(result.get()).operationId());
        verifyNoInteractions(statusCheckFactory, syncPollingScheduler);
    }

    @Test
    void testInsertDiskReusesUnattachedExistingDiskWithEmptyResult() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        when(gcpDiskInsertOperationService.insertDiskIfAbsent(eq(compute), eq(PROJECT_ID), eq(ZONE), eq(disk), eq(DISK_NAME)))
                .thenReturn(new GcpDiskInsertOutcome(Optional.empty(), Optional.of(new Disk().setName(DISK_NAME))));

        GcpCreateDiskParameters params = new GcpCreateDiskParameters(compute, PROJECT_ID, ZONE, DISK_NAME, disk, createDiskResource(), authenticatedContext);
        Optional<CloudResource> result = underTest.insertDisk(params);

        assertTrue(result.isEmpty());
        verifyNoInteractions(statusCheckFactory, syncPollingScheduler);
    }

    @Test
    void testInsertDiskReusesExistingDiskAttachedToTargetInstance() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        Disk existing = new Disk().setName(DISK_NAME)
                .setUsers(List.of("https://www.googleapis.com/compute/v1/projects/test-project/zones/us-central1-a/instances/" + INSTANCE_ID));
        when(gcpDiskInsertOperationService.insertDiskIfAbsent(eq(compute), eq(PROJECT_ID), eq(ZONE), eq(disk), eq(DISK_NAME)))
                .thenReturn(new GcpDiskInsertOutcome(Optional.empty(), Optional.of(existing)));

        GcpCreateDiskParameters params = new GcpCreateDiskParameters(compute, PROJECT_ID, ZONE, DISK_NAME, disk, createDiskResource(), authenticatedContext);
        Optional<CloudResource> result = underTest.insertDisk(params);

        assertTrue(result.isEmpty());
        verifyNoInteractions(statusCheckFactory, syncPollingScheduler);
    }

    @Test
    void testInsertDiskFailsWhenExistingDiskAttachedToDifferentInstance() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        Disk existing = new Disk().setName(DISK_NAME)
                .setUsers(List.of("https://www.googleapis.com/compute/v1/projects/test-project/zones/us-central1-a/instances/other-instance"));
        when(gcpDiskInsertOperationService.insertDiskIfAbsent(eq(compute), eq(PROJECT_ID), eq(ZONE), eq(disk), eq(DISK_NAME)))
                .thenReturn(new GcpDiskInsertOutcome(Optional.empty(), Optional.of(existing)));

        GcpCreateDiskParameters params = new GcpCreateDiskParameters(compute, PROJECT_ID, ZONE, DISK_NAME, disk, createDiskResource(), authenticatedContext);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.insertDisk(params));
        assertTrue(exception.getMessage().contains("different instance"));
        verifyNoInteractions(statusCheckFactory, syncPollingScheduler);
    }

    @Test
    void testInsertDiskRetriesOnTransientGoogleError() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        when(gcpDiskInsertOperationService.insertDiskIfAbsent(eq(compute), eq(PROJECT_ID), eq(ZONE), eq(disk), eq(DISK_NAME)))
                .thenThrow(googleException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Service Unavailable"));

        GcpCreateDiskParameters params = new GcpCreateDiskParameters(compute, PROJECT_ID, ZONE, DISK_NAME, disk, createDiskResource(), authenticatedContext);

        assertThrows(CloudConnectorException.class, () -> underTest.insertDisk(params));
        verifyNoInteractions(statusCheckFactory, syncPollingScheduler);
    }

    @Test
    void testInsertDiskFailsFastOnNonTransientGoogleError() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        when(gcpDiskInsertOperationService.insertDiskIfAbsent(eq(compute), eq(PROJECT_ID), eq(ZONE), eq(disk), eq(DISK_NAME)))
                .thenThrow(googleException(HttpStatus.SC_BAD_REQUEST, "Bad Request"));

        GcpCreateDiskParameters params = new GcpCreateDiskParameters(compute, PROJECT_ID, ZONE, DISK_NAME, disk, createDiskResource(), authenticatedContext);

        assertThrows(CloudbreakServiceException.class, () -> underTest.insertDisk(params));
        verifyNoInteractions(statusCheckFactory, syncPollingScheduler);
    }

    @Test
    void testDeleteDiskReturnsOperationResourceAndDoesNotPoll() throws Exception {
        when(compute.disks()).thenReturn(disks);
        Compute.Disks.Delete delete = mock(Compute.Disks.Delete.class);
        Disk disk = new Disk().setName(DISK_NAME);
        when(disks.delete(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME))).thenReturn(delete);
        when(delete.execute()).thenReturn(new Operation().setName("op-2"));

        GcpCreateDiskParameters params = new GcpCreateDiskParameters(compute, PROJECT_ID, ZONE, DISK_NAME, disk, createDiskResource(), authenticatedContext);
        Optional<CloudResource> result = underTest.deleteDisk(params);

        assertTrue(result.isPresent());
        assertEquals("op-2", GcpOperationUtil.getOperationInfo(result.get()).operationId());
        verify(disks).delete(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME));
        verifyNoInteractions(statusCheckFactory, syncPollingScheduler);
    }

    @Test
    void testDeleteDiskReturnsEmptyWhenDiskNotFound() throws Exception {
        when(compute.disks()).thenReturn(disks);
        Compute.Disks.Delete delete = mock(Compute.Disks.Delete.class);
        Disk disk = new Disk().setName(DISK_NAME);
        when(disks.delete(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME))).thenReturn(delete);
        when(delete.execute()).thenThrow(googleException(HttpStatus.SC_NOT_FOUND, "Not Found"));

        GcpCreateDiskParameters params = new GcpCreateDiskParameters(compute, PROJECT_ID, ZONE, DISK_NAME, disk, createDiskResource(), authenticatedContext);
        Optional<CloudResource> result = underTest.deleteDisk(params);

        assertTrue(result.isEmpty());
        verifyNoInteractions(statusCheckFactory, syncPollingScheduler);
    }

    @Test
    void testPollDiskOperationsSchedulesOncePerBatch() throws Exception {
        CloudResource op1 = createDiskResource();
        CloudResource op2 = createDiskResource();
        PollTask<List<CloudResourceStatus>> task = mock(PollTask.class);
        when(statusCheckFactory.newPollResourceTask(eq(underTest), eq(authenticatedContext), eq(List.of(op1, op2)), eq(gcpContext), eq(true)))
                .thenReturn(task);

        underTest.pollDiskOperations(authenticatedContext, gcpContext, List.of(op1, op2));

        verify(syncPollingScheduler).schedule(task);
    }

    @Test
    void testPollDiskOperationsReturnsEmptyWhenNothingToPoll() throws Exception {
        List<CloudResourceStatus> result = underTest.pollDiskOperations(authenticatedContext, gcpContext, List.of());

        assertTrue(result.isEmpty());
        verifyNoInteractions(statusCheckFactory, syncPollingScheduler);
    }

    @Test
    void testCheckResourcesDelegatesToAttachedDiskSetCheck() {
        List<CloudResource> resources = List.of();

        List<CloudResourceStatus> result = underTest.checkResources(gcpContext, authenticatedContext, resources);

        assertEquals(0, result.size());
    }

    private GoogleJsonResponseException googleException(int statusCode, String statusMessage) {
        GoogleJsonError details = new GoogleJsonError();
        details.setCode(statusCode);
        return new GoogleJsonResponseException(new HttpResponseException.Builder(statusCode, statusMessage, new HttpHeaders()), details);
    }

    private CloudResource createDiskResource() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("key", "val");
        return CloudResource.builder()
                .withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withInstanceId(INSTANCE_ID)
                .withAvailabilityZone(ZONE)
                .withParameters(parameters)
                .build();
    }

    @Test
    void detachDiskDetachesAndPollsWhenAttachedToTargetInstance() throws Exception {
        Disk disk = new Disk().setUsers(List.of(INSTANCE_URL));
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(disk);
        when(compute.instances().detachDisk(PROJECT_ID, ZONE, INSTANCE_ID, DISK_NAME).execute()).thenReturn(new Operation());
        PollTask<List<CloudResourceStatus>> pollTask = mock(PollTask.class);
        when(statusCheckFactory.newPollResourceTask(eq(underTest), eq(authenticatedContext), any(), eq(gcpContext), anyBoolean()))
                .thenReturn(pollTask);

        underTest.detachDiskFromInstance(parameters(), gcpContext);

        verify(syncPollingScheduler).schedule(pollTask);
    }

    @Test
    void detachDiskSkipsWhenDiskHasNoUsers() throws Exception {
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(new Disk());

        List<CloudResourceStatus> result = underTest.detachDiskFromInstance(parameters(), gcpContext);

        assertTrue(result.isEmpty());
        verify(compute.instances(), never()).detachDisk(any(), any(), any(), any());
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void detachDiskSkipsWhenDiskAttachedToDifferentInstance() throws Exception {
        Disk disk = new Disk().setUsers(List.of(INSTANCE_URL.replace(INSTANCE_ID, "otherInstance")));
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(disk);

        List<CloudResourceStatus> result = underTest.detachDiskFromInstance(parameters(), gcpContext);

        assertTrue(result.isEmpty());
        verify(compute.instances(), never()).detachDisk(any(), any(), any(), any());
    }

    @Test
    void detachDiskSkipsWhenDiskNotFound() throws Exception {
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenThrow(googleException(404, "Not Found"));

        List<CloudResourceStatus> result = underTest.detachDiskFromInstance(parameters(), gcpContext);

        assertTrue(result.isEmpty());
        verify(compute.instances(), never()).detachDisk(any(), any(), any(), any());
    }

    @Test
    void detachDiskThrowsRetryableOnTransientError() throws Exception {
        Disk disk = new Disk().setUsers(List.of(INSTANCE_URL));
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(disk);
        when(compute.instances().detachDisk(PROJECT_ID, ZONE, INSTANCE_ID, DISK_NAME).execute()).thenThrow(googleException(429, "Too Many Requests"));

        assertThrows(CloudConnectorException.class, () -> underTest.detachDiskFromInstance(parameters(), gcpContext));
    }

    @Test
    void detachDiskFailsFastOnClientError() throws Exception {
        Disk disk = new Disk().setUsers(List.of(INSTANCE_URL));
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(disk);
        when(compute.instances().detachDisk(PROJECT_ID, ZONE, INSTANCE_ID, DISK_NAME).execute()).thenThrow(googleException(400, "Bad Request"));

        assertThrows(CloudbreakServiceException.class, () -> underTest.detachDiskFromInstance(parameters(), gcpContext));
    }

    @Test
    void attachDiskAttachesAndPollsWhenDiskNotYetAttached() throws Exception {
        AttachedDisk attachedDisk = new AttachedDisk().setDeviceName(DISK_NAME);
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(new Disk());
        when(compute.instances().attachDisk(PROJECT_ID, ZONE, INSTANCE_ID, attachedDisk).execute()).thenReturn(new Operation());
        PollTask<List<CloudResourceStatus>> pollTask = mock(PollTask.class);
        when(statusCheckFactory.newPollResourceTask(eq(underTest), eq(authenticatedContext), any(), eq(gcpContext), anyBoolean()))
                .thenReturn(pollTask);

        underTest.attachDiskToInstance(parameters(), attachedDisk, gcpContext);

        verify(syncPollingScheduler).schedule(pollTask);
    }

    @Test
    void attachDiskProceedsWhenDiskNotFoundOnProvider() throws Exception {
        AttachedDisk attachedDisk = new AttachedDisk().setDeviceName(DISK_NAME);
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenThrow(googleException(404, "Not Found"));
        when(compute.instances().attachDisk(PROJECT_ID, ZONE, INSTANCE_ID, attachedDisk).execute()).thenReturn(new Operation());
        PollTask<List<CloudResourceStatus>> pollTask = mock(PollTask.class);
        when(statusCheckFactory.newPollResourceTask(eq(underTest), eq(authenticatedContext), any(), eq(gcpContext), anyBoolean()))
                .thenReturn(pollTask);

        underTest.attachDiskToInstance(parameters(), attachedDisk, gcpContext);

        verify(syncPollingScheduler).schedule(pollTask);
    }

    @Test
    void attachDiskSkipsWhenAlreadyAttachedToTargetInstance() throws Exception {
        AttachedDisk attachedDisk = new AttachedDisk().setDeviceName(DISK_NAME);
        Disk disk = new Disk().setUsers(List.of(INSTANCE_URL));
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(disk);

        List<CloudResourceStatus> result = underTest.attachDiskToInstance(parameters(), attachedDisk, gcpContext);

        assertTrue(result.isEmpty());
        verify(compute.instances(), never()).attachDisk(any(), any(), any(), any());
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void attachDiskThrowsWhenAttachedToDifferentInstance() throws Exception {
        AttachedDisk attachedDisk = new AttachedDisk().setDeviceName(DISK_NAME);
        Disk disk = new Disk().setUsers(List.of(INSTANCE_URL.replace(INSTANCE_ID, "otherInstance")));
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(disk);

        assertThrows(CloudbreakServiceException.class, () -> underTest.attachDiskToInstance(parameters(), attachedDisk, gcpContext));
        verify(compute.instances(), never()).attachDisk(any(), any(), any(), any());
    }

    @Test
    void attachDiskThrowsRetryableOnTransientError() throws Exception {
        AttachedDisk attachedDisk = new AttachedDisk().setDeviceName(DISK_NAME);
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(new Disk());
        when(compute.instances().attachDisk(PROJECT_ID, ZONE, INSTANCE_ID, attachedDisk).execute()).thenThrow(googleException(500, "Server Error"));

        assertThrows(CloudConnectorException.class, () -> underTest.attachDiskToInstance(parameters(), attachedDisk, gcpContext));
    }

    @Test
    void attachDiskFailsFastOnClientError() throws Exception {
        AttachedDisk attachedDisk = new AttachedDisk().setDeviceName(DISK_NAME);
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(new Disk());
        when(compute.instances().attachDisk(PROJECT_ID, ZONE, INSTANCE_ID, attachedDisk).execute()).thenThrow(googleException(400, "Bad Request"));

        assertThrows(CloudbreakServiceException.class, () -> underTest.attachDiskToInstance(parameters(), attachedDisk, gcpContext));
    }

    @Test
    void deleteDiskDeletesAndPolls() throws Exception {
        when(compute.disks().delete(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(new Operation());
        PollTask<List<CloudResourceStatus>> pollTask = mock(PollTask.class);
        when(statusCheckFactory.newPollResourceTask(eq(underTest), eq(authenticatedContext), any(), eq(gcpContext), anyBoolean()))
                .thenReturn(pollTask);

        underTest.deleteDisk(parameters(), gcpContext);

        verify(syncPollingScheduler).schedule(pollTask);
    }

    @Test
    void deleteDiskToleratesMissingDisk() throws Exception {
        when(compute.disks().delete(PROJECT_ID, ZONE, DISK_NAME).execute()).thenThrow(googleException(404, "Not Found"));

        List<CloudResourceStatus> result = underTest.deleteDisk(parameters(), gcpContext);

        assertTrue(result.isEmpty());
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void deleteDiskThrowsRetryableOnTransientError() throws Exception {
        when(compute.disks().delete(PROJECT_ID, ZONE, DISK_NAME).execute()).thenThrow(googleException(429, "Too Many Requests"));

        assertThrows(CloudConnectorException.class, () -> underTest.deleteDisk(parameters(), gcpContext));
    }

    @Test
    void createSnapshotSubmitsSnapshotAndPollsZonalOperation() throws Exception {
        Snapshot snapshot = new Snapshot().setName(SNAPSHOT_NAME);
        when(compute.disks().createSnapshot(PROJECT_ID, ZONE, DISK_NAME, snapshot).execute()).thenReturn(new Operation().setName("op-snap"));
        PollTask<List<CloudResourceStatus>> pollTask = mock(PollTask.class);
        when(statusCheckFactory.newPollResourceTask(eq(underTest), eq(authenticatedContext), any(), eq(gcpContext), anyBoolean()))
                .thenReturn(pollTask);

        underTest.createSnapshot(snapshotParameters(), snapshot, gcpContext);

        verify(syncPollingScheduler).schedule(pollTask);
    }

    @Test
    void createSnapshotFailsFastWhenOperationReturnsHttpError() throws Exception {
        Snapshot snapshot = new Snapshot().setName(SNAPSHOT_NAME);
        when(compute.disks().createSnapshot(PROJECT_ID, ZONE, DISK_NAME, snapshot).execute())
                .thenReturn(new Operation().setName("op-snap").setHttpErrorStatusCode(400).setHttpErrorMessage("BAD REQUEST"));

        CloudbreakServiceException exception =
                assertThrows(CloudbreakServiceException.class, () -> underTest.createSnapshot(snapshotParameters(), snapshot, gcpContext));
        assertTrue(exception.getMessage().contains("BAD REQUEST"));
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void createSnapshotThrowsRetryableOnTransientError() throws Exception {
        Snapshot snapshot = new Snapshot().setName(SNAPSHOT_NAME);
        when(compute.disks().createSnapshot(PROJECT_ID, ZONE, DISK_NAME, snapshot).execute()).thenThrow(googleException(503, "Service Unavailable"));

        assertThrows(CloudConnectorException.class, () -> underTest.createSnapshot(snapshotParameters(), snapshot, gcpContext));
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void createSnapshotFailsFastOnClientError() throws Exception {
        Snapshot snapshot = new Snapshot().setName(SNAPSHOT_NAME);
        when(compute.disks().createSnapshot(PROJECT_ID, ZONE, DISK_NAME, snapshot).execute()).thenThrow(googleException(400, "Bad Request"));

        assertThrows(CloudbreakServiceException.class, () -> underTest.createSnapshot(snapshotParameters(), snapshot, gcpContext));
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void createSnapshotToleratesConflictWhenExistingSnapshotIsReady() throws Exception {
        Snapshot snapshot = new Snapshot().setName(SNAPSHOT_NAME);
        when(compute.disks().createSnapshot(PROJECT_ID, ZONE, DISK_NAME, snapshot).execute()).thenThrow(googleException(409, "already exists"));
        when(compute.snapshots().get(PROJECT_ID, SNAPSHOT_NAME).execute()).thenReturn(new Snapshot().setName(SNAPSHOT_NAME).setStatus("READY"));

        List<CloudResourceStatus> result = underTest.createSnapshot(snapshotParameters(), snapshot, gcpContext);

        assertTrue(result.isEmpty(), "A conflict on an already-READY snapshot is treated as success");
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void createSnapshotThrowsRetryableOnConflictWhenExistingSnapshotStillCreating() throws Exception {
        Snapshot snapshot = new Snapshot().setName(SNAPSHOT_NAME);
        when(compute.disks().createSnapshot(PROJECT_ID, ZONE, DISK_NAME, snapshot).execute()).thenThrow(googleException(409, "already exists"));
        when(compute.snapshots().get(PROJECT_ID, SNAPSHOT_NAME).execute()).thenReturn(new Snapshot().setName(SNAPSHOT_NAME).setStatus("CREATING"));

        assertThrows(CloudConnectorException.class, () -> underTest.createSnapshot(snapshotParameters(), snapshot, gcpContext));
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void createSnapshotSelfHealsConflictWhenExistingSnapshotFailed() throws Exception {
        Snapshot snapshot = new Snapshot().setName(SNAPSHOT_NAME);
        when(compute.disks().createSnapshot(PROJECT_ID, ZONE, DISK_NAME, snapshot).execute()).thenThrow(googleException(409, "already exists"));
        when(compute.snapshots().get(PROJECT_ID, SNAPSHOT_NAME).execute()).thenReturn(new Snapshot().setName(SNAPSHOT_NAME).setStatus("FAILED"));

        // a FAILED leftover is deleted (best-effort) and a retryable exception is thrown so the deterministically named
        // snapshot is recreated on the next @Retryable attempt, instead of failing the whole type change
        assertThrows(CloudConnectorException.class, () -> underTest.createSnapshot(snapshotParameters(), snapshot, gcpContext));
        verify(compute.snapshots().delete(PROJECT_ID, SNAPSHOT_NAME)).execute();
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void deleteSnapshotDeletesAndPollsGlobalOperation() throws Exception {
        when(compute.snapshots().delete(PROJECT_ID, SNAPSHOT_NAME).execute()).thenReturn(new Operation().setName("op-snap-del"));
        PollTask<List<CloudResourceStatus>> pollTask = mock(PollTask.class);
        when(statusCheckFactory.newPollResourceTask(eq(underTest), eq(authenticatedContext), any(), eq(gcpContext), anyBoolean()))
                .thenReturn(pollTask);

        underTest.deleteSnapshot(snapshotParameters(), gcpContext);

        verify(syncPollingScheduler).schedule(pollTask);
    }

    @Test
    void deleteSnapshotToleratesMissingSnapshot() throws Exception {
        when(compute.snapshots().delete(PROJECT_ID, SNAPSHOT_NAME).execute()).thenThrow(googleException(404, "Not Found"));

        List<CloudResourceStatus> result = underTest.deleteSnapshot(snapshotParameters(), gcpContext);

        assertTrue(result.isEmpty());
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void deleteSnapshotThrowsRetryableOnTransientError() throws Exception {
        when(compute.snapshots().delete(PROJECT_ID, SNAPSHOT_NAME).execute()).thenThrow(googleException(500, "Server Error"));

        assertThrows(CloudConnectorException.class, () -> underTest.deleteSnapshot(snapshotParameters(), gcpContext));
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void deleteSnapshotFailsFastOnClientError() throws Exception {
        when(compute.snapshots().delete(PROJECT_ID, SNAPSHOT_NAME).execute()).thenThrow(googleException(400, "Bad Request"));

        assertThrows(CloudbreakServiceException.class, () -> underTest.deleteSnapshot(snapshotParameters(), gcpContext));
        verify(syncPollingScheduler, never()).schedule(any());
    }

    @Test
    void resolveReturnsFullMigrationWhenNewDiskDoesNotExist() throws Exception {
        when(compute.disks().get(PROJECT_ID, ZONE, NEW_DISK_NAME).execute()).thenThrow(googleException(404, "Not Found"));

        DiskTypeChangeResumePoint result = underTest.resolveDiskTypeChangeResumePoint(newDiskParameters(), oldDiskParameters(), SNAPSHOT_NAME);

        assertEquals(ResumeAction.FULL_MIGRATION, result.action());
        assertTrue(result.existingNewDisk().isEmpty(), "No new disk exists yet, so none can be carried into the result");
    }

    @Test
    void resolveResumesAtDetachWhenNewDiskReadyUnattachedAndOldStillAttached() throws Exception {
        Disk newDisk = new Disk().setName(NEW_DISK_NAME).setStatus("READY").setSourceSnapshot(SOURCE_SNAPSHOT_URL).setSizeGb(150L);
        when(compute.disks().get(PROJECT_ID, ZONE, NEW_DISK_NAME).execute()).thenReturn(newDisk);
        Disk oldDisk = new Disk().setName(DISK_NAME).setUsers(List.of(INSTANCE_URL));
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(oldDisk);

        DiskTypeChangeResumePoint result = underTest.resolveDiskTypeChangeResumePoint(newDiskParameters(), oldDiskParameters(), SNAPSHOT_NAME);

        assertEquals(ResumeAction.RESUME_AT_DETACH, result.action());
        assertEquals(150L, result.existingNewDisk().orElseThrow().getSizeGb());
    }

    @Test
    void resolveResumesAtAttachWhenNewDiskReadyUnattachedAndOldAlreadyDetached() throws Exception {
        Disk newDisk = new Disk().setName(NEW_DISK_NAME).setStatus("READY").setSourceSnapshot(SOURCE_SNAPSHOT_URL).setSizeGb(150L);
        when(compute.disks().get(PROJECT_ID, ZONE, NEW_DISK_NAME).execute()).thenReturn(newDisk);
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenReturn(new Disk().setName(DISK_NAME));

        DiskTypeChangeResumePoint result = underTest.resolveDiskTypeChangeResumePoint(newDiskParameters(), oldDiskParameters(), SNAPSHOT_NAME);

        assertEquals(ResumeAction.RESUME_AT_ATTACH, result.action());
        assertEquals(150L, result.existingNewDisk().orElseThrow().getSizeGb());
    }

    @Test
    void resolveCleansUpWhenNewDiskAlreadyAttachedToTargetInstance() throws Exception {
        Disk newDisk = new Disk().setName(NEW_DISK_NAME).setStatus("READY").setSourceSnapshot(SOURCE_SNAPSHOT_URL).setSizeGb(150L)
                .setUsers(List.of(INSTANCE_URL));
        when(compute.disks().get(PROJECT_ID, ZONE, NEW_DISK_NAME).execute()).thenReturn(newDisk);

        DiskTypeChangeResumePoint result = underTest.resolveDiskTypeChangeResumePoint(newDiskParameters(), oldDiskParameters(), SNAPSHOT_NAME);

        assertEquals(ResumeAction.CLEANUP_ONLY, result.action());
        assertEquals(150L, result.existingNewDisk().orElseThrow().getSizeGb());
    }

    @Test
    void resolveRecreatesWhenUnattachedNewDiskSourceSnapshotDoesNotMatch() throws Exception {
        Disk newDisk = new Disk().setName(NEW_DISK_NAME).setStatus("READY").setSizeGb(150L)
                .setSourceSnapshot("https://www.googleapis.com/compute/v1/projects/test-project/global/snapshots/some-alien-snapshot");
        when(compute.disks().get(PROJECT_ID, ZONE, NEW_DISK_NAME).execute()).thenReturn(newDisk);

        DiskTypeChangeResumePoint result = underTest.resolveDiskTypeChangeResumePoint(newDiskParameters(), oldDiskParameters(), SNAPSHOT_NAME);

        assertEquals(ResumeAction.RECREATE_NEW_DISK, result.action(),
                "An unattached leftover not created from our snapshot is self-healed, not adopted");
        assertTrue(result.existingNewDisk().isPresent(), "The untrustworthy leftover disk is carried so the connector can delete it");
    }

    @Test
    void resolveFailsFastWhenNewDiskAttachedToDifferentInstance() throws Exception {
        Disk newDisk = new Disk().setName(NEW_DISK_NAME).setStatus("READY").setSourceSnapshot(SOURCE_SNAPSHOT_URL)
                .setUsers(List.of(INSTANCE_URL.replace(INSTANCE_ID, "otherInstance")));
        when(compute.disks().get(PROJECT_ID, ZONE, NEW_DISK_NAME).execute()).thenReturn(newDisk);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.resolveDiskTypeChangeResumePoint(newDiskParameters(), oldDiskParameters(), SNAPSHOT_NAME));

        assertTrue(exception.getMessage().contains("different instance"));
    }

    @Test
    void resolveRecreatesWhenNewDiskExistsButNotReady() throws Exception {
        Disk newDisk = new Disk().setName(NEW_DISK_NAME).setStatus("CREATING").setSourceSnapshot(SOURCE_SNAPSHOT_URL).setSizeGb(150L);
        when(compute.disks().get(PROJECT_ID, ZONE, NEW_DISK_NAME).execute()).thenReturn(newDisk);

        DiskTypeChangeResumePoint result = underTest.resolveDiskTypeChangeResumePoint(newDiskParameters(), oldDiskParameters(), SNAPSHOT_NAME);

        assertEquals(ResumeAction.RECREATE_NEW_DISK, result.action());
        assertTrue(result.existingNewDisk().isPresent(), "The unusable leftover disk is carried so the connector can delete it");
    }

    @Test
    void resolveResumesAtAttachWhenNewDiskReadyUnattachedAndOldDiskMissing() throws Exception {
        Disk newDisk = new Disk().setName(NEW_DISK_NAME).setStatus("READY").setSourceSnapshot(SOURCE_SNAPSHOT_URL).setSizeGb(150L);
        when(compute.disks().get(PROJECT_ID, ZONE, NEW_DISK_NAME).execute()).thenReturn(newDisk);
        when(compute.disks().get(PROJECT_ID, ZONE, DISK_NAME).execute()).thenThrow(googleException(404, "Not Found"));

        DiskTypeChangeResumePoint result = underTest.resolveDiskTypeChangeResumePoint(newDiskParameters(), oldDiskParameters(), SNAPSHOT_NAME);

        assertEquals(ResumeAction.RESUME_AT_ATTACH, result.action());
    }

    private GcpSnapshotParameters snapshotParameters() {
        return new GcpSnapshotParameters(compute, PROJECT_ID, ZONE, DISK_NAME, SNAPSHOT_NAME, cloudResource, authenticatedContext);
    }

    private GcpDiskAttachmentParameters parameters() {
        return new GcpDiskAttachmentParameters(compute, PROJECT_ID, ZONE, INSTANCE_ID, DISK_NAME, DISK_NAME, cloudResource, authenticatedContext);
    }

    private GcpDiskAttachmentParameters newDiskParameters() {
        return new GcpDiskAttachmentParameters(compute, PROJECT_ID, ZONE, INSTANCE_ID, NEW_DISK_NAME, NEW_DISK_NAME, cloudResource, authenticatedContext);
    }

    private GcpDiskAttachmentParameters oldDiskParameters() {
        return new GcpDiskAttachmentParameters(compute, PROJECT_ID, ZONE, INSTANCE_ID, DISK_NAME, DISK_NAME, cloudResource, authenticatedContext);
    }
}
