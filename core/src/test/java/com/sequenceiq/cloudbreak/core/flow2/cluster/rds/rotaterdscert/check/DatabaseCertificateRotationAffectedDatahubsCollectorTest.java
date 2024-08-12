package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class DatabaseCertificateRotationAffectedDatahubsCollectorTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ENVIRONMENT_CRN = "someEnvCrn";

    private static final String CLUSTER_CRN = "someClusterCrn";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackService stackService;

    @Mock
    private WorkspaceService workspaceService;

    private DatabaseCertificateRotationAffectedDatahubsCollector underTest;

    @BeforeEach
    void setUp() {
        underTest = new DatabaseCertificateRotationAffectedDatahubsCollector(stackDtoService, stackService, workspaceService);
    }

    @Test
    @DisplayName("Test check if there is no datahub for the environment")
    void testIsDatahubCertCheckNecessaryNoDatahubForEnvironment() {
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(any(), anyList())).thenReturn(List.of());
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        List<String> result = underTest.collectDatahubNamesWhereCertCheckNecessary(ENVIRONMENT_CRN);

        assertTrue(result.isEmpty());
        verify(stackDtoService, times(1)).findAllByEnvironmentCrnAndStackType(any(), anyList());
        verifyNoMoreInteractions(stackDtoService);
    }

    @Test
    @DisplayName("Test check if there is one datahub for the environment and it has no Hive Metastore and external db null")
    void testIsDatahubCertCheckNecessaryOneDhNoHiveNullDb() {
        List<StackDto> stackViews = new ArrayList<>();
        StackDto stackView = mock(StackDto.class);
        when(stackView.getResourceCrn()).thenReturn(CLUSTER_CRN);
        stackViews.add(stackView);
        Stack stack = new Stack();
        stack.setCluster(new Cluster());
        stack.setResourceCrn(CLUSTER_CRN);
        stack.getCluster().setDatabaseServerCrn(null);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(any(), anyList())).thenReturn(stackViews);
        when(stackService.getByCrn(any())).thenReturn(stack);

        List<String> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.collectDatahubNamesWhereCertCheckNecessary(ENVIRONMENT_CRN));

        assertTrue(result.isEmpty());
        verify(stackDtoService, times(1)).findAllByEnvironmentCrnAndStackType(any(), anyList());
        verify(stackService, times(1)).getByCrn(any());
        verifyNoMoreInteractions(stackService);
    }

    @Test
    @DisplayName("Test check if there is one datahub for the environment and it has no Hive Metastore and no external db")
    void testIsDatahubCertCheckNecessaryOneDhNoHiveEmbedded() {
        List<StackDto> stackViews = new ArrayList<>();
        StackDto stackView = mock(StackDto.class);
        when(stackView.getResourceCrn()).thenReturn(CLUSTER_CRN);
        stackViews.add(stackView);
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        stack.setCluster(new Cluster());
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NONE);
        stack.setDatabase(database);

        when(stackDtoService.findAllByEnvironmentCrnAndStackType(any(), anyList())).thenReturn(stackViews);
        when(stackService.getByCrn(any())).thenReturn(stack);

        List<String> result = underTest.collectDatahubNamesWhereCertCheckNecessary(ENVIRONMENT_CRN);

        assertTrue(result.isEmpty());
        verify(stackDtoService, times(1)).findAllByEnvironmentCrnAndStackType(any(), anyList());
        verify(stackService, times(1)).getByCrn(any());
        verifyNoMoreInteractions(stackDtoService);
    }

    @Test
    @DisplayName("Test check if there is one datahub for the environment and it has no Hive Metastore but has external db")
    void testIsDatahubCertCheckNecessaryOneDhNoHiveNotEmbedded() {
        List<StackDto> stackViews = new ArrayList<>();
        StackDto stackView = mock(StackDto.class);
        when(stackView.getResourceCrn()).thenReturn(CLUSTER_CRN);
        stackViews.add(stackView);
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        stack.setName(CLUSTER_CRN);
        stack.setCluster(new Cluster());
        Database database = mock(Database.class);
        DatabaseAvailabilityType availabilityType = mock(DatabaseAvailabilityType.class);
        when(database.getExternalDatabaseAvailabilityType()).thenReturn(availabilityType);
        when(availabilityType.isEmbedded()).thenReturn(false);
        stack.setDatabase(database);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(any(), anyList())).thenReturn(stackViews);
        when(stackService.getByCrn(any())).thenReturn(stack);

        List<String> result = underTest.collectDatahubNamesWhereCertCheckNecessary(ENVIRONMENT_CRN);

        assertFalse(result.isEmpty());
        assertTrue(result.contains(CLUSTER_CRN));
        assertTrue(result.size() == 1);
        verify(stackDtoService, times(1)).findAllByEnvironmentCrnAndStackType(any(), anyList());
        verify(stackService, times(1)).getByCrn(any());
        verifyNoMoreInteractions(stackDtoService);
    }

    @Test
    @DisplayName("Test check if there is one datahub for the environment and it has empty blueprint and no external db")
    void testIsDatahubCertCheckNecessaryOneDhEmptyBpEmbedded() {
        List<StackDto> stackViews = new ArrayList<>();
        StackDto stackView = mock(StackDto.class);
        when(stackView.getResourceCrn()).thenReturn(CLUSTER_CRN);
        stackViews.add(stackView);
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        Cluster cluster = new Cluster();
        cluster.setBlueprint(new Blueprint());
        stack.setCluster(cluster);
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NONE);
        stack.setDatabase(database);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(any(), anyList())).thenReturn(stackViews);
        when(stackService.getByCrn(any())).thenReturn(stack);

        List<String> result = underTest.collectDatahubNamesWhereCertCheckNecessary(ENVIRONMENT_CRN);

        assertTrue(result.isEmpty());
        verify(stackDtoService, times(1)).findAllByEnvironmentCrnAndStackType(any(), anyList());
        verify(stackService, times(1)).getByCrn(any());
        verifyNoMoreInteractions(stackDtoService);
    }

    @Test
    @DisplayName("Test check if there is one datahub for the environment and it has blueprint with HIVEMETASTORE and no external db")
    void testIsDatahubCertCheckNecessaryOneDhHiveEmbedded() {
        List<StackDto> stackViews = new ArrayList<>();
        StackDto stackView = mock(StackDto.class);
        when(stackView.getResourceCrn()).thenReturn(CLUSTER_CRN);
        stackViews.add(stackView);
        Stack stack = new Stack();
        stack.setName(CLUSTER_CRN);
        stack.setResourceCrn(CLUSTER_CRN);
        Cluster cluster = new Cluster();
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("HIVEMETASTORE");
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NONE);
        stack.setDatabase(database);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(any(), anyList())).thenReturn(stackViews);
        when(stackService.getByCrn(any())).thenReturn(stack);

        List<String> result = underTest.collectDatahubNamesWhereCertCheckNecessary(ENVIRONMENT_CRN);

        assertFalse(result.isEmpty());
        assertTrue(result.size() == 1);
        assertTrue(result.get(0).equals(CLUSTER_CRN));
        verify(stackDtoService, times(1)).findAllByEnvironmentCrnAndStackType(any(), anyList());
        verify(stackService, times(1)).getByCrn(any());
        verifyNoMoreInteractions(stackDtoService);
    }

    @Test
    @DisplayName("Test check if there is one datahub for the environment and it has no hive metastore but external db does")
    void testIsDatahubCertCheckNecessaryOneDhHiveNotEmbedded() {
        List<StackDto> stackViews = new ArrayList<>();
        StackDto stackView = mock(StackDto.class);
        when(stackView.getResourceCrn()).thenReturn(CLUSTER_CRN);
        stackViews.add(stackView);
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        stack.setName(CLUSTER_CRN);
        Cluster cluster = new Cluster();
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("HIVEMETASTORE");
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        Database database = mock(Database.class);
        DatabaseAvailabilityType availabilityType = mock(DatabaseAvailabilityType.class);
        lenient().when(database.getExternalDatabaseAvailabilityType()).thenReturn(availabilityType);
        lenient().when(availabilityType.isEmbedded()).thenReturn(false);
        stack.setDatabase(database);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(any(), anyList())).thenReturn(stackViews);
        when(stackService.getByCrn(any())).thenReturn(stack);

        List<String> result = underTest.collectDatahubNamesWhereCertCheckNecessary(ENVIRONMENT_CRN);

        assertFalse(result.isEmpty());
        assertTrue(result.size() == 1);
        assertTrue(result.get(0).equals(CLUSTER_CRN));
        verify(stackDtoService, times(1)).findAllByEnvironmentCrnAndStackType(any(), anyList());
        verify(stackService, times(1)).getByCrn(any());
        verifyNoMoreInteractions(stackDtoService);
    }

    @Test
    @DisplayName("Test check if there are two datahubs for the environment and none has Hive Metastore and external db")
    void testIsDatahubCertCheckNecessaryTwoDhNoHiveNullDb() {
        List<StackDto> stackViews = new ArrayList<>();
        StackDto stackView1 = mock(StackDto.class);
        when(stackView1.getResourceCrn()).thenReturn(CLUSTER_CRN + '1');
        StackDto stackView2 = mock(StackDto.class);
        when(stackView2.getResourceCrn()).thenReturn(CLUSTER_CRN + '2');
        stackViews.addAll(Set.of(stackView1, stackView2));
        Stack stack1 = new Stack();
        stack1.setResourceCrn(CLUSTER_CRN + '1');
        stack1.setCluster(new Cluster());
        Stack stack2 = new Stack();
        stack2.setResourceCrn(CLUSTER_CRN + '2');
        stack2.setCluster(new Cluster());
        stack2.setDatabase(null);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(any(), anyList())).thenReturn(stackViews);
        when(stackService.getByCrn(stackView1.getResourceCrn())).thenReturn(stack1);
        when(stackService.getByCrn(stackView2.getResourceCrn())).thenReturn(stack2);

        List<String> result = underTest.collectDatahubNamesWhereCertCheckNecessary(ENVIRONMENT_CRN);

        assertTrue(result.isEmpty());
        verify(stackDtoService, times(1)).findAllByEnvironmentCrnAndStackType(any(), anyList());
        verify(stackService, times(1)).getByCrn(stackView1.getResourceCrn());
        verify(stackService, times(1)).getByCrn(stackView2.getResourceCrn());
        verifyNoMoreInteractions(stackDtoService);
    }

    @Test
    @DisplayName("Test check if there are two datahubs for the environment and one has Hive Metastore and other no external db")
    void testIsDatahubCertCheckNecessaryTwoDhOneHiveNullDb() {
        List<StackDto> stackViews = new ArrayList<>();
        StackDto stackView1 = mock(StackDto.class);
        when(stackView1.getResourceCrn()).thenReturn(CLUSTER_CRN + '1');
        StackDto stackView2 = mock(StackDto.class);
        when(stackView2.getResourceCrn()).thenReturn(CLUSTER_CRN + '2');
        stackViews.addAll(Set.of(stackView1, stackView2));
        Stack stack1 = new Stack();
        stack1.setResourceCrn(CLUSTER_CRN + '1');
        stack1.setName(CLUSTER_CRN + '1');
        Blueprint blueprint = new Blueprint();
        Cluster cluster = new Cluster();
        blueprint.setBlueprintText("HIVEMETASTORE");
        cluster.setBlueprint(blueprint);
        stack1.setCluster(cluster);
        Stack stack2 = new Stack();
        stack2.setResourceCrn(CLUSTER_CRN + '2');
        stack2.setName(CLUSTER_CRN + '2');
        stack2.setCluster(new Cluster());
        stack2.setDatabase(null);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(any(), anyList())).thenReturn(stackViews);
        when(stackService.getByCrn(stackView1.getResourceCrn())).thenReturn(stack1);
        when(stackService.getByCrn(stackView2.getResourceCrn())).thenReturn(stack2);

        List<String> result = underTest.collectDatahubNamesWhereCertCheckNecessary(ENVIRONMENT_CRN);

        assertFalse(result.isEmpty());
        assertTrue(result.size() == 1);
        assertTrue(result.get(0).equals(CLUSTER_CRN + '1'));
        verify(stackDtoService, times(1)).findAllByEnvironmentCrnAndStackType(any(), anyList());
        verify(stackService, times(1)).getByCrn(stackView1.getResourceCrn());
        verify(stackService, times(1)).getByCrn(stackView2.getResourceCrn());
        verifyNoMoreInteractions(stackDtoService);
    }

}