package com.sequenceiq.cloudbreak.service.blueprint;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintLoaderServiceTest {

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
    private BlueprintLoaderService underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private BlueprintRepository blueprintRepository;

    @Mock
    private JsonHelper jsonHelper;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Before
    public void setUp() throws IOException {
        when(conversionService.convert(any(BlueprintRequest.class), any(Class.class))).thenAnswer(new Answer<Blueprint>() {
            @Override
            public Blueprint answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                BlueprintRequest blueprintRequest = (BlueprintRequest) args[0];
                return blueprint(blueprintRequest);
            }
        });
        when(blueprintRepository.findAllDefaultInAccount(anyString())).thenReturn(new HashSet<>());
        when(blueprintRepository.save(any(Blueprint.class))).thenReturn(new Blueprint());
        when(blueprintUtils.convertStringToJsonNode(anyString())).thenReturn(JsonUtil.readTree(JSON));
    }

    @Test
    public void threeDefaultBlueprintWithCorrectParameters() {
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("test1=testloader", "test2=testloader", "test3=testloader"));
        Set<Blueprint> blueprints = underTest.loadBlueprints(cbUser());
        Assert.assertEquals(3, blueprints.size());
    }

    @Test
    public void twoDefaultBlueprintWithCorrectParameters() {
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("test1=testloader", "test2=testloader"));
        Set<Blueprint> blueprints = underTest.loadBlueprints(cbUser());
        Assert.assertEquals(2, blueprints.size());
    }

    @Test
    public void threeDefaultBlueprintButOneIsIncorrect() throws IOException {
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("test1=testloader", "test2=testloader", "incorrect"));
        when(blueprintUtils.readDefaultBlueprintFromFile(new String[]{"incorrect"})).thenThrow(new FileNotFoundException("not found"));
        Set<Blueprint> blueprints = underTest.loadBlueprints(cbUser());
        Assert.assertEquals(2, blueprints.size());
    }

    @Test
    public void threeDefaultBlueprintButEveryParamIsInCorrect() throws IOException {
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("incorrect0", "incorrect1", "incorrect2"));
        when(blueprintUtils.readDefaultBlueprintFromFile(anyObject())).thenThrow(new FileNotFoundException("not found"));
        Set<Blueprint> blueprints = underTest.loadBlueprints(cbUser());
        Assert.assertEquals(0, blueprints.size());
    }

    @Test
    public void threeDefaultBlueprintButEveryParamIsJustFileName() {
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("testloader", "testloader", "testloader"));
        Set<Blueprint> blueprints = underTest.loadBlueprints(cbUser());
        Assert.assertEquals(3, blueprints.size());
    }

    @Test
    public void launchDefaultBlueprintEveryParamIsCorrect() {
        ReflectionTestUtils.setField(underTest, "blueprintArray",
                Arrays.asList(
                        "EDW-ETL: Apache Hive 1.2.1, Apache Spark 1.6=testloader",
                        "Data Science: Apache Spark 1.6, Zeppelin=testloader",
                        "EDW-ETL: Apache Hive 1.2.1, Apache Spark 1.6=testloader",
                        "EDW-ETL: Apache Spark 2.0-preview, Apache Hive 2.0=testloader",
                        "EDW-Analytics: Apache Hive 2.0 LLAP, Apache Zeppelin=testloader"));
        Set<Blueprint> blueprints = underTest.loadBlueprints(cbUser());
        Assert.assertEquals(5, blueprints.size());
    }

    @Test
    public void updateDefaultBlueprintTestWhenOnlyOneDefaultIsConfigure() throws IOException {
        when(blueprintRepository.findAll()).thenReturn(Arrays.asList(TestUtil.blueprint("testloader1")));
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("testloader1", "testloader", "testloader"));
        when(blueprintUtils.readDefaultBlueprintFromFile(anyObject())).thenReturn(JSON);
        when(blueprintUtils.countHostGroups(anyObject())).thenReturn(2);
        when(blueprintRepository.save(any(Blueprint.class))).thenReturn(new Blueprint());
        underTest.updateDefaultBlueprints();
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
        underTest.updateDefaultBlueprints();
        verify(blueprintRepository, times(3)).save(any(Blueprint.class));
    }

    private Blueprint blueprint(BlueprintRequest blueprintRequest) {
        Blueprint blueprint = new Blueprint();
        blueprint.setName(blueprintRequest.getName());
        blueprint.setBlueprintText(blueprintRequest.getAmbariBlueprint());
        return blueprint;
    }

    private CbUser cbUser() {
        return new CbUser("userId", "userName", "account", new ArrayList<>(), "givenName", "familyName", new Date());
    }
}
