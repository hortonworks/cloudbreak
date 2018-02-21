package com.sequenceiq.cloudbreak.blueprint.template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.RdsType;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintTemplateProcessorTest {

    @InjectMocks
    private final BlueprintTemplateProcessor underTest = new BlueprintTemplateProcessor();

    @Test
    public void testMustacheGeneratorWithSimpleUseCase() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-mustache-test.bp");
        Cluster cluster = cluster();

        String result = underTest.process(testBlueprint, cluster, cluster.getRdsConfigs(), ambariDatabase());
        Assert.assertTrue(result.contains("testbucket"));
        Assert.assertTrue(result.contains("{{ zookeeper_quorum }}"));
        Assert.assertTrue(result.contains("{{default('/configurations/hadoop-env/hdfs_log_dir_prefix', '/var/log/hadoop')}}"));
        Assert.assertTrue(result.contains(cluster.getName()));
        Assert.assertTrue(result.contains("jdbc:postgresql://10.1.1.1:5432/ranger"));
        Assert.assertTrue(result.contains("cn=users,dc=example,dc=org"));
        Assert.assertTrue(result.contains("ldap://localhost:389"));
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
        } catch (JsonProcessingException ignored) {
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

    private AmbariDatabase ambariDatabase() {
        AmbariDatabase ambariDatabase = new AmbariDatabase();
        ambariDatabase.setFancyName("mysql");
        ambariDatabase.setHost("10.0.0.2");
        ambariDatabase.setName("ambari-database");
        ambariDatabase.setPassword("password123#$@");
        ambariDatabase.setPort(3000);
        ambariDatabase.setUserName("ambari-database-user");
        ambariDatabase.setVendor("mysql");
        return ambariDatabase;
    }
}