package com.sequenceiq.cloudbreak.core.flow2.chain.util;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class RepairServiceRestartDeciderTest {

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 2L;

    private static final String TRIGGERED_VARIANT = "AWS_NATIVE";

    private static final Map<String, List<String>> COD_HOST_GROUPS = Map.of(
            "master", List.of("GATEWAY", "NAMENODE", "MASTER", "RESOURCEMANAGER", "SERVER", "FAILOVERCONTROLLER", "JOURNALNODE"),
            "leader", List.of("GATEWAY", "SERVER", "JOURNALNODE", "HBASERESTSERVER", "HBASETHRIFTSERVER", "JOBHISTORY"),
            "gateway", List.of("GATEWAY", "KNOX_GATEWAY", "HUE_SERVER", "HUE_LOAD_BALANCER"),
            "worker", List.of("GATEWAY", "DATANODE", "NODEMANAGER", "REGIONSERVER", "PHOENIX_QUERY_SERVER"),
            "strongmeta", List.of("GATEWAY", "REGIONSERVER"),
            "restworker", List.of("GATEWAY", "HBASERESTSERVER"),
            "edge", List.of("GATEWAY"));

    private static final Map<String, List<String>> DATA_ENGINEERING_HOST_GROUPS = Map.of(
            "manager", List.of("GATEWAY", "KNOX_GATEWAY", "OOZIE_SERVER"),
            "master", List.of("GATEWAY", "NAMENODE", "HIVEMETASTORE", "HIVESERVER2", "RESOURCEMANAGER", "SERVER"),
            "worker", List.of("GATEWAY", "DATANODE", "NODEMANAGER"),
            "compute", List.of("GATEWAY", "NODEMANAGER"),
            "gateway", List.of("GATEWAY"));

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @InjectMocks
    private RepairServiceRestartDecider underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "skipWorkerRestartEnabled", true);
        lenient().when(stackDto.getCluster()).thenReturn(clusterView);
        lenient().when(clusterView.getId()).thenReturn(CLUSTER_ID);
    }

    @Test
    void testRestartServicesNotNeededWhenNotRequestedByTheRepairEvent() {
        boolean result = restartServicesNeeded(upgradeRepairEvent(false), Set.of("worker"));

        assertFalse(result);
        verifyNoInteractions(clusterComponentConfigProvider, cmTemplateProcessorFactory);
    }

    @Test
    void testRestartServicesNeededWhenSkipIsDisabled() {
        ReflectionTestUtils.setField(underTest, "skipWorkerRestartEnabled", false);

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("worker"));

        assertTrue(result);
        verifyNoInteractions(clusterComponentConfigProvider, cmTemplateProcessorFactory);
    }

    @Test
    void testRestartServicesFlagIsKeptWithoutClassificationForRollingRestart() {
        ClusterRepairTriggerEvent event = new ClusterRepairTriggerEvent(STACK_ID, Map.of("worker", List.of("worker0")), RepairType.ONE_BY_ONE, true,
                TRIGGERED_VARIANT, true, true);

        boolean result = restartServicesNeeded(event, Set.of("worker"));

        assertTrue(result);
        verifyNoInteractions(clusterComponentConfigProvider, cmTemplateProcessorFactory);
    }

    @Test
    void testRestartServicesNeededWhenRepairIsNotPartOfAnUpgrade() {
        ClusterRepairTriggerEvent event = new ClusterRepairTriggerEvent(STACK_ID, Map.of("worker", List.of("worker0")), RepairType.BATCH, true, null);

        boolean result = restartServicesNeeded(event, Set.of("worker"));

        assertTrue(result);
        verifyNoInteractions(clusterComponentConfigProvider, cmTemplateProcessorFactory);
    }

    @Test
    void testRestartServicesNeededWhenClouderaManagerIsOlderThan790() {
        setUpClouderaManagerVersion("7.8.1");

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("worker"));

        assertTrue(result);
        verifyNoInteractions(cmTemplateProcessorFactory);
    }

    @Test
    void testRestartServicesNeededWhenClouderaManagerRepoIsMissing() {
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(null);

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("worker"));

        assertTrue(result);
        verifyNoInteractions(cmTemplateProcessorFactory);
    }

    @Test
    void testRestartServicesNotNeededForCodWorkerBatch() {
        setUpClouderaManagerVersion("7.13.1");
        setUpDeployedTemplate(COD_HOST_GROUPS);

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("worker"));

        assertFalse(result);
    }

    @Test
    void testRestartServicesNotNeededForCodWorkerAndStrongMetaAndEdgeBatch() {
        setUpClouderaManagerVersion("7.9.0");
        setUpDeployedTemplate(COD_HOST_GROUPS);

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("worker", "strongmeta", "edge"));

        assertFalse(result);
    }

    @Test
    void testRestartServicesNeededForCodBatchContainingMaster() {
        setUpClouderaManagerVersion("7.13.1");
        setUpDeployedTemplate(COD_HOST_GROUPS);

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("worker", "master"));

        assertTrue(result);
    }

    @Test
    void testRestartServicesNeededForCodBatchContainingLeader() {
        setUpClouderaManagerVersion("7.13.1");
        setUpDeployedTemplate(COD_HOST_GROUPS);

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("leader"));

        assertTrue(result);
    }

    @Test
    void testRestartServicesNeededForCodBatchContainingRestWorker() {
        setUpClouderaManagerVersion("7.13.1");
        setUpDeployedTemplate(COD_HOST_GROUPS);

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("restworker"));

        assertTrue(result);
    }

    @Test
    void testRestartServicesNotNeededForDataEngineeringWorkerAndComputeAndGatewayBatch() {
        setUpClouderaManagerVersion("7.11.0");
        setUpDeployedTemplate(DATA_ENGINEERING_HOST_GROUPS);

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("worker", "compute", "gateway"));

        assertFalse(result);
    }

    @Test
    void testRestartServicesNeededForDataEngineeringBatchContainingManager() {
        setUpClouderaManagerVersion("7.11.0");
        setUpDeployedTemplate(DATA_ENGINEERING_HOST_GROUPS);

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("compute", "manager"));

        assertTrue(result);
    }

    @Test
    void testRestartServicesNeededWhenHostGroupIsMissingFromTheDeployedTemplate() {
        setUpClouderaManagerVersion("7.13.1");
        setUpDeployedTemplate(DATA_ENGINEERING_HOST_GROUPS);

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("worker", "customgroup"));

        assertTrue(result);
    }

    @Test
    void testRestartServicesNeededWhenThereIsNoDeployedTemplate() {
        setUpClouderaManagerVersion("7.13.1");
        when(stackDto.getBlueprintJsonText()).thenReturn(null);

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("worker"));

        assertTrue(result);
        verifyNoInteractions(cmTemplateProcessorFactory);
    }

    @Test
    void testRestartServicesNeededWhenDeployedTemplateIsBlank() {
        setUpClouderaManagerVersion("7.13.1");
        when(stackDto.getBlueprintJsonText()).thenReturn(" ");

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("worker"));

        assertTrue(result);
        verifyNoInteractions(cmTemplateProcessorFactory);
    }

    @Test
    void testRestartServicesNeededWhenTheDeployedTemplateAddsANonWorkerRoleToTheWorkerHostGroup() {
        setUpClouderaManagerVersion("7.13.1");
        setUpDeployedTemplate(Map.of("worker", List.of("GATEWAY", "DATANODE", "NODEMANAGER", "IMPALAD")));

        boolean result = restartServicesNeeded(upgradeRepairEvent(true), Set.of("worker"));

        assertTrue(result);
        verify(stackDto, never()).getBlueprint();
    }

    @Test
    void testClouderaManagerRepoAndDeployedTemplateAreReadOnceForEveryBatchOfTheChain() {
        setUpClouderaManagerVersion("7.13.1");
        setUpDeployedTemplate(COD_HOST_GROUPS);

        RepairServiceRestartPolicy restartPolicy = underTest.restartPolicyFor(upgradeRepairEvent(true), stackDto);
        List<Boolean> restartServicesByBatch = Stream.of(Set.of("leader", "gateway"), Set.of("master"), Set.of("worker"), Set.of("worker"))
                .map(restartPolicy::restartServicesNeeded)
                .toList();

        assertEquals(List.of(true, true, false, false), restartServicesByBatch);
        verify(clusterComponentConfigProvider, times(1)).getClouderaManagerRepoDetails(CLUSTER_ID);
        verify(cmTemplateProcessorFactory, times(1)).get(anyString());
    }

    private boolean restartServicesNeeded(ClusterRepairTriggerEvent event, Set<String> repairedHostGroups) {
        return underTest.restartPolicyFor(event, stackDto).restartServicesNeeded(repairedHostGroups);
    }

    private ClusterRepairTriggerEvent upgradeRepairEvent(boolean restartServices) {
        return new ClusterRepairTriggerEvent(STACK_ID, Map.of("worker", List.of("worker0")), RepairType.BATCH, restartServices, TRIGGERED_VARIANT);
    }

    private void setUpClouderaManagerVersion(String version) {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion(version);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
    }

    private void setUpDeployedTemplate(Map<String, List<String>> rolesByHostGroup) {
        String cmTemplateText = cmTemplateText(rolesByHostGroup);
        when(stackDto.getBlueprintJsonText()).thenReturn(cmTemplateText);
        when(cmTemplateProcessorFactory.get(cmTemplateText)).thenReturn(new CmTemplateProcessor(cmTemplateText));
    }

    private String cmTemplateText(Map<String, List<String>> rolesByHostGroup) {
        String roleConfigGroups = rolesByHostGroup.values().stream()
                .flatMap(List::stream)
                .collect(toSet())
                .stream()
                .map(role -> String.format("{\"refName\": \"rcg-%s\", \"roleType\": \"%s\", \"base\": true}", role, role))
                .collect(joining(", "));
        String hostTemplates = rolesByHostGroup.entrySet().stream()
                .map(hostGroup -> String.format("{\"refName\": \"%s\", \"cardinality\": 1, \"roleConfigGroupsRefNames\": [%s]}",
                        hostGroup.getKey(),
                        hostGroup.getValue().stream().map(role -> String.format("\"rcg-%s\"", role)).collect(joining(", "))))
                .collect(joining(", "));
        return String.format("{\"cdhVersion\": \"7.2.18\", \"displayName\": \"test\", "
                        + "\"services\": [{\"refName\": \"service\", \"serviceType\": \"SERVICE\", \"roleConfigGroups\": [%s]}], "
                        + "\"hostTemplates\": [%s]}",
                roleConfigGroups, hostTemplates);
    }
}
