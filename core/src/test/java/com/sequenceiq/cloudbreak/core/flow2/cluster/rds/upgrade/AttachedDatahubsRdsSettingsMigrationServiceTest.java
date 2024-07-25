package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackIdViewImpl;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class AttachedDatahubsRdsSettingsMigrationServiceTest {
    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackService stackService;

    @Mock
    private RdsSettingsMigrationService rdsSettingsMigrationService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @InjectMocks
    private AttachedDatahubsRdsSettingsMigrationService underTest;

    @Test
    void testMigrateHappyPath() throws Exception {
        // GIVEN
        StackDto stackDto = mock(StackDto.class);
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setResourceCrn("crn");
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getResourceCrn()).thenReturn("crn");
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        Set<StackIdView> stackIds = new LinkedHashSet<>();
        stackIds.add(new StackIdViewImpl(2L, "name1", "crn1"));
        stackIds.add(new StackIdViewImpl(3L, "name2", "crn2"));
        stackIds.add(new StackIdViewImpl(4L, "name3", "crn3"));
        when(stackService.findNotTerminatedByDatalakeCrn("crn")).thenReturn(stackIds);
        List<Long> ids = List.of(2L, 3L, 4L);
        List<StackClusterStatusView> statuses = List.of(createStackClusterStatusView(2L, "name1", Status.AVAILABLE, Status.AVAILABLE),
                createStackClusterStatusView(3L, "name2", Status.AVAILABLE, Status.AVAILABLE),
                createStackClusterStatusView(4L, "name3", Status.AVAILABLE, Status.AVAILABLE));
        when(stackService.findStatusesByIds(ids)).thenReturn(statuses);
        Set<RDSConfig> rdsConfigs = Set.of(new RDSConfig());
        when(rdsSettingsMigrationService.collectRdsConfigs(anyLong(), any())).thenReturn(rdsConfigs);
        StackDto datahub = createDatahub(1L);
        when(stackDtoService.getById(2L)).thenReturn(datahub);
        when(stackDtoService.getById(3L)).thenReturn(datahub);
        when(stackDtoService.getById(4L)).thenReturn(datahub);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(isNull())).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.doesCMComponentExistsInBlueprint(anyString())).thenReturn(true);
        Table<String, String, String> cmConfigTable = HashBasedTable.create();
        when(rdsSettingsMigrationService.collectCMServiceConfigs(rdsConfigs)).thenReturn(cmConfigTable);
        // WHEN
        underTest.migrate(1L);
        // THEN
        verify(rdsSettingsMigrationService, times(3)).updateCMServiceConfigs(datahub, cmConfigTable, true);
        verify(cloudbreakEventService).fireCloudbreakEvent(1L, Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.CLUSTER_RDS_UPGRADE_ATTACHED_DATAHUBS_MIGRATE_DBSETTINGS_FINISHED, List.of("name1, name2, name3", ""));
    }

    @Test
    void testMigrateWithSkippedClusters() throws Exception {
        // GIVEN
        StackDto stackDto = mock(StackDto.class);
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setResourceCrn("crn");
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getResourceCrn()).thenReturn("crn");
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        Set<StackIdView> stackIds = new LinkedHashSet<>();
        stackIds.add(new StackIdViewImpl(2L, "name1", "crn1"));
        stackIds.add(new StackIdViewImpl(3L, "name2", "crn2"));
        stackIds.add(new StackIdViewImpl(4L, "name3", "crn3"));
        when(stackService.findNotTerminatedByDatalakeCrn("crn")).thenReturn(stackIds);
        List<Long> ids = List.of(2L, 3L, 4L);
        List<StackClusterStatusView> statuses = List.of(createStackClusterStatusView(2L, "name1", Status.AVAILABLE, Status.AVAILABLE),
                createStackClusterStatusView(3L, "name2", Status.STOPPED, Status.AVAILABLE),
                createStackClusterStatusView(4L, "name3", Status.AVAILABLE, Status.STOPPED));
        when(stackService.findStatusesByIds(ids)).thenReturn(statuses);
        Set<RDSConfig> rdsConfigs = Set.of(new RDSConfig());
        when(rdsSettingsMigrationService.collectRdsConfigs(anyLong(), any())).thenReturn(rdsConfigs);
        StackDto datahub = createDatahub(2L);
        when(stackDtoService.getById(2L)).thenReturn(datahub);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.doesCMComponentExistsInBlueprint(anyString())).thenReturn(true);
        Table<String, String, String> cmConfigTable = HashBasedTable.create();
        when(rdsSettingsMigrationService.collectCMServiceConfigs(rdsConfigs)).thenReturn(cmConfigTable);
        // WHEN
        underTest.migrate(1L);
        // THEN
        verify(rdsSettingsMigrationService, times(1)).updateCMServiceConfigs(datahub, cmConfigTable, true);
        verify(cloudbreakEventService).fireCloudbreakEvent(1L, Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.CLUSTER_RDS_UPGRADE_ATTACHED_DATAHUBS_MIGRATE_DBSETTINGS_FINISHED, List.of("name1",
                        "name2 [stackStatus: STOPPED, clusterStatus: AVAILABLE], name3 [stackStatus: AVAILABLE, clusterStatus: STOPPED]"));
    }

    @Test
    void testMigrateWithSkippedAndFailedAndNoUpdateNeededClusters() throws Exception {
        // GIVEN
        StackDto stackDto = mock(StackDto.class);
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setResourceCrn("crn");
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getResourceCrn()).thenReturn("crn");
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        Set<StackIdView> stackIds = new LinkedHashSet<>();
        stackIds.add(new StackIdViewImpl(2L, "name1", "crn1"));
        stackIds.add(new StackIdViewImpl(3L, "name2", "crn2"));
        stackIds.add(new StackIdViewImpl(4L, "name3", "crn3"));
        stackIds.add(new StackIdViewImpl(5L, "name4", "crn4"));
        when(stackService.findNotTerminatedByDatalakeCrn("crn")).thenReturn(stackIds);
        List<Long> ids = List.of(2L, 3L, 4L, 5L);
        List<StackClusterStatusView> statuses = List.of(createStackClusterStatusView(2L, "name1", Status.AVAILABLE, Status.AVAILABLE),
                createStackClusterStatusView(3L, "name2", Status.AVAILABLE, Status.AVAILABLE),
                createStackClusterStatusView(4L, "name3", Status.AVAILABLE, Status.STOPPED),
                createStackClusterStatusView(5L, "name4", Status.AVAILABLE, Status.AVAILABLE));
        when(stackService.findStatusesByIds(ids)).thenReturn(statuses);
        Set<RDSConfig> rdsConfigs = Set.of(new RDSConfig());
        when(rdsSettingsMigrationService.collectRdsConfigs(anyLong(), any())).thenReturn(rdsConfigs);
        StackDto datahub1 = createDatahub(2L);
        StackDto datahub2 = createDatahub(3L);
        StackDto datahub3 = createDatahub(5L);
        when(stackDtoService.getById(2L)).thenReturn(datahub1);
        when(stackDtoService.getById(3L)).thenReturn(datahub2);
        when(stackDtoService.getById(5L)).thenReturn(datahub3);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.doesCMComponentExistsInBlueprint(anyString())).thenReturn(true, true, false);
        Table<String, String, String> cmConfigTable = HashBasedTable.create();
        when(rdsSettingsMigrationService.collectCMServiceConfigs(rdsConfigs)).thenReturn(cmConfigTable);
        doNothing().when(rdsSettingsMigrationService).updateCMServiceConfigs(datahub1, cmConfigTable, true);
        Exception exception = new Exception("exception");
        doThrow(exception).when(rdsSettingsMigrationService).updateCMServiceConfigs(datahub2, cmConfigTable, true);
        // WHEN
        underTest.migrate(1L);
        // THEN
        verify(rdsSettingsMigrationService, times(2)).updateCMServiceConfigs(any(), eq(cmConfigTable), eq(true));
        verify(cloudbreakEventService).fireCloudbreakEvent(1L, Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.CLUSTER_RDS_UPGRADE_ATTACHED_DATAHUBS_MIGRATE_SETTINGS_FAILED, List.of("name1, name4",
                        "name3 [stackStatus: AVAILABLE, clusterStatus: STOPPED]", "name2: exception"));
    }

    private StackClusterStatusView createStackClusterStatusView(Long id, String name, Status stackStatus, Status clusterStatus) {
        StackClusterStatusView status = mock(StackClusterStatusView.class);
        lenient().when(status.getId()).thenReturn(id);
        lenient().when(status.getName()).thenReturn(name);
        lenient().when(status.getClusterStatus()).thenReturn(clusterStatus);
        lenient().when(status.getStatus()).thenReturn(stackStatus);
        lenient().when(status.getClusterId()).thenReturn(id);
        return status;
    }

    private StackDto createDatahub(Long id) {
        StackDto datahub = mock(StackDto.class);
        Blueprint blueprint = new Blueprint();
        ClusterView dhCluster = mock(ClusterView.class);
        lenient().when(dhCluster.getId()).thenReturn(1L);
        StackView dhStack = mock(StackView.class);
        lenient().when(dhStack.getId()).thenReturn(1L);
        lenient().when(datahub.getStack()).thenReturn(dhStack);
        lenient().when(datahub.getCluster()).thenReturn(dhCluster);
        lenient().when(datahub.getBlueprint()).thenReturn(blueprint);
        return datahub;
    }
}
