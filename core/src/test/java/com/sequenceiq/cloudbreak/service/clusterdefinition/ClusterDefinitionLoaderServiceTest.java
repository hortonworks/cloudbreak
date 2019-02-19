package com.sequenceiq.cloudbreak.service.clusterdefinition;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
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
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.init.clusterdefinition.ClusterDefinitionLoaderService;
import com.sequenceiq.cloudbreak.init.clusterdefinition.DefaultAmbariBlueprintCache;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDefinitionLoaderServiceTest {

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
    private ClusterDefinitionLoaderService underTest;

    @Mock
    private DefaultAmbariBlueprintCache blueprintCache;

    @Mock
    private DefaultAmbariBlueprintCache defaultAmbariBlueprintCache;

    @Mock
    private Workspace workspace;

    @Test
    public void testBlueprintLoaderWhenTheUserWhenUserHaveAllTheDefaultBlueprintThenItShouldReturnWithFalse() {
        Set<ClusterDefinition> clusterDefinitions = generateDatabaseData(3);
        Map<String, ClusterDefinition> defaultBlueprints = generateCacheData(3);
        when(defaultAmbariBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.addingDefaultClusterDefinitionsAreNecessaryForTheUser(clusterDefinitions);

        Assert.assertFalse(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    public void testBlueprintLoaderWhenTheUserIsANewOneInTheNewWorkspaceThenItShouldReturnWithTrue() {
        Set<ClusterDefinition> clusterDefinitions = generateDatabaseData(0);
        Map<String, ClusterDefinition> defaultBlueprints = generateCacheData(2);
        when(defaultAmbariBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.addingDefaultClusterDefinitionsAreNecessaryForTheUser(clusterDefinitions);

        Assert.assertTrue(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    public void testBlueprintLoaderWhenTheUserIsANewOneInTheExistingWorkspaceThenItShouldReturnWithTrue() {
        Set<ClusterDefinition> clusterDefinitions = generateDatabaseData(1);
        Map<String, ClusterDefinition> defaultBlueprints = generateCacheData(2);
        when(defaultAmbariBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.addingDefaultClusterDefinitionsAreNecessaryForTheUser(clusterDefinitions);

        Assert.assertTrue(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    public void testBlueprintLoaderWhenTheUserHasAllDefaultBlueprintButOneOfItWasChangeThenItShouldReturnWithTrue() {
        Set<ClusterDefinition> clusterDefinitions = generateDatabaseData(3);
        Map<String, ClusterDefinition> defaultBlueprints = generateCacheData(3, 1);
        when(defaultAmbariBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        boolean addingDefaultBlueprintsAreNecessaryForTheUser = underTest.addingDefaultClusterDefinitionsAreNecessaryForTheUser(clusterDefinitions);

        Assert.assertTrue(addingDefaultBlueprintsAreNecessaryForTheUser);
    }

    @Test
    public void testLoadBlueprintsForTheSpecifiedUserWhenOneNewDefaultExistThenRepositoryShouldUpdateOnlyOneBlueprint() {
        Set<ClusterDefinition> clusterDefinitions = generateDatabaseData(3);
        Map<String, ClusterDefinition> defaultBlueprints = generateCacheData(3, 1);
        when(defaultAmbariBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        Collection<ClusterDefinition> resultSet = underTest.loadClusterDEfinitionsForTheWorkspace(clusterDefinitions, workspace, this::mockSave);

        Assert.assertEquals(4L, resultSet.size());
    }

    @Test
    public void testLoadBlueprintsForTheSpecifiedUserIsNewOneAndNoDefaultBlueprintAddedThenAllDefaultShouldBeAdd() {
        Set<ClusterDefinition> clusterDefinitions = generateDatabaseData(0);
        Map<String, ClusterDefinition> defaultBlueprints = generateCacheData(3);
        when(defaultAmbariBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        Collection<ClusterDefinition> resultSet = underTest.loadClusterDEfinitionsForTheWorkspace(clusterDefinitions, workspace, this::mockSave);

        Assert.assertEquals(3L, resultSet.size());
    }

    @Test
    public void testLoadBlueprintsForTheSpecifiedUserWhenEveryDefaultExistThenRepositoryShouldNotUpdateAnything() {
        Set<ClusterDefinition> clusterDefinitions = generateDatabaseData(3);
        Map<String, ClusterDefinition> defaultBlueprints = generateCacheData(3);
        when(defaultAmbariBlueprintCache.defaultBlueprints()).thenReturn(defaultBlueprints);

        Collection<ClusterDefinition> resultSet = underTest.loadClusterDEfinitionsForTheWorkspace(clusterDefinitions, workspace, this::mockSave);

        Assert.assertEquals(3L, resultSet.size());
    }

    private Iterable<ClusterDefinition> mockSave(Iterable<ClusterDefinition> blueprints, Workspace workspace) {
        return blueprints;
    }

    private Map<String, ClusterDefinition> generateCacheData(int cacheSize) {
        return generateCacheData(cacheSize, 0);
    }

    private Map<String, ClusterDefinition> generateCacheData(int cacheSize, int startIndex) {
        Map<String, ClusterDefinition> cacheData = new HashMap<>();
        for (int i = startIndex; i < cacheSize + startIndex; i++) {
            ClusterDefinition clusterDefinition = createBlueprint(DEFAULT, i);
            cacheData.put(clusterDefinition.getName(), clusterDefinition);
        }
        return cacheData;
    }

    private Set<ClusterDefinition> generateDatabaseData(int cacheSize) {
        Set<ClusterDefinition> databaseData = new HashSet<>();
        for (int i = 0; i < cacheSize; i++) {
            ClusterDefinition clusterDefinition = createBlueprint(DEFAULT, i);
            databaseData.add(clusterDefinition);
        }
        return databaseData;
    }

    public static ClusterDefinition createBlueprint(ResourceStatus resourceStatus, int index) {
        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setId((long) index);
        clusterDefinition.setStackName("test-validation" + index);
        clusterDefinition.setClusterDefinitionText(JSON + index);
        clusterDefinition.setHostGroupCount(3);
        clusterDefinition.setStatus(resourceStatus);
        clusterDefinition.setDescription("test validation" + index);
        clusterDefinition.setName("multi-node-hdfs-yarn" + index);
        return clusterDefinition;
    }

    public static CloudbreakUser identityUser() {
        return new CloudbreakUser(LUCKY_MAN, LUCKY_MAN, LUCKY_MAN, LOTTERY_WINNERS);
    }
}
