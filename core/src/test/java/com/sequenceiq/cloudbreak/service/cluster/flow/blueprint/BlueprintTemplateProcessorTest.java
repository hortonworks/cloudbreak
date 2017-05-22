package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.common.type.RdsType;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template.BlueprintTemplateProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintTemplateProcessorTest {

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @InjectMocks
    private BlueprintTemplateProcessor underTest = new BlueprintTemplateProcessor();

    @Before
    public void before() {
        AmbariDatabase ambariDatabase = new AmbariDatabase();
        ambariDatabase.setFancyName("testDb");
        ambariDatabase.setHost("10.1.1.2");
        ambariDatabase.setName("test_db");
        ambariDatabase.setPassword("mypasswordissoosecure");
        ambariDatabase.setUserName("itsjustme");
        ambariDatabase.setPort(5432);
        ambariDatabase.setVendor("postgres");

        when(clusterComponentConfigProvider.getAmbariDatabase(anyLong())).thenReturn(ambariDatabase);
    }

    @Test
    public void testMustacheGeneratorWithSimpleUseCase() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-mustache-test.bp");
        Cluster cluster = cluster();

        String result = underTest.process(testBlueprint, cluster, cluster.getRdsConfigs());
        Assert.assertTrue(result.contains("testbucket"));
        Assert.assertTrue(result.contains(cluster.getName()));
        Assert.assertTrue(result.contains(cluster.getName()));
    }

    private Cluster cluster() {
        Cluster cluster = TestUtil.cluster();
        Set<RDSConfig> rdsConfigSet = new HashSet<>();
        rdsConfigSet.add(rdsConfig(RdsType.DRUID));
        rdsConfigSet.add(rdsConfig(RdsType.HIVE));
        rdsConfigSet.add(rdsConfig(RdsType.RANGER));
        cluster.setRdsConfigs(rdsConfigSet);
        Map<String, String> inputs = new HashMap<>();
        inputs.put("S3_BUCKET", "testbucket");
        try {
            cluster.setBlueprintInputs(new Json(inputs));
        } catch (JsonProcessingException e) {
            cluster.setBlueprintInputs(null);
        }
        return cluster;
    }

    private RDSConfig rdsConfig(RdsType rdsType) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setName(rdsType.name().toLowerCase());
        rdsConfig.setConnectionPassword("iamsoosecure");
        rdsConfig.setConnectionUserName("heyitsme");
        rdsConfig.setConnectionURL("jdbc:postgresql://10.1.1.1:5432/" + rdsType.name().toLowerCase());
        rdsConfig.setType(rdsType);
        return rdsConfig;
    }
}