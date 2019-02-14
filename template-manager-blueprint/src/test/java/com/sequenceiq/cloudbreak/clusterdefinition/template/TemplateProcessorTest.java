package com.sequenceiq.cloudbreak.clusterdefinition.template;

import static com.sequenceiq.cloudbreak.clusterdefinition.filesystem.ClusterDefinitionTestUtil.generalClusterConfigs;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;
import com.sequenceiq.cloudbreak.template.model.ClusterDefinitionStackInfo;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.model.HdfConfigs;
import com.sequenceiq.cloudbreak.template.views.ClusterDefinitionView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class TemplateProcessorTest {

    @InjectMocks
    private final TemplateProcessor underTest = new TemplateProcessor();

    @Test
    public void testMustacheGeneratorWithSimpleUseCase() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-mustache-test.bp");

        Cluster cluster = cluster();
        ClusterDefinitionStackInfo clusterDefinitionStackInfo = new ClusterDefinitionStackInfo("hdp", "2.4");
        GeneralClusterConfigs generalClusterConfigs = generalClusterConfigs();
        generalClusterConfigs.setClusterName("dummyCluster");
        generalClusterConfigs.setStackName("dummyCluster");

        Map<String, Object> properties = new HashMap<>();
        properties.put("S3_BUCKET", "testbucket");

        TemplatePreparationObject templatePreparationObject = Builder.builder()
                .withRdsConfigs(cluster.getRdsConfigs())
                .withGateway(cluster.getGateway(), "/cb/secret/signkey")
                .withLdapConfig(cluster.getLdapConfig(), "cn=admin,dc=example,dc=org", "admin<>char")
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withClusterDefinitionView(new ClusterDefinitionView(testBlueprint, clusterDefinitionStackInfo.getVersion(),
                        clusterDefinitionStackInfo.getType()))
                .withFixInputs(properties)
                .build();

        String result = underTest.process(testBlueprint, templatePreparationObject, Maps.newHashMap());
        assertTrue(result.contains("testbucket"));
        assertTrue(result.contains("{{ zookeeper_quorum }}"));
        assertTrue(result.contains("{{default('/configurations/hadoop-env/hdfs_log_dir_prefix', '/var/log/hadoop')}}"));
        assertTrue(result.contains(cluster.getName()));
        assertTrue(result.contains("jdbc:postgresql://10.1.1.1:5432/ranger"));
        assertTrue(result.contains("cn=users,dc=example,dc=org"));
        assertTrue(result.contains("ldap://localhost:389"));
        assertFalse(result.contains("cloud-storage-present"));
    }

    @Test
    public void testMustacheGeneratorWithSimpleUseCaseAndCloudStorage() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-mustache-test.bp");

        Cluster cluster = cluster();
        ClusterDefinitionStackInfo clusterDefinitionStackInfo = new ClusterDefinitionStackInfo("hdp", "2.4");
        GeneralClusterConfigs generalClusterConfigs = generalClusterConfigs();
        generalClusterConfigs.setClusterName("dummyCluster");
        generalClusterConfigs.setStackName("dummyCluster");

        Map<String, Object> properties = new HashMap<>();
        properties.put("S3_BUCKET", "testbucket");

        TemplatePreparationObject templatePreparationObject = Builder.builder()
                .withRdsConfigs(cluster.getRdsConfigs())
                .withGateway(cluster.getGateway(), "/cb/secret/signkey")
                .withLdapConfig(cluster.getLdapConfig(), "cn=admin,dc=example,dc=org", "admin<>char")
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withClusterDefinitionView(new ClusterDefinitionView(testBlueprint, clusterDefinitionStackInfo.getVersion(),
                        clusterDefinitionStackInfo.getType()))
                .withFixInputs(properties)
                .withFileSystemConfigurationView(s3FileSystemConfig())
                .build();

        String result = underTest.process(testBlueprint, templatePreparationObject, Maps.newHashMap());
        assertTrue(result.contains("testbucket"));
        assertTrue(result.contains("{{ zookeeper_quorum }}"));
        assertTrue(result.contains("{{default('/configurations/hadoop-env/hdfs_log_dir_prefix', '/var/log/hadoop')}}"));
        assertTrue(result.contains(cluster.getName()));
        assertTrue(result.contains("jdbc:postgresql://10.1.1.1:5432/ranger"));
        assertTrue(result.contains("cn=users,dc=example,dc=org"));
        assertTrue(result.contains("ldap://localhost:389"));
        assertTrue(result.contains("cloud-storage-present"));
        assertTrue(result.contains("0_test/test/end"));
    }

    @Test
    public void testMustacheGeneratorWithSomeTrickyModelSimpleUseCase() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-mustache-tricky-test.bp");

        Cluster cluster = cluster();
        ClusterDefinitionStackInfo clusterDefinitionStackInfo = new ClusterDefinitionStackInfo("hdp", "2.4");
        GeneralClusterConfigs generalClusterConfigs = generalClusterConfigs();
        generalClusterConfigs.setClusterName("dummyCluster");
        generalClusterConfigs.setStackName("dummyCluster");


        Map<String, Object> trickyObject = new HashMap<>();
        trickyObject.put("apple.pie.salat", "cool");
        Map<String, Object> trickyObject2 = new HashMap<>();
        trickyObject2.put("scary", "movie");
        trickyObject.put("stranger_things", trickyObject2);

        Json json = new Json(trickyObject);

        Map<String, Object> properties = new HashMap<>();
        properties.put("S3_BUCKET", "testbucket");
        properties.put("custom", json.getMap());

        TemplatePreparationObject templatePreparationObject = Builder.builder()
                .withRdsConfigs(cluster.getRdsConfigs())
                .withGateway(cluster.getGateway(), "/cb/secret/signkey")
                .withLdapConfig(cluster.getLdapConfig(), "cn=admin,dc=example,dc=org", "admin<>char")
                .withFixInputs(properties)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withClusterDefinitionView(new ClusterDefinitionView(testBlueprint, clusterDefinitionStackInfo.getVersion(),
                        clusterDefinitionStackInfo.getType()))
                .build();

        String result = underTest.process(testBlueprint, templatePreparationObject, Maps.newHashMap());
        assertTrue(result.contains("testbucket"));
        assertTrue(result.contains("{{ zookeeper_quorum }}"));
        assertTrue(result.contains("{{default('/configurations/hadoop-env/hdfs_log_dir_prefix', '/var/log/hadoop')}}"));
        assertTrue(result.contains(cluster.getName()));
        assertTrue(result.contains("jdbc:postgresql://10.1.1.1:5432/ranger"));
        assertTrue(result.contains("cn=users,dc=example,dc=org"));
        assertTrue(result.contains("ldap://localhost:389"));
    }

    @Test
    public void testMustacheGeneratorShouldEscapeNifiHtmlBasedContentsQuotes() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-mustache-test.bp");

        Cluster cluster = cluster();
        ClusterDefinitionStackInfo clusterDefinitionStackInfo = new ClusterDefinitionStackInfo("hdp", "2.4");
        GeneralClusterConfigs generalClusterConfigs = generalClusterConfigs();
        generalClusterConfigs.setClusterName("dummyCluster");
        generalClusterConfigs.setStackName("dummyCluster");

        Map<String, Object> properties = new HashMap<>();
        properties.put("S3_BUCKET", "testbucket");
        HdfConfigs hdfConfigs = new HdfConfigs("<property name=\"Node Identity 1\">CN=hostname-2, OU=NIFI</property>",
                "<property name=\"Nifi Identity 1\">CN=hostname-2, OU=NIFI</property>",
                "<property name=\"Nifi Identity 1\">CN=hostname-2, OU=NIFI</property>", Optional.empty());

        TemplatePreparationObject templatePreparationObject = Builder.builder()
                .withRdsConfigs(cluster.getRdsConfigs())
                .withGateway(cluster.getGateway(), "/cb/secret/signkey")
                .withLdapConfig(cluster.getLdapConfig(), "cn=admin,dc=example,dc=org", "admin<>char")
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withFixInputs(properties)
                .withClusterDefinitionView(new ClusterDefinitionView(testBlueprint, clusterDefinitionStackInfo.getVersion(),
                        clusterDefinitionStackInfo.getType()))
                .withHdfConfigs(hdfConfigs)
                .build();

        String result = underTest.process(testBlueprint, templatePreparationObject, Maps.newHashMap());
        assertTrue(result.contains("\"content\": \"<property name=\\\"Node Identity 1\\\">CN=hostname-2, OU=NIFI</property>\""));
        assertFalse(result.contains("\"content\": \"<property name=\"Node Identity 1\">CN=hostname-2, OU=NIFI</property>\""));
    }

    @Test
    public void testMustacheGeneratorForRangerRDS() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-mustache-test.bp");

        Cluster cluster = cluster();
        ClusterDefinitionStackInfo clusterDefinitionStackInfo = new ClusterDefinitionStackInfo("hdp", "2.4");

        TemplatePreparationObject templatePreparationObject = Builder.builder()
                .withRdsConfigs(cluster.getRdsConfigs())
                .withGateway(cluster.getGateway(), "/cb/secret/signkey")
                .withLdapConfig(cluster.getLdapConfig(), "cn=admin,dc=example,dc=org", "admin<>char")
                .withGeneralClusterConfigs(generalClusterConfigs())
                .withClusterDefinitionView(new ClusterDefinitionView(testBlueprint, clusterDefinitionStackInfo.getVersion(),
                        clusterDefinitionStackInfo.getType()))
                .build();

        String result = underTest.process(testBlueprint, templatePreparationObject, Maps.newHashMap());

        assertTrue(result.contains("\"db_host\": \"10.1.1.1:5432\""));
        assertTrue(result.contains("\"db_user\": \"heyitsme\""));
        assertTrue(result.contains("\"db_password\": \"iamsoosecure\""));
        assertTrue(result.contains("\"db_name\": \"ranger\""));
        assertTrue(result.contains("\"ranger_privelege_user_jdbc_url\": \"jdbc:postgresql://10.1.1.1:5432\""));
        assertTrue(result.contains("\"ranger.jpa.jdbc.url\": \"jdbc:postgresql://10.1.1.1:5432/ranger\""));
    }

    @Test
    public void testMustacheGeneratorForHiveRDS() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-mustache-test.bp");

        Cluster cluster = cluster();
        ClusterDefinitionStackInfo clusterDefinitionStackInfo = new ClusterDefinitionStackInfo("hdp", "2.4");

        TemplatePreparationObject templatePreparationObject = Builder.builder()
                .withRdsConfigs(cluster.getRdsConfigs())
                .withGateway(cluster.getGateway(), "/cb/secret/signkey")
                .withLdapConfig(cluster.getLdapConfig(), "cn=admin,dc=example,dc=org", "admin<>char")
                .withGeneralClusterConfigs(generalClusterConfigs())
                .withClusterDefinitionView(new ClusterDefinitionView(testBlueprint, clusterDefinitionStackInfo.getVersion(),
                        clusterDefinitionStackInfo.getType()))
                .build();

        String result = underTest.process(testBlueprint, templatePreparationObject, Maps.newHashMap());

        assertTrue(result.contains("\"javax.jdo.option.ConnectionURL\": \"jdbc:postgresql://10.1.1.1:5432/hive\""));
        assertTrue(result.contains("\"javax.jdo.option.ConnectionUserName\": \"heyitsme\""));
        assertTrue(result.contains("\"javax.jdo.option.ConnectionPassword\": \"iamsoosecure\""));
        assertTrue(result.contains("\"javax.jdo.option.ConnectionDriverName\": \"org.postgresql.Driver\""));
        assertTrue(result.contains("\"hive_database_type\": \"postgres\""));
        assertTrue(result.contains("\"hive_database\": \"Existing PostgreSQL Database\","));
    }

    @Test
    public void testMustacheGeneratorForDruidRDS() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-mustache-test.bp");

        Cluster cluster = cluster();
        ClusterDefinitionStackInfo clusterDefinitionStackInfo = new ClusterDefinitionStackInfo("hdp", "2.4");

        TemplatePreparationObject templatePreparationObject = Builder.builder()
                .withRdsConfigs(cluster.getRdsConfigs())
                .withGateway(cluster.getGateway(), "/cb/secret/signkey")
                .withLdapConfig(cluster.getLdapConfig(), "cn=admin,dc=example,dc=org", "admin<>char")
                .withGeneralClusterConfigs(generalClusterConfigs())
                .withClusterDefinitionView(new ClusterDefinitionView(testBlueprint, clusterDefinitionStackInfo.getVersion(),
                        clusterDefinitionStackInfo.getType()))
                .build();

        String result = underTest.process(testBlueprint, templatePreparationObject, Maps.newHashMap());

        assertTrue(result.contains("\"druid.metadata.storage.type\": \"postgresql\""));
        assertTrue(result.contains("\"druid.metadata.storage.connector.connectURI\": \"jdbc:postgresql://10.1.1.1:5432/druid\""));
        assertTrue(result.contains("\"druid.metadata.storage.connector.user\": \"heyitsme\""));
        assertTrue(result.contains("\"druid.metadata.storage.connector.password\": \"iamsoosecure\""));
    }

    @Test
    public void testMustacheGeneratorForCustomRDSType() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-mustache-test.bp");
        Cluster cluster = cluster();
        cluster.getRdsConfigs().add(rdsConfig("customRds"));
        ClusterDefinitionStackInfo clusterDefinitionStackInfo = new ClusterDefinitionStackInfo("hdp", "2.4");

        TemplatePreparationObject templatePreparationObject = Builder.builder()
                .withClusterDefinitionView(new ClusterDefinitionView(testBlueprint, clusterDefinitionStackInfo.getVersion(),
                        clusterDefinitionStackInfo.getType()))
                .withRdsConfigs(cluster.getRdsConfigs())
                .withGeneralClusterConfigs(generalClusterConfigs(cluster))
                .build();

        String result = underTest.process(testBlueprint, templatePreparationObject, Maps.newHashMap());

        assertTrue(result.contains("\"custom.metadata.storage.type\": \"postgresql\""));
        assertTrue(result.contains("\"custom.metadata.storage.engine\": \"postgres\""));
        assertTrue(result.contains("\"custom.metadata.storage.connector.connectURI\": \"jdbc:postgresql://10.1.1.1:5432/customRds\""));
        assertTrue(result.contains("\"custom.metadata.storage.connector.host\": \"10.1.1.1\""));
        assertTrue(result.contains("\"custom.metadata.storage.connector.user\": \"heyitsme\""));
        assertTrue(result.contains("\"custom.metadata.storage.connector.password\": \"iamsoosecure\""));
        assertTrue(result.contains("\"custom.metadata.storage.connector.databasename\": \"customRds\""));
    }

    private Cluster cluster() {
        Cluster cluster = TestUtil.cluster();
        Set<RDSConfig> rdsConfigSet = new HashSet<>();
        rdsConfigSet.add(rdsConfig(DatabaseType.DRUID.name().toLowerCase()));
        RDSConfig hiveRds = rdsConfig(DatabaseType.HIVE.name().toLowerCase());
        rdsConfigSet.add(hiveRds);
        rdsConfigSet.add(rdsConfig(DatabaseType.RANGER.name().toLowerCase()));
        cluster.setRdsConfigs(rdsConfigSet);
        return cluster;
    }

    private RDSConfig rdsConfig(String rdsType) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(TestUtil.generateUniqueId());
        rdsConfig.setName(rdsType);
        rdsConfig.setConnectionPassword("iamsoosecure");
        rdsConfig.setConnectionUserName("heyitsme");
        rdsConfig.setConnectionURL("jdbc:postgresql://10.1.1.1:5432/" + rdsType);
        rdsConfig.setConnectionDriver("org.postgresql.Driver");
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES);
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

    private BaseFileSystemConfigurationsView s3FileSystemConfig() {
        return TemplateCoreTestUtil.s3FileSystemConfiguration(TemplateCoreTestUtil.storageLocationViews());
    }
}