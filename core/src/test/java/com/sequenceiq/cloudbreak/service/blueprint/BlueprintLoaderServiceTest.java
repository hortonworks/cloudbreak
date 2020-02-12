package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT_DELETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.init.blueprint.DefaultBlueprintCache;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintLoaderServiceTest {

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
    private Workspace workspace;

    public static Blueprint createBlueprint(ResourceStatus resourceStatus, int index) {
        Blueprint blueprint = new Blueprint();
        blueprint.setId((long) index);
        blueprint.setStackName("test-validation" + index);
        blueprint.setBlueprintText(JSON + index);
        blueprint.setHostGroupCount(3);
        blueprint.setStatus(resourceStatus);
        blueprint.setDescription("test validation" + index);
        blueprint.setName("multi-node-hdfs-yarn" + index);
        return blueprint;
    }

    public static CloudbreakUser identityUser() {
        return new CloudbreakUser(LUCKY_MAN, LUCKY_MAN, LUCKY_MAN, LUCKY_MAN, LOTTERY_WINNERS);
    }

    @Test
    public void testWhenUserHaveAllTheDefaultBlueprintThenReturnFalse() {
        Set<Blueprint> blueprints = generateBlueprintData(3);
        Map<String, Blueprint> defaultBlueprints = generateCacheData(3);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        Assert.assertFalse(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    public void testWhenTheUserIsANewOneInTheNewWorkspaceThenReturnTrue() {
        Set<Blueprint> blueprints = generateBlueprintData(0);
        Map<String, Blueprint> defaultBlueprints = generateCacheData(2);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        Assert.assertTrue(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    public void testWhenUserDeletedDefault() {
        Set<Blueprint> blueprints = generateBlueprintData(0);
        blueprints.add(createBlueprint(DEFAULT_DELETED, 0));
        Map<String, Blueprint> defaultBlueprints = generateCacheData(1);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        Assert.assertFalse(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    public void testWhenTheUserIsANewOneInTheExistingWorkspaceThenReturnTrue() {
        Set<Blueprint> blueprints = generateBlueprintData(1);
        Map<String, Blueprint> defaultBlueprints = generateCacheData(2);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        Assert.assertTrue(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    public void testWhenTheUserHasAllDefaultBlueprintsButOneWasChangedThenReturnTrue() {
        Set<Blueprint> blueprints = generateBlueprintData(3);
        Map<String, Blueprint> defaultBlueprints = generateCacheData(3, 1);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        Assert.assertTrue(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    public void testWhenTheUserHasUserManagedBlueprintsButOneDefaultBlueprintHasSameNameThenReturnFalse() {
        Set<Blueprint> blueprints = new HashSet<>();
        blueprints.add(createBlueprint(USER_MANAGED, 0));
        Map<String, Blueprint> defaultBlueprints = generateCacheData(1);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.isAddingDefaultBlueprintsNecessaryForTheUser(blueprints);

        // We don't want to add the blueprint just update, so we should return false this point
        Assert.assertFalse(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    public void testForTheSpecifiedUserWhenOneNewDefaultExistThenRepositoryShouldUpdateOnlyOneBlueprint() {
        Set<Blueprint> blueprints = generateBlueprintData(3);
        Map<String, Blueprint> defaultBlueprints = generateCacheData(3, 2);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        Collection<Blueprint> resultSet = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave));
        Assert.assertEquals(3L, resultSet.size());
    }

    @Test
    public void testLoadBlueprintsWhenUserIsANewOneInTheExistingWorkspaceThenAllDefaultShouldBeAdd() {
        Set<Blueprint> blueprints = generateBlueprintData(0);
        Map<String, Blueprint> defaultBlueprints = generateCacheData(3);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        Collection<Blueprint> resultSet = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave));
        Assert.assertEquals(3L, resultSet.size());
    }

    @Test
    public void testLoadBlueprintsForTheSpecifiedUserWhenEveryDefaultExistThenRepositoryShouldNotUpdateAnything() {
        Set<Blueprint> blueprints = generateBlueprintData(3);
        Map<String, Blueprint> defaultBlueprints = generateCacheData(3);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        Collection<Blueprint> resultSet = underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave);
        Assert.assertEquals(3L, resultSet.size());
    }

    @Test
    public void testLoadBlueprintsForTheSpecifiedUserWhenDefaultBlueprintExistsInDBAsUserManagedThenUpdateToDefault() {
        Set<Blueprint> blueprints = new HashSet<>();
        Blueprint blueprint = createBlueprint(USER_MANAGED, 0);
        blueprints.add(blueprint);
        Map<String, Blueprint> defaultBlueprints = generateCacheData(1);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        Collection<Blueprint> resultSet = underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave);

        Assert.assertTrue(resultSet.stream().findFirst().isPresent());
        Assert.assertEquals(resultSet.stream().findFirst().get().getStatus(), DEFAULT);
        Assert.assertEquals(blueprint.getStatus(), DEFAULT);
    }

    @Test
    public void testLoadBlueprintsForTheSpecifiedUserWhenHasUserManagedBlueprintButNoDefultWithThisNameThenShouldNotUpdate() {
        Set<Blueprint> blueprints = new HashSet<>();
        Blueprint blueprint = createBlueprint(USER_MANAGED, 1);
        blueprints.add(blueprint);
        Map<String, Blueprint> defaultBlueprints = generateCacheData(1);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        Collection<Blueprint> resultSet = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.loadBlueprintsForTheWorkspace(blueprints, workspace, this::mockSave));
        Assert.assertTrue(resultSet.stream().findFirst().isPresent());
        Assert.assertEquals(resultSet.stream().findFirst().get().getStatus(), DEFAULT);
        Assert.assertEquals(blueprint.getStatus(), USER_MANAGED);
    }

    private Iterable<Blueprint> mockSave(Iterable<Blueprint> blueprints, Workspace workspace) {
        return blueprints;
    }

    private Map<String, Blueprint> generateCacheData(int cacheSize) {
        return generateCacheData(cacheSize, 0);
    }

    private Map<String, Blueprint> generateCacheData(int cacheSize, int startIndex) {
        Map<String, Blueprint> cacheData = new HashMap<>();
        for (int i = startIndex; i < cacheSize + startIndex; i++) {
            Blueprint blueprint = createBlueprint(DEFAULT, i);
            cacheData.put(blueprint.getName(), blueprint);
        }
        return cacheData;
    }

    private Set<Blueprint> generateBlueprintData(int blueprintSize) {
        Set<Blueprint> blueprintData = new HashSet<>();
        for (int i = 0; i < blueprintSize; i++) {
            Blueprint blueprint = createBlueprint(DEFAULT, i);
            blueprintData.add(blueprint);
        }
        return blueprintData;
    }
}
