package com.sequenceiq.cloudbreak.blueprint.template;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
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
        assertTrue(result.contains("testbucket"));
        assertTrue(result.contains("{{ zookeeper_quorum }}"));
        assertTrue(result.contains("{{default('/configurations/hadoop-env/hdfs_log_dir_prefix', '/var/log/hadoop')}}"));
        assertTrue(result.contains(cluster.getName()));
        assertTrue(result.contains("jdbc:postgresql://10.1.1.1:5432/ranger"));
        assertTrue(result.contains("cn=users,dc=example,dc=org"));
        assertTrue(result.contains("ldap://localhost:389"));
    }

    @Test
    public void testMustacheGeneratorForRangerRDS() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-mustache-test.bp");
        Cluster cluster = cluster();

        String result = underTest.process(testBlueprint, cluster, cluster.getRdsConfigs(), ambariDatabase());

        assertTrue(result.contains("\"db_host\": \"10.1.1.1:5432\""));
        assertTrue(result.contains("\"db_user\": \"heyitsme\""));
        assertTrue(result.contains("\"db_password\": \"iamsoosecure\""));
        assertTrue(result.contains("\"db_name\": \"ranger\""));
        assertTrue(result.contains("\"ranger_privelege_user_jdbc_url\": \"jdbc:postgresql://10.1.1.1:5432\""));
        assertTrue(result.contains("\"ranger.jpa.jdbc.url\": \"jdbc:postgresql://10.1.1.1:5432/ranger\""));
    }

    @Test
    public void testMustacheGeneratorForHiveRDS() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-mustache-test.bp");
        Cluster cluster = cluster();

        String result = underTest.process(testBlueprint, cluster, cluster.getRdsConfigs(), ambariDatabase());

        assertTrue(result.contains("\"javax.jdo.option.ConnectionURL\": \"jdbc:postgresql://10.1.1.1:5432/hive\""));
        assertTrue(result.contains("\"javax.jdo.option.ConnectionUserName\": \"heyitsme\""));
        assertTrue(result.contains("\"javax.jdo.option.ConnectionPassword\": \"iamsoosecure\""));
        assertTrue(result.contains("\"javax.jdo.option.ConnectionDriverName\": \"org.postgresql.Driver\""));
        assertTrue(result.contains("\"hive_database_type\": \"postgres\""));
        assertTrue(result.contains("\"hive_database\": \"Existing postgresql Database\","));
    }

    @Test
    public void testMustacheGeneratorForDruidRDS() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-mustache-test.bp");
        Cluster cluster = cluster();

        String result = underTest.process(testBlueprint, cluster, cluster.getRdsConfigs(), ambariDatabase());

        assertTrue(result.contains("\"druid.metadata.storage.type\": \"postgresql\""));
        assertTrue(result.contains("\"druid.metadata.storage.connector.connectURI\": \"jdbc:postgresql://10.1.1.1:5432/druid\""));
        assertTrue(result.contains("\"druid.metadata.storage.connector.user\": \"heyitsme\""));
        assertTrue(result.contains("\"druid.metadata.storage.connector.password\": \"iamsoosecure\""));
    }

    @Test
    public void testMustacheGeneratorForCustomRDSType() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-mustache-test.bp");
        Cluster cluster = cluster();
        cluster.getRdsConfigs().add(rdsConfig("customRds"));

        String result = underTest.process(testBlueprint, cluster, cluster.getRdsConfigs(), ambariDatabase());

        assertTrue(result.contains("\"custom.metadata.storage.type\": \"postgresql\""));
        assertTrue(result.contains("\"custom.metadata.storage.engine\": \"postgres\""));
        assertTrue(result.contains("\"custom.metadata.storage.connector.connectURI\": \"jdbc:postgresql://10.1.1.1:5432/customRds\""));
        assertTrue(result.contains("\"custom.metadata.storage.connector.host\": \"10.1.1.1\""));
        assertTrue(result.contains("\"custom.metadata.storage.connector.connectionHost\": \"10.1.1.1:5432\""));
        assertTrue(result.contains("\"custom.metadata.storage.connector.user\": \"heyitsme\""));
        assertTrue(result.contains("\"custom.metadata.storage.connector.password\": \"iamsoosecure\""));
        assertTrue(result.contains("\"custom.metadata.storage.connector.databasename\": \"customRds\""));
    }

    private Cluster cluster() {
        Cluster cluster = TestUtil.cluster();
        Set<RDSConfig> rdsConfigSet = new HashSet<>();
        rdsConfigSet.add(rdsConfig(RdsType.DRUID.name().toLowerCase()));
        RDSConfig hiveRds = rdsConfig(RdsType.HIVE.name().toLowerCase());
        rdsConfigSet.add(hiveRds);
        rdsConfigSet.add(rdsConfig(RdsType.RANGER.name().toLowerCase()));
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

    private RDSConfig rdsConfig(String rdsType) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setName(rdsType);
        rdsConfig.setConnectionPassword("iamsoosecure");
        rdsConfig.setConnectionUserName("heyitsme");
        rdsConfig.setConnectionURL("jdbc:postgresql://10.1.1.1:5432/" + rdsType);
        rdsConfig.setConnectionDriver("org.postgresql.Driver");
        rdsConfig.setDatabaseEngine("POSTGRES");
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