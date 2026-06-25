package com.sequenceiq.cloudbreak.cloud.gcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.DisksResizeRequest;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
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

    private static final String DISK_NAME = "i1v1";

    @InjectMocks
    private GcpDiskUpdateRetryService underTest;

    @Mock
    private ResourcePollTaskFactory statusCheckFactory;

    @Mock
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Mock
    private Compute compute;

    @Mock
    private Compute.Disks disks;

    @Mock
    private GcpContext gcpContext;

    @Mock
    private AuthenticatedContext authenticatedContext;

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
    void testCheckResourcesDelegatesToAttachedDiskSetCheck() {
        List<CloudResource> resources = List.of();

        List<CloudResourceStatus> result = underTest.checkResources(gcpContext, authenticatedContext, resources);

        assertEquals(0, result.size());
    }

    private CloudResource createDiskResource() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("key", "val");
        return CloudResource.builder()
                .withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withAvailabilityZone(ZONE)
                .withParameters(parameters)
                .build();
    }
}
