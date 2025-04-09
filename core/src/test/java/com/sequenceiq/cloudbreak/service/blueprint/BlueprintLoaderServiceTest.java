package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT_DELETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintFile;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.init.blueprint.DefaultBlueprintCache;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class BlueprintLoaderServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String LUCKY_MAN = "lucky_man";

    private static final String LOTTERY_WINNERS = "lottery_winners";

    private static final String JSON = "{\"validation\":{\"Blueprints\":{\"blueprint_name\":\"hdp-etl-edw-tp\","
            + "\"stack_name\":\"HDP\",\"stack_version\":\"2.5\"},\"configurations\":[{\"core-site\":{\"fs.trash.interval\":\"4320\"}},{\"hdfs-site\":"
            + "{\"dfs.namenode.safemode.threshold-pct\":\"0.99\"}},{\"hive-site\":{\"hive.exec.compress.output\":\"true\",\"hive.merge.mapfiles\""
            + ":\"true\",\"hive.server2.tez.initialize.default.sessions\":\"true\"}},{\"mapred-site\":{\"mapreduce.job.reduce.slowstart.completedmaps\""
            + ":\"0.7\",\"mapreduce.map.output.compress\":\"true\",\"mapreduce.output.fileoutputformat.compress\":\"true\"}},{\"yarn-site\""
            + ":{\"yarn.acl.enable\":\"true\"}}],\"host_groups\":[{\"name\":\"master\",\"configurations\":[],\"components\":[{\"name\""
            + ":\"APP_TIMELINE_SERVER\"},{\"name\":\"HCAT\"},{\"name\":\"HDFS_CLIENT\"},{\"name\":\"HISTORYSERVER\"},{\"name\":\"HIVE_CLIENT\"}"
            + ",{\"name\":\"HIVE_METASTORE\"},{\"name\":\"HIVE_SERVER\"},{\"name\":\"JOURNALNODE\"},{\"name\":\"LIVY_SERVER\"},{\"name\""
            + ":\"MAPREDUCE2_CLIENT\"},{\"name\":\"METRICS_COLLECTOR\"},{\"name\":\"METRICS_GRAFANA\"},{\"name\":\"METRICS_MONITOR\"},"
            + "{\"name\":\"MYSQL_SERVER\"},{\"name\":\"NAMENODE\"},{\"name\":\"PIG\"},{\"name\":\"RESOURCEMANAGER\"},{\"name\":\"SECONDARY_NAMENODE\"}"
            + ",{\"name\":\"SPARK2_CLIENT\"},{\"name\":\"SPARK2_JOBHISTORYSERVER\"},{\"name\":\"SQOOP\"},{\"name\":\"TEZ_CLIENT\"},"
            + "{\"name\":\"WEBHCAT_SERVER\"},{\"name\":\"YARN_CLIENT\"},{\"name\":\"ZEPPELIN_MASTER\"},{\"name\":\"ZOOKEEPER_CLIENT\"},"
            + "{\"name\":\"ZOOKEEPER_SERVER\"}],\"cardinality\":\"1\"},{\"name\":\"worker\",\"configurations\":[],\"components\":["
            + "{\"name\":\"DATANODE\"},{\"name\":\"METRICS_MONITOR\"},{\"name\":\"NODEMANAGER\"}],\"cardinality\":\"1+\"}]},\"inputs\": []}";

    @InjectMocks
    private BlueprintLoaderService underTest;

    @Mock
    private DefaultBlueprintCache blueprintCache;

    @Mock
    private DefaultBlueprintCache defaultBlueprintCache;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private ClusterTemplateService clusterTemplateService;

    @Mock
    private Workspace workspace;

    @Spy
    private BlueprintListFilters blueprintListFilters;

    @Mock
    private EntitlementService entitlementService;

    public static Blueprint createBlueprint(ResourceStatus resourceStatus, int index, BlueprintUpgradeOption upgradeOption) {
        Blueprint blueprint = new Blueprint();
        blueprint.setId((long) index);
        blueprint.setStackName("test-validation" + index);
        if (resourceStatus == DEFAULT || resourceStatus == DEFAULT_DELETED) {
            blueprint.setDefaultBlueprintText(JSON + index);
        }
        blueprint.setBlueprintTextToBlankIfDefaultTextIsPresent(JSON + index);
        blueprint.setHostGroupCount(3);
        blueprint.setStatus(resourceStatus);
        blueprint.setDescription("test validation" + index);
        blueprint.setName("multi-node-hdfs-yarn" + index);
        blueprint.setBlueprintUpgradeOption(upgradeOption);
        return blueprint;
    }

    public static BlueprintFile createBlueprintFile(ResourceStatus resourceStatus, int index, BlueprintUpgradeOption upgradeOption) {
        return new BlueprintFile.Builder()
                .stackName("test-validation" + index)
                .blueprintText(JSON + index)
                .defaultBlueprintText(JSON + index)
                .hostGroupCount(3)
                .stackVersion("2")
                .stackType("dl")
                .name("multi-node-hdfs-yarn" + index)
                .description("test validation" + index)
                .blueprintUpgradeOption(upgradeOption)
                .build();
    }

    public static CloudbreakUser identityUser() {
        return new CloudbreakUser(LUCKY_MAN, LUCKY_MAN, LUCKY_MAN, LUCKY_MAN, LOTTERY_WINNERS);
    }

    @Test
    void testWhenUserHaveAllTheDefaultBlueprintThenReturnFalse() {
        Set<Blueprint> blueprints = generateBlueprintData(3, BlueprintUpgradeOption.ENABLED);
        setupMock(generateCacheData(3, BlueprintUpgradeOption.ENABLED));

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        assertFalse(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    void testWhenTheUserIsANewOneInTheNewWorkspaceThenReturnTrue() {
        Set<Blueprint> blueprints = generateBlueprintData(0, BlueprintUpgradeOption.ENABLED);
        setupMock(generateCacheData(2, BlueprintUpgradeOption.ENABLED));

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        assertTrue(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    void testWhenUserDeletedDefaultAndPresentedInCacheShouldReturnTrue() {
        Set<Blueprint> blueprints = generateBlueprintData(0, BlueprintUpgradeOption.ENABLED);
        blueprints.add(createBlueprint(DEFAULT_DELETED, 0, BlueprintUpgradeOption.ENABLED));
        setupMock(generateCacheData(1, BlueprintUpgradeOption.ENABLED));

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        assertTrue(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    void testWhenUserDeletedDefaultAndNOTPresentedInCacheShouldReturnFalse() {
        Set<Blueprint> blueprints = generateBlueprintData(0, BlueprintUpgradeOption.ENABLED);
        blueprints.add(createBlueprint(DEFAULT_DELETED, 0, BlueprintUpgradeOption.ENABLED));
        setupMock(new HashMap<>());

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        assertFalse(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    void testWhenTheUserIsANewOneInTheExistingWorkspaceThenReturnTrue() {
        Set<Blueprint> blueprints = generateBlueprintData(1, BlueprintUpgradeOption.ENABLED);
        setupMock(generateCacheData(2, BlueprintUpgradeOption.ENABLED));

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        assertTrue(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    void testWhenTheUserHasAllDefaultBlueprintsButOneWasChangedThenReturnTrue() {
        Set<Blueprint> blueprints = generateBlueprintData(3, BlueprintUpgradeOption.ENABLED);
        setupMock(generateCacheData(3, 1, BlueprintUpgradeOption.ENABLED));

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        assertTrue(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    void testWhenTheUserHasUserManagedBlueprintsButOneDefaultBlueprintHasSameNameThenReturnFalse() {
        Set<Blueprint> blueprints = new HashSet<>();
        blueprints.add(createBlueprint(USER_MANAGED, 0, BlueprintUpgradeOption.ENABLED));
        setupMock(generateCacheData(1, BlueprintUpgradeOption.ENABLED));

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        // We don't want to add the blueprint just update, so we should return false this point
        assertFalse(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    void testForTheSpecifiedUserWhenNewDefaultsExistAndNeedsToRetireOldBlueprintsThenRepositoryShouldAddTwoNewBlueprintAndRetireTwoOld() {
        Set<ClusterTemplate> emptyClusterTemplateList = Sets.newHashSet();
        // We have a0, a1, a2 in the DB
        Set<Blueprint> blueprints = generateBlueprintData(3, BlueprintUpgradeOption.ENABLED);
        // We have a2, a3, a4 as defaults
        Map<String, BlueprintFile> defaultBlueprints = generateCacheData(3, 2, BlueprintUpgradeOption.ENABLED);
        setupMock(defaultBlueprints);
        when(clusterTemplateService.getTemplatesByBlueprint(any(Blueprint.class))).thenReturn(emptyClusterTemplateList);

        Collection<Blueprint> resultSet = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave));

        ArgumentCaptor<Iterable<Blueprint>> argument = ArgumentCaptor.forClass(Iterable.class);
        verify(blueprintService).pureSaveAll(argument.capture());
        verify(blueprintService, times(1)).pureSaveAll(any(Iterable.class));
        assertEquals(2, Iterators.size(argument.getValue().iterator()));
        assertEquals(3L, resultSet.size());
        assertTrue(CollectionUtils.isEqualCollection(Sets.newHashSet(
                "multi-node-hdfs-yarn0", "multi-node-hdfs-yarn1", "multi-node-hdfs-yarn2"
        ), collectBpNames(blueprints)));
        assertTrue(CollectionUtils.isEqualCollection(Sets.newHashSet(
                "multi-node-hdfs-yarn2", "multi-node-hdfs-yarn3", "multi-node-hdfs-yarn4"
        ), defaultBlueprints.keySet()));
        assertTrue(CollectionUtils.isEqualCollection(Sets.newHashSet(
                "multi-node-hdfs-yarn2", "multi-node-hdfs-yarn3", "multi-node-hdfs-yarn4"
        ), collectBpNames(resultSet)));
    }

    @Test
    void testForTheSpecifiedUserWhenNewDefaultsExistAndNONeedsToRetireOldBlueprintsThenRepositoryShouldUpdateAddTwoNewAndReturnWithFive() {
        Set<ClusterTemplate> notEmptyClusterTemplateList = Sets.newHashSet(new ClusterTemplate());
        // We have a0, a1, a2 in the DB
        Set<Blueprint> blueprints = generateBlueprintData(3, BlueprintUpgradeOption.ENABLED);
        // We have a2, a3, a4 as defaults
        Map<String, BlueprintFile> defaultBlueprints = generateCacheData(3, 2, BlueprintUpgradeOption.ENABLED);
        setupMock(defaultBlueprints);
        when(clusterTemplateService.getTemplatesByBlueprint(any(Blueprint.class))).thenReturn(notEmptyClusterTemplateList);
        Collection<Blueprint> resultSet = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave));

        ArgumentCaptor<Iterable<Blueprint>> argument = ArgumentCaptor.forClass(Iterable.class);
        verify(blueprintService).pureSaveAll(argument.capture());
        verify(blueprintService, times(1)).pureSaveAll(any(Iterable.class));
        assertEquals(0, Iterators.size(argument.getValue().iterator()));
        assertEquals(5L, resultSet.size());
        assertTrue(CollectionUtils.isEqualCollection(Sets.newHashSet(
                "multi-node-hdfs-yarn0", "multi-node-hdfs-yarn1", "multi-node-hdfs-yarn2"
        ), collectBpNames(blueprints)));
        assertTrue(CollectionUtils.isEqualCollection(Sets.newHashSet(
                "multi-node-hdfs-yarn2", "multi-node-hdfs-yarn3", "multi-node-hdfs-yarn4"
        ), defaultBlueprints.keySet()));
        assertTrue(CollectionUtils.isEqualCollection(Sets.newHashSet(
                "multi-node-hdfs-yarn0", "multi-node-hdfs-yarn1", "multi-node-hdfs-yarn2", "multi-node-hdfs-yarn3", "multi-node-hdfs-yarn4"
        ), collectBpNames(resultSet)));
    }

    public Set<String> collectBpNames(Collection<Blueprint> resultSet) {
        return resultSet.stream().map(a -> a.getName()).collect(Collectors.toSet());
    }

    @Test
    void testLoadBlueprintsWhenUserIsANewOneInTheExistingWorkspaceThenAllDefaultShouldBeAdd() {
        Set<Blueprint> blueprints = generateBlueprintData(0, BlueprintUpgradeOption.ENABLED);
        setupMock(generateCacheData(3, BlueprintUpgradeOption.ENABLED));

        Collection<Blueprint> resultSet = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave));
        assertEquals(3L, resultSet.size());
    }

    @Test
    void testLoadBlueprintsForTheSpecifiedUserWhenEveryDefaultExistThenRepositoryShouldNotUpdateAnything() {
        Set<Blueprint> blueprints = generateBlueprintData(3, BlueprintUpgradeOption.ENABLED);
        setupMock(generateCacheData(3, BlueprintUpgradeOption.ENABLED));

        Collection<Blueprint> resultSet = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave));

        assertEquals(3L, resultSet.size());
    }

    @Test
    void testLoadBlueprintsForTheSpecifiedUserWhenDefaultBlueprintExistsInDBAsUserManagedThenUpdateToDefault() {
        Set<Blueprint> blueprints = new HashSet<>();
        Blueprint blueprint = createBlueprint(USER_MANAGED, 0, BlueprintUpgradeOption.ENABLED);
        blueprints.add(blueprint);
        setupMock(generateCacheData(1, BlueprintUpgradeOption.ENABLED));

        Collection<Blueprint> resultSet = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave));

        assertTrue(resultSet.stream().findFirst().isPresent());
        assertEquals(DEFAULT, resultSet.stream().findFirst().get().getStatus());
        assertEquals(DEFAULT, blueprint.getStatus());
    }

    @Test
    void testLoadBlueprintsForTheSpecifiedUserWhenHasUserManagedBlueprintButNoDefultWithThisNameThenShouldNotUpdate() {
        Set<Blueprint> blueprints = new HashSet<>();
        Blueprint blueprint = createBlueprint(USER_MANAGED, 1, BlueprintUpgradeOption.ENABLED);
        blueprints.add(blueprint);
        setupMock(generateCacheData(1, BlueprintUpgradeOption.ENABLED));

        Collection<Blueprint> resultSet = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave));
        assertTrue(resultSet.stream().findFirst().isPresent());
        assertEquals(DEFAULT, resultSet.stream().findFirst().get().getStatus());
        assertEquals(USER_MANAGED, blueprint.getStatus());
    }

    @Test
    void testLoadBlueprintsForTheSpecifiedUserWhenDefaultBlueprintsInTheDBHaveNullDefaultBlueprintText() {
        Set<Blueprint> blueprints = generateBlueprintData(3, BlueprintUpgradeOption.ENABLED).stream()
                .map(bp -> {
                    bp.setDefaultBlueprintText(null);
                    return bp;
                })
                .collect(Collectors.toSet());
        setupMock(generateCacheData(3, BlueprintUpgradeOption.ENABLED));

        Set<Blueprint> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave));

        assertTrue(result.stream().allMatch(bp -> bp.getBlueprintText() == null && bp.getDefaultBlueprintText() != null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testLoadBlueprintsForTheWorkspaceWhenLakehouseOptimizerDefaultBlueprintsAreMissing(boolean lakehouseOptimizerEnabled) {
        Map<String, BlueprintFile> defaultBlueprints = generateCacheData(3, BlueprintUpgradeOption.ENABLED);
        defaultBlueprints.put("7.3.1 - Lakehouse Optimizer", new BlueprintFile.Builder()
                .stackName("cloudera_lakehouse_optimizer")
                .blueprintText("blueprintText")
                .stackVersion("stackVersion")
                .stackType("CDH")
                .name("7.3.1 - Lakehouse Optimizer")
                .build());
        setupMock(defaultBlueprints);
        when(entitlementService.isLakehouseOptimizerEnabled("1234")).thenReturn(lakehouseOptimizerEnabled);

        Set<Blueprint> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(new HashSet<>(), workspace, this::mockSave));

        assertEquals(lakehouseOptimizerEnabled ? 4 : 3, result.size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testLoadBlueprintsForTheWorkspaceWhenHybridDefaultBlueprintsAreMissing(boolean hybridEnabled) {
        Map<String, BlueprintFile> defaultBlueprints = generateCacheData(3, BlueprintUpgradeOption.ENABLED);
        defaultBlueprints.put("7.3.1 - Hybrid", new BlueprintFile.Builder()
                .stackName("hybrid")
                .hybridOption(BlueprintHybridOption.BURST_TO_CLOUD)
                .blueprintText("blueprintText")
                .stackVersion("stackVersion")
                .stackType("CDH")
                .name("7.3.1 - Hybrid")
                .build());
        setupMock(defaultBlueprints);
        when(entitlementService.hybridCloudEnabled("1234")).thenReturn(hybridEnabled);

        Set<Blueprint> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(new HashSet<>(), workspace, this::mockSave));

        assertEquals(hybridEnabled ? 4 : 3, result.size());
        if (hybridEnabled) {
            assertThat(result).anyMatch(bp -> BlueprintHybridOption.BURST_TO_CLOUD.equals(bp.getHybridOption()));
        } else {
            assertThat(result).noneMatch(bp -> BlueprintHybridOption.BURST_TO_CLOUD.equals(bp.getHybridOption()));
        }
    }

    @Test
    void testIsAddingDefaultBlueprintsNecessaryForTheUserWhenBlueprintTextUnchangedButDefaultBlueprintTextNullForDefaultBlueprintInDBThenUpdateDB() {
        Set<Blueprint> blueprintsFromDatabase = generateBlueprintData(3, BlueprintUpgradeOption.ENABLED).stream()
                .map(bp -> {
                    bp.setDefaultBlueprintText(null);
                    return bp;
                })
                .collect(Collectors.toSet());
        setupMock(generateCacheData(3, BlueprintUpgradeOption.ENABLED));

        boolean result = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprintsFromDatabase);

        assertTrue(result);
    }

    private Iterable<Blueprint> mockSave(Iterable<Blueprint> blueprints, Workspace workspace) {
        return blueprints;
    }

    private Map<String, BlueprintFile> generateCacheData(int cacheSize, BlueprintUpgradeOption upgradeOption) {
        return generateCacheData(cacheSize, 0, upgradeOption);
    }

    private Map<String, BlueprintFile> generateCacheData(int cacheSize, int startIndex, BlueprintUpgradeOption upgradeOption) {
        Map<String, BlueprintFile> cacheData = new HashMap<>();
        for (int i = startIndex; i < cacheSize + startIndex; i++) {
            BlueprintFile blueprintFile = createBlueprintFile(DEFAULT, i, upgradeOption);
            cacheData.put(blueprintFile.getName(), blueprintFile);
        }
        return cacheData;
    }

    private Set<Blueprint> generateBlueprintData(int blueprintSize, BlueprintUpgradeOption upgradeOption) {
        Set<Blueprint> blueprintData = new HashSet<>();
        for (int i = 0; i < blueprintSize; i++) {
            Blueprint blueprint = createBlueprint(DEFAULT, i, upgradeOption);
            blueprintData.add(blueprint);
        }
        return blueprintData;
    }

    private Set<BlueprintFile> generateBlueprintFileData(int blueprintSize, BlueprintUpgradeOption upgradeOption) {
        Set<BlueprintFile> blueprintData = new HashSet<>();
        for (int i = 0; i < blueprintSize; i++) {
            BlueprintFile blueprintFile = createBlueprintFile(DEFAULT, i, upgradeOption);
            blueprintData.add(blueprintFile);
        }
        return blueprintData;
    }

    private void setupMock(Map<String, BlueprintFile> defaultBlueprints) {
        lenient().when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);
    }

    public BlueprintLoaderService getUnderTest() {
        return underTest;
    }
}
