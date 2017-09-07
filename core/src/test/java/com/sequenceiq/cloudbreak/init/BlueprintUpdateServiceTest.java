package com.sequenceiq.cloudbreak.init;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintUpdateServiceTest {

    private static final String JSON = "{\"blueprint\":{\"Blueprints\":{\"blueprint_name\":\"hdp-etl-edw-tp\","
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
    private BlueprintUpdateService underTest;

    @Mock
    private BlueprintRepository blueprintRepository;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Before
    public void setUp() throws IOException {
        when(blueprintRepository.save(any(Blueprint.class))).thenReturn(new Blueprint());
        when(blueprintUtils.convertStringToJsonNode(anyString())).thenReturn(JsonUtil.readTree(JSON));
        when(blueprintUtils.isBlueprintNamePreConfigured(anyString(), any())).thenCallRealMethod();
    }

    @Test
    public void updateDefaultBlueprintTestWhenOnlyOneDefaultIsConfigure() throws IOException {
        when(blueprintRepository.findAll()).thenReturn(Collections.singletonList(TestUtil.blueprint("testloader1")));
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("testloader1", "testloader", "testloader"));
        when(blueprintUtils.readDefaultBlueprintFromFile(anyObject())).thenReturn(JSON);
        when(blueprintUtils.countHostGroups(anyObject())).thenReturn(2);
        when(blueprintRepository.save(any(Blueprint.class))).thenReturn(new Blueprint());
        underTest.onApplicationEvent(null);
        verify(blueprintRepository, times(1)).save(any(Blueprint.class));
    }

    @Test
    public void updateDefaultBlueprintTestWhenEveryDefaultIsConfigure() throws IOException {
        when(blueprintRepository.findAll()).thenReturn(Arrays.asList(
                TestUtil.blueprint("testloader1"),
                TestUtil.blueprint("testloader2"),
                TestUtil.blueprint("testloader3")
        ));
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("testloader1", "testloader2", "testloader3"));
        when(blueprintUtils.readDefaultBlueprintFromFile(anyObject())).thenReturn(JSON);
        when(blueprintUtils.countHostGroups(anyObject())).thenReturn(2);
        when(blueprintRepository.save(any(Blueprint.class))).thenReturn(new Blueprint());
        underTest.onApplicationEvent(null);
        verify(blueprintRepository, times(3)).save(any(Blueprint.class));
    }
}
