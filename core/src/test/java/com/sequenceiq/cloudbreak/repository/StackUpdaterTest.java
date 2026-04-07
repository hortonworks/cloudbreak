package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateProperties;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeMetadata;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeProperty;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.notification.StackNotificationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.UsageLoggingUtil;

@ExtendWith(MockitoExtension.class)
public class StackUpdaterTest {

    @Mock
    private StackService stackService;

    @Mock
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Mock
    private UsageLoggingUtil usageLoggingUtil;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackNotificationService stackNotificationAssembler;

    @Mock
    private InstanceMetadataUpdateProperties instanceMetadataUpdateProperties;

    @InjectMocks
    private StackUpdater underTest;

    @Test
    public void skipStackStatusUpdateWhenActualStatusEqualsNewStatus() {
        Stack stack = TestUtil.stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));

        DetailedStackStatus newStatus = DetailedStackStatus.AVAILABLE;
        when(stackService.getByIdWithTransaction(anyLong())).thenReturn(stack);

        Stack modifiedStack = underTest.updateStackStatus(1L, newStatus, "newReason");
        assertEquals(stack.getStatus(), modifiedStack.getStatus());
        assertEquals(newStatus.getStatus(), modifiedStack.getStatus());
        verify(stackService, never()).save(any());
    }

    @Test
    public void skipStackStatusUpdateWhenStatusIsDeleteCompleted() {
        Stack stack = TestUtil.stack(Status.DELETE_COMPLETED);

        DetailedStackStatus newStatus = DetailedStackStatus.AVAILABLE;
        when(stackService.getByIdWithTransaction(anyLong())).thenReturn(stack);

        Stack modifiedStack = underTest.updateStackStatus(1L, newStatus, "newReason");
        assertEquals(Status.DELETE_COMPLETED, modifiedStack.getStatus());
        verify(stackService, never()).save(any());
    }

    @Test
    public void updateStackStatusAndReason() {
        Stack stack = TestUtil.stack(TestUtil.cluster());

        DetailedStackStatus newStatus = DetailedStackStatus.DELETE_COMPLETED;
        String newStatusReason = "test";
        when(stackService.getByIdWithTransaction(anyLong())).thenReturn(stack);
        when(stackService.save(any(Stack.class))).thenReturn(stack);

        Stack newStack = underTest.updateStackStatus(1L, newStatus, newStatusReason);
        assertEquals(newStatus.getStatus(), newStack.getStatus());
        assertEquals(newStatusReason, newStack.getStatusReason());
        verify(stackService, times(1)).save(eq(stack));
        verify(clusterService, times(1)).save(eq(stack.getCluster()));
    }

    @Test
    void testUpdateImdsVersionIfMatchingVersion() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setSupportedImdsVersion("v2");
        when(stackService.get(any())).thenReturn(stack);
        mockImdsUpdateTypes();

        underTest.updateSupportedImdsVersionIfNecessary(1L, InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED);

        verify(stackService, times(0)).save(any());
    }

    @Test
    void testUpdateImdsVersion() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setSupportedImdsVersion("v1");
        when(stackService.get(any())).thenReturn(stack);
        mockImdsUpdateTypes();
        when(stackService.save(any())).thenReturn(stack);

        underTest.updateSupportedImdsVersionIfNecessary(1L, InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED);

        verify(stackService).save(any());
    }

    private void mockImdsUpdateTypes() {
        InstanceMetadataUpdateTypeMetadata metadataV2 = new InstanceMetadataUpdateTypeMetadata("v2");
        InstanceMetadataUpdateTypeProperty propertyV2 = new InstanceMetadataUpdateTypeProperty("AWS", Map.of(AWS, metadataV2));
        when(instanceMetadataUpdateProperties.getTypes()).thenReturn(Map.of(InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED, propertyV2));
    }

}
