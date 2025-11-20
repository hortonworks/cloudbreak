package com.sequenceiq.cloudbreak.job.diskusage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class DiskUsageSyncServiceTest {

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:1234:cluster:5678";

    private static final String PRIMARY_GW_FQDN = "pgw.example.com";

    private static final String PRIMARY_GW_GROUP_NAME = "master";

    private static final long STACK_ID = 1L;

    private static final int DB_DISK_USAGE_THRESHOLD = 80;

    private static final int DISK_INCREMENT_SIZE = 100;

    private static final int MAX_DISK_SIZE = 2000;

    @Mock
    private DiskUsageSyncConfig diskUsageSyncConfig;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @InjectMocks
    private DiskUsageSyncService underTest;

    @Mock
    private StackDto stack;

    @Mock
    private Database database;

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private InstanceGroupView instanceGroupView;

    @Mock
    private Template template;

    @BeforeEach
    void setUp() throws CloudbreakOrchestratorFailedException {
        lenient().when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getDatabase()).thenReturn(database);
        lenient().when(diskUsageSyncConfig.getDbDiskUsageThresholdPercentage()).thenReturn(DB_DISK_USAGE_THRESHOLD);
        lenient().when(diskUsageSyncConfig.getDiskIncrementSize()).thenReturn(DISK_INCREMENT_SIZE);
        lenient().when(diskUsageSyncConfig.getMaxDiskSize()).thenReturn(MAX_DISK_SIZE);

        lenient().when(stack.getPrimaryGatewayFQDN()).thenReturn(Optional.of(PRIMARY_GW_FQDN));
        lenient().when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        lenient().when(hostOrchestrator.getDatabaseDiskUsagePercentage(gatewayConfig, PRIMARY_GW_FQDN)).thenReturn(Optional.of(50));
    }

    @Test
    @DisplayName("Test that no resize is triggered when disk usage is below threshold")
    void testNoResizeWhenUsageBelowThreshold() throws CloudbreakOrchestratorFailedException {
        when(hostOrchestrator.getDatabaseDiskUsagePercentage(gatewayConfig, PRIMARY_GW_FQDN)).thenReturn(Optional.of(DB_DISK_USAGE_THRESHOLD - 1));

        underTest.checkDbDisk(stack);

        verify(flowManager, never()).triggerStackUpdateDisks(any(), any());
    }

    @Test
    @DisplayName("Test that resize is triggered when disk usage is at threshold")
    void testResizeWhenUsageAtThreshold() throws CloudbreakOrchestratorFailedException {
        setupForResize(DB_DISK_USAGE_THRESHOLD, 200);

        underTest.checkDbDisk(stack);

        ArgumentCaptor<DiskUpdateRequest> captor = ArgumentCaptor.forClass(DiskUpdateRequest.class);
        verify(flowManager, times(1)).triggerStackUpdateDisks(eq(stack), captor.capture());
        assert captor.getValue().getSize() == 200 + DISK_INCREMENT_SIZE;
    }

    @Test
    @DisplayName("Test that resize is triggered when disk usage is above threshold")
    void testResizeWhenUsageAboveThreshold() throws CloudbreakOrchestratorFailedException {
        setupForResize(DB_DISK_USAGE_THRESHOLD + 1, 200);

        underTest.checkDbDisk(stack);

        ArgumentCaptor<DiskUpdateRequest> captor = ArgumentCaptor.forClass(DiskUpdateRequest.class);
        verify(flowManager, times(1)).triggerStackUpdateDisks(eq(stack), captor.capture());
        assert captor.getValue().getSize() == 200 + DISK_INCREMENT_SIZE;
    }

    @Test
    @DisplayName("Test that resize respects max disk size")
    void testResizeRespectsMaxDiskSize() throws CloudbreakOrchestratorFailedException {
        setupForResize(DB_DISK_USAGE_THRESHOLD, MAX_DISK_SIZE - 50);

        underTest.checkDbDisk(stack);

        ArgumentCaptor<DiskUpdateRequest> captor = ArgumentCaptor.forClass(DiskUpdateRequest.class);
        verify(flowManager, times(1)).triggerStackUpdateDisks(eq(stack), captor.capture());
        assert captor.getValue().getSize() == MAX_DISK_SIZE;
    }

    @Test
    @DisplayName("Test that resize is not triggered if DB volume template is not found")
    void testNoResizeWhenDbVolumeNotFound() throws CloudbreakOrchestratorFailedException {
        when(hostOrchestrator.getDatabaseDiskUsagePercentage(gatewayConfig, PRIMARY_GW_FQDN)).thenReturn(Optional.of(DB_DISK_USAGE_THRESHOLD));
        when(stack.getPrimaryGatewayGroup()).thenReturn(instanceGroupView);
        when(instanceGroupView.getTemplate()).thenReturn(template);
        when(template.getVolumeTemplates()).thenReturn(Set.of());

        underTest.checkDbDisk(stack);

        verify(flowManager, never()).triggerStackUpdateDisks(any(), any());
    }

    @Test
    @DisplayName("Test that checkDbDisk handles orchestrator failure gracefully")
    void testCheckDbDiskHandlesOrchestratorFailure() throws CloudbreakOrchestratorFailedException {
        when(hostOrchestrator.getDatabaseDiskUsagePercentage(gatewayConfig, PRIMARY_GW_FQDN)).thenThrow(new CloudbreakOrchestratorFailedException("Error"));

        assertDoesNotThrow(() -> underTest.checkDbDisk(stack));
        verify(flowManager, never()).triggerStackUpdateDisks(any(), any());
    }

    @Test
    @DisplayName("Test that checkDbDisk handles missing primary gateway FQDN")
    void testCheckDbDiskHandlesMissingPgwFqdn() throws CloudbreakOrchestratorFailedException {
        when(stack.getPrimaryGatewayFQDN()).thenReturn(Optional.empty());

        underTest.checkDbDisk(stack);

        verify(hostOrchestrator, never()).getDatabaseDiskUsagePercentage(any(), any());
    }

    @Test
    @DisplayName("Test that resize event is logged correctly")
    void testResizeEventIsLogged() throws CloudbreakOrchestratorFailedException {
        int currentSize = 200;
        setupForResize(DB_DISK_USAGE_THRESHOLD, currentSize);

        underTest.checkDbDisk(stack);

        verify(flowMessageService, times(1)).fireEventAndLog(
                eq(STACK_ID),
                anyString(),
                any(),
                eq(String.valueOf(DB_DISK_USAGE_THRESHOLD)),
                eq(String.valueOf(currentSize + DISK_INCREMENT_SIZE))
        );
    }

    private void setupForResize(int usage, int currentSize) throws CloudbreakOrchestratorFailedException {
        when(hostOrchestrator.getDatabaseDiskUsagePercentage(gatewayConfig, PRIMARY_GW_FQDN)).thenReturn(Optional.of(usage));

        VolumeTemplate dbVolume = new VolumeTemplate();
        dbVolume.setVolumeSize(currentSize);
        dbVolume.setUsageType(VolumeUsageType.DATABASE);

        when(stack.getPrimaryGatewayGroup()).thenReturn(instanceGroupView);
        when(instanceGroupView.getGroupName()).thenReturn(PRIMARY_GW_GROUP_NAME);
        when(instanceGroupView.getTemplate()).thenReturn(template);
        when(template.getVolumeTemplates()).thenReturn(Set.of(dbVolume));
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        lenient().when(flowManager.triggerStackUpdateDisks(any(), any())).thenReturn(flowIdentifier);
        lenient().doNothing().when(flowMessageService).fireEventAndLog(anyLong(), anyString(), any(), anyString(), anyString());
    }
}
