package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.configVar;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateHostInfo;
import com.cloudera.api.swagger.model.ApiClusterTemplateInstantiator;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroupInfo;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cloud.model.AutoscaleRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.GatewayRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.cloud.model.ResizeRecommendation;
import com.sequenceiq.cloudbreak.cluster.model.ClusterHostAttributes;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnConstants;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnRoles;
import com.sequenceiq.cloudbreak.cmtemplate.inifile.IniFile;
import com.sequenceiq.cloudbreak.cmtemplate.inifile.IniFileFactory;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.model.ServiceAttributes;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.views.CustomConfigurationPropertyView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
class CmTemplateProcessorTest {

    private CmTemplateProcessor underTest;

    @Mock
    private IniFileFactory iniFileFactory;

    @Mock
    private IniFile safetyValveService;

    @Mock
    private IniFile safetyValveRole;

    @Test
    void testAddServiceConfigs() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        configs.add(new ApiClusterTemplateConfig().name("hive_metastore_database_type").variable("hive-hive_metastore_database_type"));

        underTest.addServiceConfigs("HIVE", configs);

        ApiClusterTemplateService service = underTest.getTemplate().getServices().stream().filter(srv -> "HIVE".equals(srv.getServiceType())).findAny().get();
        List<ApiClusterTemplateConfig> serviceConfigs = service.getServiceConfigs();
        assertEquals(1, serviceConfigs.size());
        assertEquals("hive_metastore_database_type", serviceConfigs.get(0).getName());
        assertEquals("hive-hive_metastore_database_type", serviceConfigs.get(0).getVariable());
    }

    @Test
    void testAddServiceConfigsWhenDuplicatedEntries() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/ENGESC-26635.bp"));

        Map<String, ServiceComponent> stringServiceComponentMap = underTest.mapRoleRefsToServiceComponents();
        Map<String, Set<ServiceComponent>> serviceComponentsByHostGroup = underTest.getServiceComponentsByHostGroup();
        Set<ServiceComponent> allComponents = underTest.getAllComponents();

        assertEquals(39, stringServiceComponentMap.size());
        assertEquals(7, serviceComponentsByHostGroup.size());
        assertEquals(33, allComponents.size());
    }

    @Test
    void testAddServiceConfigsWhenDuplicatedEntries2() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/ENGESC-26635-2.bp"));

        Map<String, ServiceComponent> stringServiceComponentMap = underTest.mapRoleRefsToServiceComponents();
        Map<String, Set<ServiceComponent>> serviceComponentsByHostGroup = underTest.getServiceComponentsByHostGroup();
        Set<ServiceComponent> allComponents = underTest.getAllComponents();

        assertEquals(39, stringServiceComponentMap.size());
        assertEquals(7, serviceComponentsByHostGroup.size());
        assertEquals(33, allComponents.size());
    }

    @Test
    void addExistingServiceConfigs() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-existing-conf.bp"));
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        configs.add(new ApiClusterTemplateConfig().name("redaction_policy_enabled").value("true"));
        configs.add(new ApiClusterTemplateConfig().name("not_present_in_template").value("some_value"));

        underTest.addServiceConfigs("HDFS", configs);

        ApiClusterTemplateService service = underTest.getTemplate().getServices().stream().filter(srv -> "HDFS".equals(srv.getServiceType())).findAny().get();
        List<ApiClusterTemplateConfig> serviceConfigs = service.getServiceConfigs();
        assertEquals(2, serviceConfigs.size());
        assertEquals("redaction_policy_enabled", serviceConfigs.get(0).getName());
        assertEquals("false", serviceConfigs.get(0).getValue());
        assertEquals(configs.get(1), serviceConfigs.get(1));
    }

    @Test
    void addExistingSafetyValveConfigs() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-existing-conf.bp"));

        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        configs.add(new ApiClusterTemplateConfig()
                            .name("hive_service_config_safety_valve")
                            .value("<property><name>testkey</name><value>testvalue</value></property>"));

        underTest.addServiceConfigs("HIVE", configs);

        ApiClusterTemplateService service = underTest.getTemplate().getServices().stream().filter(srv -> "HIVE".equals(srv.getServiceType())).findAny().get();
        List<ApiClusterTemplateConfig> serviceConfigs = service.getServiceConfigs();

        assertEquals(1, serviceConfigs.size());
        assertEquals("hive_service_config_safety_valve", serviceConfigs.get(0).getName());
        assertTrue(serviceConfigs.get(0).getValue().startsWith("<property><name>testkey</name><value>testvalue</value></property>"));
        assertTrue(serviceConfigs.get(0).getValue().endsWith(
                "<property><name>hive.metastore.server.filter.enabled</name><value>true</value></property> " +
                "<property><name>hive.metastore.filter.hook</name>" +
                "<value>org.apache.hadoop.hive.ql.security.authorization.plugin.metastore.HiveMetaStoreAuthorizer</value></property>"));
        assertTrue(serviceConfigs.get(0).getValue().contains("\n"));

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = new HashMap<>();
        roleConfigs.put("spark_on_yarn-GATEWAY-BASE",
                List.of(new ApiClusterTemplateConfig().name("spark-conf/spark-defaults.conf_client_config_safety_valve").value("testkey=testvalue")));

        underTest.addRoleConfigs("SPARK_ON_YARN", roleConfigs);

        service = underTest.getTemplate().getServices().stream().filter(srv -> "SPARK_ON_YARN".equals(srv.getServiceType())).findAny().get();
        ApiClusterTemplateRoleConfigGroup gw = service.getRoleConfigGroups().stream().filter(rcg -> "GATEWAY".equals(rcg.getRoleType())).findAny().get();
        List<ApiClusterTemplateConfig> gwConfigs = gw.getConfigs();

        assertEquals(1, gwConfigs.size());
        assertEquals("spark-conf/spark-defaults.conf_client_config_safety_valve", gwConfigs.get(0).getName());
        assertTrue(gwConfigs.get(0).getValue().startsWith("testkey=testvalue"));
        assertTrue(gwConfigs.get(0).getValue().endsWith("spark.yarn.access.hadoopFileSystems=s3a://expn-cis-sandbox-prod-cdp-us-east-1"));
        assertTrue(gwConfigs.get(0).getValue().contains("\n"));
    }

    @Test
    void addExistingSafetyValveConfigsIniFile() {
        when(iniFileFactory.create()).thenReturn(safetyValveService, safetyValveRole);

        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-existing-conf.bp"), iniFileFactory);

        List<ApiClusterTemplateConfig> serviceConfigsInput = new ArrayList<>();
        serviceConfigsInput.add(new ApiClusterTemplateConfig()
                            .name("hue_service_safety_valve")
                            .value("[desktop]\n[[knox]]\nknox_proxyhosts=foo.com"));
        String expectedSafetyValveValueService =
                "[desktop]\napp_blacklist=spark,zookeeper,hbase,impala,search,sqoop,security,pig\n[[knox]]\nknox_proxyhosts=foo.com";
        when(safetyValveService.print()).thenReturn(expectedSafetyValveValueService);

        underTest.addServiceConfigs("HUE", serviceConfigsInput);

        verify(safetyValveService).addContent("[desktop]\n[[knox]]\nknox_proxyhosts=foo.com");
        verify(safetyValveService).addContent("[desktop]\napp_blacklist=spark,zookeeper,hbase,impala,search,sqoop,security,pig");
        ApiClusterTemplateService service = underTest.getTemplate().getServices().stream().filter(srv -> "HUE".equals(srv.getServiceType())).findAny().get();
        List<ApiClusterTemplateConfig> serviceConfigsResult = service.getServiceConfigs();
        Map<String, String> serviceConfigToValue = ConfigTestUtil.getConfigNameToValueMap(serviceConfigsResult);
        assertThat(serviceConfigToValue).containsOnly(entry("hue_service_safety_valve", expectedSafetyValveValueService));

        Map<String, List<ApiClusterTemplateConfig>> roleConfigsInput = new HashMap<>();
        roleConfigsInput.put("hue-HUE_SERVER-BASE",
                List.of(new ApiClusterTemplateConfig().name("hue_server_hue_safety_valve").value("[dashboard]\n[[engines]]\n[[[sql]]]\nnesting=true")));
        String expectedSafetyValveValueRole = "[dashboard]\nhas_sql_enabled=true\n[[engines]]\n[[[sql]]]\nnesting=true";
        when(safetyValveRole.print()).thenReturn(expectedSafetyValveValueRole);

        underTest.addRoleConfigs("HUE", roleConfigsInput);

        verify(safetyValveRole).addContent("[dashboard]\n[[engines]]\n[[[sql]]]\nnesting=true");
        verify(safetyValveRole).addContent("[dashboard]\nhas_sql_enabled=true");
        ApiClusterTemplateRoleConfigGroup role = service.getRoleConfigGroups().stream().filter(rcg -> "HUE_SERVER".equals(rcg.getRoleType())).findAny().get();
        List<ApiClusterTemplateConfig> roleConfigsResult = role.getConfigs();
        Map<String, String> roleConfigToValue = ConfigTestUtil.getConfigNameToValueMap(roleConfigsResult);
        assertThat(roleConfigToValue).containsOnly(entry("hue_server_hue_safety_valve", expectedSafetyValveValueRole));

        verifyNoMoreInteractions(iniFileFactory, safetyValveService, safetyValveRole);
    }

    @Test
    void testAddRoleConfigs() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        Map<String, List<ApiClusterTemplateConfig>> configs = new HashMap<>();
        configs.put("hdfs-NAMENODE-BASE", List.of(new ApiClusterTemplateConfig().name("dfs_name_dir_list").variable("master_NAMENODE")));
        configs.put("hdfs-DATANODE-BASE", List.of(new ApiClusterTemplateConfig().name("dfs_data_dir_list").variable("worker_DATANODE")));

        underTest.addRoleConfigs("HDFS", configs);

        ApiClusterTemplateService service = underTest.getTemplate().getServices().stream().filter(srv -> "HDFS".equals(srv.getServiceType())).findAny().get();
        ApiClusterTemplateRoleConfigGroup dn = service.getRoleConfigGroups().stream().filter(rcg -> "DATANODE".equals(rcg.getRoleType())).findAny().get();
        List<ApiClusterTemplateConfig> dnConfigs = dn.getConfigs();
        ApiClusterTemplateRoleConfigGroup nn = service.getRoleConfigGroups().stream().filter(rcg -> "NAMENODE".equals(rcg.getRoleType())).findAny().get();
        List<ApiClusterTemplateConfig> nnConfigs = nn.getConfigs();

        assertEquals(1, nnConfigs.size());
        assertEquals(1, dnConfigs.size());
        assertEquals("dfs_name_dir_list", nnConfigs.get(0).getName());
        assertEquals("dfs_data_dir_list", dnConfigs.get(0).getName());
    }

    @Test
    void testAddRoleConfigsWithNoMatchingRefName() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        Map<String, List<ApiClusterTemplateConfig>> configs = new HashMap<>();
        configs.put("hdfs-NAMENODE-nomatch", List.of(new ApiClusterTemplateConfig().name("dfs_name_dir_list").variable("master_NAMENODE")));
        configs.put("hdfs-DATANODE-nomatch", List.of(new ApiClusterTemplateConfig().name("dfs_data_dir_list").variable("worker_DATANODE")));

        underTest.addRoleConfigs("HDFS", configs);

        ApiClusterTemplateService service = underTest.getTemplate().getServices().stream().filter(srv -> "HDFS".equals(srv.getServiceType())).findAny().get();
        ApiClusterTemplateRoleConfigGroup dn = service.getRoleConfigGroups().stream().filter(rcg -> "DATANODE".equals(rcg.getRoleType())).findAny().get();
        List<ApiClusterTemplateConfig> dnConfigs = dn.getConfigs();
        ApiClusterTemplateRoleConfigGroup nn = service.getRoleConfigGroups().stream().filter(rcg -> "NAMENODE".equals(rcg.getRoleType())).findAny().get();
        List<ApiClusterTemplateConfig> nnConfigs = nn.getConfigs();

        assertNull(nnConfigs);
        assertNull(dnConfigs);
    }

    @Test
    void addExistingRoleConfigs() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-existing-conf.bp"));
        Map<String, List<ApiClusterTemplateConfig>> configs = new HashMap<>();
        configs.put("hdfs-NAMENODE-BASE", List.of(new ApiClusterTemplateConfig().name("dfs_name_dir_list").variable("master_NAMENODE")));
        configs.put("hdfs-DATANODE-BASE", List.of(
                new ApiClusterTemplateConfig().name("dfs_data_dir_list").variable("worker_DATANODE"),
                new ApiClusterTemplateConfig().name("dfs_data_dir_list_2").variable("worker_DATANODE_2")));

        underTest.addRoleConfigs("HDFS", configs);

        ApiClusterTemplateService service = underTest.getTemplate().getServices().stream().filter(srv -> "HDFS".equals(srv.getServiceType())).findAny().get();
        ApiClusterTemplateRoleConfigGroup dn = service.getRoleConfigGroups().stream().filter(rcg -> "DATANODE".equals(rcg.getRoleType())).findAny().get();
        List<ApiClusterTemplateConfig> dnConfigs = dn.getConfigs();
        ApiClusterTemplateRoleConfigGroup nn = service.getRoleConfigGroups().stream().filter(rcg -> "NAMENODE".equals(rcg.getRoleType())).findAny().get();
        List<ApiClusterTemplateConfig> nnConfigs = nn.getConfigs();

        assertEquals(1, nnConfigs.size());
        assertEquals(4, dnConfigs.size());
        assertEquals("dfs_name_dir_list", nnConfigs.get(0).getName());
        ApiClusterTemplateConfig dfs1Config = dnConfigs.stream().filter(c -> "dfs_data_dir_list".equals(c.getName())).findFirst().get();
        assertEquals("/dfs/dn", dfs1Config.getValue());
        assertNull(dfs1Config.getVariable());
        ApiClusterTemplateConfig dfs2Config = dnConfigs.stream().filter(c -> "dfs_data_dir_list_2".equals(c.getName())).findFirst().get();
        assertEquals("worker_DATANODE_2", dfs2Config.getVariable());
        assertNull(dfs2Config.getValue());
    }

    @Test
    void testIsRoleTypePresentInServiceWithSingleRole() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("NAMENODE"));

        assertTrue(present);
    }

    @Test
    void testIsRoleTypePresentInServiceWithMultipleRole() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("DATANODE", "NAMENODE"));

        assertTrue(present);
    }

    @Test
    void testIsRoleTypePresentInServiceWithFakeRole() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("MYROLE"));

        assertFalse(present);
    }

    @Test
    void testAddInstantiatorWithBaseRoles() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        ClouderaManagerRepo clouderaManagerRepoDetails = new ClouderaManagerRepo();
        clouderaManagerRepoDetails.setVersion(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_6_3_0.getVersion());
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setClusterName("cluster");
        TemplatePreparationObject.Builder tpoBuilder = new TemplatePreparationObject.Builder().withGeneralClusterConfigs(generalClusterConfigs);
        TemplatePreparationObject templatePreparationObject = tpoBuilder.build();

        underTest.addInstantiator(clouderaManagerRepoDetails, templatePreparationObject, "dszabo-sdx");

        ApiClusterTemplateInstantiator instantiator = underTest.getTemplate().getInstantiator();
        List<ApiClusterTemplateRoleConfigGroupInfo> roleConfigGroups = instantiator.getRoleConfigGroups();
        List<String> refNames = roleConfigGroups.stream().map(ApiClusterTemplateRoleConfigGroupInfo::getRcgRefName).collect(Collectors.toList());

        assertEquals(2, refNames.size());
        assertTrue(refNames.containsAll(List.of("yarn-NODEMANAGER-BASE", "hdfs-DATANODE-BASE")));
        assertEquals("cluster", instantiator.getClusterName());
    }

    @Test
    void testAddInstantiatorWithoutBaseRoles() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-custom-ref.bp"));
        ClouderaManagerRepo clouderaManagerRepoDetails = new ClouderaManagerRepo();
        clouderaManagerRepoDetails.setVersion(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_6_3_0.getVersion());
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setClusterName("cluster");
        TemplatePreparationObject.Builder tpoBuilder = new TemplatePreparationObject.Builder().withGeneralClusterConfigs(generalClusterConfigs);
        TemplatePreparationObject templatePreparationObject = tpoBuilder.build();

        underTest.addInstantiator(clouderaManagerRepoDetails, templatePreparationObject, "dszabo-sdx");

        ApiClusterTemplateInstantiator instantiator = underTest.getTemplate().getInstantiator();
        List<ApiClusterTemplateRoleConfigGroupInfo> roleConfigGroups = instantiator.getRoleConfigGroups();

        assertNull(roleConfigGroups);
    }

    @Test
    void addInstantiatorKeepsCustomClusterName() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-custom_cluster_name.bp"));
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setClusterName("cluster");
        TemplatePreparationObject templatePreparationObject = new TemplatePreparationObject.Builder()
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();

        underTest.addInstantiator(null, templatePreparationObject, null);

        ApiClusterTemplateInstantiator instantiator = underTest.getTemplate().getInstantiator();
        assertEquals("kusztom", instantiator.getClusterName());
    }

    @Test
    void mapRoleRefsToServiceComponents() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        Map<String, ServiceComponent> expected = new HashMap<>();
        expected.put("hbase-MASTER-BASE", ServiceComponent.of("HBASE", "MASTER"));
        expected.put("hbase-REGIONSERVER-BASE", ServiceComponent.of("HBASE", "REGIONSERVER"));
        expected.put("hdfs-BALANCER-BASE", ServiceComponent.of("HDFS", "BALANCER"));
        expected.put("hdfs-DATANODE-BASE", ServiceComponent.of("HDFS", "DATANODE"));
        expected.put("hdfs-NAMENODE-BASE", ServiceComponent.of("HDFS", "NAMENODE"));
        expected.put("hdfs-SECONDARYNAMENODE-BASE", ServiceComponent.of("HDFS", "SECONDARYNAMENODE"));
        expected.put("hive-GATEWAY-BASE", ServiceComponent.of("HIVE", "GATEWAY"));
        expected.put("hive-HIVEMETASTORE-BASE", ServiceComponent.of("HIVE", "HIVEMETASTORE"));
        expected.put("hive-HIVESERVER2-BASE", ServiceComponent.of("HIVE", "HIVESERVER2"));
        expected.put("impala-CATALOGSERVER-BASE", ServiceComponent.of("IMPALA", "CATALOGSERVER"));
        expected.put("impala-IMPALAD-BASE", ServiceComponent.of("IMPALA", "IMPALAD"));
        expected.put("impala-STATESTORE-BASE", ServiceComponent.of("IMPALA", "STATESTORE"));
        expected.put("kafka-KAFKA_BROKER-BASE", ServiceComponent.of("KAFKA", "KAFKA_BROKER"));
        expected.put("spark_on_yarn-GATEWAY-BASE", ServiceComponent.of("SPARK_ON_YARN", "GATEWAY"));
        expected.put("spark_on_yarn-SPARK_YARN_HISTORY_SERVER-BASE", ServiceComponent.of("SPARK_ON_YARN", "SPARK_YARN_HISTORY_SERVER"));
        expected.put("yarn-JOBHISTORY-BASE", ServiceComponent.of("YARN", "JOBHISTORY"));
        expected.put("yarn-NODEMANAGER-BASE", ServiceComponent.of("YARN", "NODEMANAGER"));
        expected.put("yarn-RESOURCEMANAGER-BASE", ServiceComponent.of("YARN", "RESOURCEMANAGER"));
        expected.put("zookeeper-SERVER-BASE", ServiceComponent.of("ZOOKEEPER", "SERVER"));

        Map<String, ServiceComponent> actual = underTest.mapRoleRefsToServiceComponents();

        assertSortedEquals(expected, actual);
    }

    @Test
    void testGetAllComponents() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        Set<ServiceComponent> expected = Set.of(
                ServiceComponent.of("HBASE", "MASTER"),
                ServiceComponent.of("HBASE", "REGIONSERVER"),
                ServiceComponent.of("HDFS", "BALANCER"),
                ServiceComponent.of("HDFS", "DATANODE"),
                ServiceComponent.of("HDFS", "NAMENODE"),
                ServiceComponent.of("HDFS", "SECONDARYNAMENODE"),
                ServiceComponent.of("HIVE", "GATEWAY"),
                ServiceComponent.of("HIVE", "HIVEMETASTORE"),
                ServiceComponent.of("HIVE", "HIVESERVER2"),
                ServiceComponent.of("IMPALA", "CATALOGSERVER"),
                ServiceComponent.of("IMPALA", "IMPALAD"),
                ServiceComponent.of("IMPALA", "STATESTORE"),
                ServiceComponent.of("KAFKA", "KAFKA_BROKER"),
                ServiceComponent.of("SPARK_ON_YARN", "GATEWAY"),
                ServiceComponent.of("SPARK_ON_YARN", "SPARK_YARN_HISTORY_SERVER"),
                ServiceComponent.of("YARN", "JOBHISTORY"),
                ServiceComponent.of("YARN", "NODEMANAGER"),
                ServiceComponent.of("YARN", "RESOURCEMANAGER"),
                ServiceComponent.of("ZOOKEEPER", "SERVER")
        );

        Set<ServiceComponent> actual = underTest.getAllComponents();

        assertSortedEquals(expected, actual);
    }

    @Test
    void testGetServiceComponentsByHostGroup() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        Map<String, Set<ServiceComponent>> expected = Map.of(
                "master", Set.of(
                        ServiceComponent.of("HBASE", "MASTER"),
                        ServiceComponent.of("HDFS", "BALANCER"),
                        ServiceComponent.of("HDFS", "NAMENODE"),
                        ServiceComponent.of("HDFS", "SECONDARYNAMENODE"),
                        ServiceComponent.of("HIVE", "GATEWAY"),
                        ServiceComponent.of("HIVE", "HIVEMETASTORE"),
                        ServiceComponent.of("HIVE", "HIVESERVER2"),
                        ServiceComponent.of("IMPALA", "CATALOGSERVER"),
                        ServiceComponent.of("IMPALA", "STATESTORE"),
                        ServiceComponent.of("KAFKA", "KAFKA_BROKER"),
                        ServiceComponent.of("SPARK_ON_YARN", "GATEWAY"),
                        ServiceComponent.of("SPARK_ON_YARN", "SPARK_YARN_HISTORY_SERVER"),
                        ServiceComponent.of("YARN", "JOBHISTORY"),
                        ServiceComponent.of("YARN", "RESOURCEMANAGER"),
                        ServiceComponent.of("ZOOKEEPER", "SERVER")
                ),
                "worker", Set.of(
                        ServiceComponent.of("HBASE", "REGIONSERVER"),
                        ServiceComponent.of("HDFS", "DATANODE"),
                        ServiceComponent.of("HIVE", "GATEWAY"),
                        ServiceComponent.of("IMPALA", "IMPALAD"),
                        ServiceComponent.of("SPARK_ON_YARN", "GATEWAY"),
                        ServiceComponent.of("YARN", "NODEMANAGER")
                )
        );

        Map<String, Set<ServiceComponent>> actual = underTest.getServiceComponentsByHostGroup();

        assertEquals(expected.keySet(), actual.keySet());
        expected.keySet().forEach(k -> assertSortedEquals(expected.get(k), actual.get(k)));
    }

    @Test
    void testGetComponentsInHostGroup() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        Set<String> expected = Set.of("REGIONSERVER", "DATANODE", "GATEWAY", "IMPALAD", "NODEMANAGER");

        Set<String> actual = underTest.getComponentsInHostGroup("worker");

        assertSortedEquals(expected, actual);
    }

    @Test
    void testGetHostGroupsWithComponent() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        assertEquals(Set.of(), underTest.getHostGroupsWithComponent("FAKE_COMPONENT"));
        assertEquals(Set.of("master"), underTest.getHostGroupsWithComponent("HIVEMETASTORE"));
        assertEquals(Set.of("worker"), underTest.getHostGroupsWithComponent("DATANODE"));
        assertEquals(Set.of("master", "worker"), underTest.getHostGroupsWithComponent("GATEWAY"));
    }

    @Test
    void testExtendTemplateWithAdditionalServicesWithNoAdditionalServices() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        String originalTemplate = templateToString(underTest.getTemplate());

        underTest.extendTemplateWithAdditionalServices(Map.of());

        String actualTemplate = templateToString(underTest.getTemplate());
        assertEquals(originalTemplate, actualTemplate);
    }

    @Test
    void testExtendTemplateWithAdditionalServicesWithKnoxService() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        ApiClusterTemplateService knox = new ApiClusterTemplateService().serviceType("KNOX").refName("knox");
        ApiClusterTemplateRoleConfigGroup knoxGateway = new ApiClusterTemplateRoleConfigGroup()
                .roleType("KNOX_GATEWAY").base(true).refName("knox-KNOX_GATEWAY-BASE");
        knox.roleConfigGroups(List.of(knoxGateway));

        underTest.extendTemplateWithAdditionalServices(Map.of("master", knox));
        String actualTemplate = templateToString(underTest.getTemplate());

        CmTemplateProcessor expectedProcessor = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-knox.bp"));
        String expectedTemplate = templateToString(expectedProcessor.getTemplate());
        assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    void testExtendTemplateWithAdditionalServicesWithKnoxServiceAndMultipleGateway() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-multi-gateway.bp"));
        ApiClusterTemplateService knox = new ApiClusterTemplateService().serviceType("KNOX").refName("knox");
        ApiClusterTemplateRoleConfigGroup knoxGateway = new ApiClusterTemplateRoleConfigGroup()
                .roleType("KNOX_GATEWAY").base(true).refName("knox-KNOX_GATEWAY-BASE");
        knox.roleConfigGroups(List.of(knoxGateway));

        underTest.extendTemplateWithAdditionalServices(Map.of("master", knox, "master2", knox));
        String actualTemplate = templateToString(underTest.getTemplate());

        CmTemplateProcessor expectedProcessor = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-knox-multi-gateway.bp"));
        String expectedTemplate = templateToString(expectedProcessor.getTemplate());
        assertEquals(expectedTemplate, actualTemplate);
    }

    @Test
    void danglingVariablesAreRemoved() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-variables.bp"));

        underTest.removeDanglingVariableReferences();

        Optional<ApiClusterTemplateService> zookeeper = underTest.getServiceByType("ZOOKEEPER");
        assertTrue(zookeeper.isPresent());
        assertNull(zookeeper.get().getServiceConfigs());

        Optional<ApiClusterTemplateService> impala = underTest.getServiceByType("IMPALA");
        assertTrue(impala.isPresent());
        assertEquals(List.of(configVar("other_service_config", "variable_with_value")), impala.get().getServiceConfigs());
    }

    @Test
    void getCardinalityByHostGroup() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-multi-gateway.bp"));

        Map<String, InstanceCount> expected = Map.of(
                "master", InstanceCount.EXACTLY_ONE,
                "master2", InstanceCount.EXACTLY_ONE,
                "worker", InstanceCount.ONE_OR_MORE
        );
        assertEquals(expected, underTest.getCardinalityByHostGroup());

        underTest = new CmTemplateProcessor(getBlueprintText("input/cdp-data-mart.bp"));

        expected = Map.of(
                "master", InstanceCount.EXACTLY_ONE,
                "worker", InstanceCount.ONE_OR_MORE,
                "compute", InstanceCount.ZERO_OR_MORE
        );
        assertEquals(expected, underTest.getCardinalityByHostGroup());

        underTest = new CmTemplateProcessor(getBlueprintText("input/namenode-ha.bp"));

        expected = Map.of(
                "master", InstanceCount.exactly(2),
                "quorum", InstanceCount.atLeast(3),
                "gateway", InstanceCount.EXACTLY_ONE,
                "worker", InstanceCount.atLeast(3)
        );
        assertEquals(expected, underTest.getCardinalityByHostGroup());
    }

    @Test
    void recommendGatewayTestWhenNameAndCardinality() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-multi-gateway.bp"));
        assertEquals(new GatewayRecommendation(Set.of("master")), underTest.recommendGateway());

        underTest = new CmTemplateProcessor(getBlueprintText("input/namenode-ha.bp"));
        assertEquals(new GatewayRecommendation(Set.of("gateway")), underTest.recommendGateway());

        underTest = new CmTemplateProcessor(getBlueprintText("input/namenode-ha-single-worker.bp"));
        assertEquals(new GatewayRecommendation(Set.of("worker")), underTest.recommendGateway());

        underTest = new CmTemplateProcessor(getBlueprintText("input/namenode-ha-no-gateway.bp"));
        assertEquals(new GatewayRecommendation(Set.of()), underTest.recommendGateway());
    }

    @Test
    void recommendGatewayTestWhenExplicitAndSingleEntryAndNoMatch() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/namenode-ha-single-worker-with-knox-of-0.bp"));

        GatewayRecommendation result = underTest.recommendGateway();
        assertThat(result).isEqualTo(new GatewayRecommendation(Set.of("worker")));
    }

    @ParameterizedTest(name = "path={0}")
    @ValueSource(strings = {"input/namenode-ha-with-knox.bp", "input/namenode-ha-single-worker-with-knox-of-2.bp"})
    void recommendGatewayTestWhenExplicitAndSingleEntryAndMatch(String path) {
        underTest = new CmTemplateProcessor(getBlueprintText(path));

        GatewayRecommendation result = underTest.recommendGateway();
        assertThat(result).isEqualTo(new GatewayRecommendation(Set.of("yolo")));
    }

    @Test
    void recommendGatewayTestWhenExplicitAndMultipleEntriesButNoMatch() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/namenode-ha-single-worker-with-knoxes-of-0-0.bp"));

        GatewayRecommendation result = underTest.recommendGateway();
        assertThat(result).isEqualTo(new GatewayRecommendation(Set.of("worker")));
    }

    @Test
    void recommendGatewayTestWhenExplicitAndMultipleEntriesAndOneMatch() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/namenode-ha-single-worker-with-knoxes-of-0-2.bp"));

        GatewayRecommendation result = underTest.recommendGateway();
        assertThat(result).isEqualTo(new GatewayRecommendation(Set.of("entrance")));
    }

    @Test
    void recommendGatewayTestWhenExplicitAndMultipleEntriesAndTwoMatchesAndSameCounts() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/namenode-ha-single-worker-with-knoxes-of-2-2.bp"));

        GatewayRecommendation result = underTest.recommendGateway();
        assertThat(result).isEqualTo(new GatewayRecommendation(Set.of("door")));
    }

    @Test
    void recommendGatewayTestWhenExplicitAndMultipleEntriesAndTwoMatchesAndDifferentCounts() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/namenode-ha-single-worker-with-knoxes-of-1-2.bp"));

        GatewayRecommendation result = underTest.recommendGateway();
        assertThat(result).isEqualTo(new GatewayRecommendation(Set.of("window")));
    }

    @ParameterizedTest(name = "path={0}")
    @ValueSource(strings = {"input/namenode-ha-single-worker-with-knoxes-of-1-1-2.bp", "input/namenode-ha-single-worker-with-knoxes-of-1-2-3.bp"})
    void recommendGatewayTestWhenExplicitAndMultipleEntriesAndThreeMatches(String path) {
        underTest = new CmTemplateProcessor(getBlueprintText(path));

        GatewayRecommendation result = underTest.recommendGateway();
        assertThat(result).isEqualTo(new GatewayRecommendation(Set.of("entrance")));
    }

    @Test
    void recommendAutoscale() {
        Versioned blueprintVersion = () -> "7.2.11";

        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-multi-gateway.bp"));
        assertEquals(new AutoscaleRecommendation(Set.of(), Set.of()), underTest
                .recommendAutoscale(blueprintVersion, List.of()));

        underTest = new CmTemplateProcessor(getBlueprintText("input/namenode-ha.bp"));
        assertEquals(new AutoscaleRecommendation(Set.of("gateway"), Set.of()), underTest.recommendAutoscale(blueprintVersion, List.of()));

        underTest = new CmTemplateProcessor(getBlueprintText("input/kafka.bp"));
        assertEquals(new AutoscaleRecommendation(Set.of(), Set.of()), underTest.recommendAutoscale(blueprintVersion, List.of()));

        underTest = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));
        assertEquals(new AutoscaleRecommendation(Set.of("compute"), Set.of("compute")), underTest.recommendAutoscale(blueprintVersion, List.of()));

        underTest = new CmTemplateProcessor(getBlueprintText("input/data-mart.bp"));
        assertEquals(new AutoscaleRecommendation(Set.of(), Set.of()), underTest.recommendAutoscale(blueprintVersion, List.of()));

        underTest = new CmTemplateProcessor(getBlueprintText("input/cdp-streaming-small.bp"));
        assertEquals(new AutoscaleRecommendation(Set.of("kraft"), Set.of()), underTest.recommendAutoscale(blueprintVersion, List.of()));

        underTest = new CmTemplateProcessor(getBlueprintText("input/data-mart.bp"));
        assertEquals(new AutoscaleRecommendation(Set.of("executor"), Set.of()), underTest.recommendAutoscale(blueprintVersion,
                List.of("DATAHUB_IMPALA_SCHEDULE_BASED_SCALING")));
    }

    @Test
    void recommendResize() {
        Versioned blueprintVersion = () -> "7.2.11";

        underTest = new CmTemplateProcessor(getBlueprintText("input/kafka.bp"));
        assertEquals(new ResizeRecommendation(Set.of("broker", "quorum"), Set.of("broker", "quorum")), underTest.recommendResize(List.of(), blueprintVersion));

        underTest = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));
        Set<String> hostGroups = Set.of("gateway", "compute", "worker");
        assertEquals(new ResizeRecommendation(hostGroups, hostGroups), underTest.recommendResize(List.of(), blueprintVersion));

        underTest = new CmTemplateProcessor(getBlueprintText("input/cb5660.bp"));
        hostGroups = Set.of("gateway", "quorum", "worker", "compute");
        assertEquals(new ResizeRecommendation(hostGroups, hostGroups), underTest.recommendResize(List.of(), blueprintVersion));

        underTest = new CmTemplateProcessor(getBlueprintText("input/nifi.bp"));
        assertEquals(new ResizeRecommendation(Set.of(), Set.of()), underTest.recommendResize(List.of(), blueprintVersion));
    }

    @Test
    void getHostTemplates() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/cdp-invalid-multi-host-template-name.bp"));
        assertEquals(3, underTest.getHostTemplateNames().size());
        assertEquals(2, underTest.getHostTemplateNames().stream().filter("master"::equals).count());
    }

    @Test
    void testGetComputeHostGroups() {
        Versioned blueprintVersion = () -> "7.2.11";

        underTest = new CmTemplateProcessor(getBlueprintText("input/custom-hostgroups-for-nms.bp"));
        assertEquals(2, underTest.getComputeHostGroups(blueprintVersion).size());
    }

    @Test
    void testYARNServiceAttributes() {
        Versioned blueprintVersion = () -> "7.2.11";

        underTest = new CmTemplateProcessor(getBlueprintText("input/custom-hostgroups-for-nms.bp"));
        assertEquals(7, underTest.getHostTemplateNames().size());
        Map<String, Map<String, ServiceAttributes>> attrs = underTest.getHostGroupBasedServiceAttributes(blueprintVersion);
        assertEquals(4, attrs.size());

        Map<String, ServiceAttributes> serviceAttributesMap;

        serviceAttributesMap = attrs.get("worker");
        assertEquals(1, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().size());
        assertEquals(YarnConstants.ATTRIBUTE_NAME_NODE_INSTANCE_TYPE, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().keySet().iterator().next());
        assertEquals(YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_WORKER, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().values().iterator().next());

        serviceAttributesMap = attrs.get("compute");
        assertEquals(1, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().size());
        assertEquals(YarnConstants.ATTRIBUTE_NAME_NODE_INSTANCE_TYPE, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().keySet().iterator().next());
        assertEquals(YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_COMPUTE, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().values().iterator().next());

        // Verify that custom hostGroup names also get marked as "compute" or "worker"
        serviceAttributesMap = attrs.get("customnm1");
        assertEquals(1, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().size());
        assertEquals(YarnConstants.ATTRIBUTE_NAME_NODE_INSTANCE_TYPE, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().keySet().iterator().next());
        assertEquals(YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_WORKER, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().values().iterator().next());

        serviceAttributesMap = attrs.get("customnm2");
        assertEquals(1, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().size());
        assertEquals(YarnConstants.ATTRIBUTE_NAME_NODE_INSTANCE_TYPE, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().keySet().iterator().next());
        assertEquals(YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_COMPUTE, serviceAttributesMap.get(YarnRoles.YARN).getAttributes().values().iterator().next());
    }

    @Test
    void testHostWithUpperCase() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-host-with-uppercase.bp"));
        Set<String> hosts = Set.of("master", "executor", "coordinator");
        assertTrue(underTest.getTemplate().getHostTemplates().stream().allMatch(ht -> hosts.contains(ht.getRefName())));
    }

    @Test
    void addHostsTest() {
        Map<String, List<Map<String, String>>> hostGroupMappings = Map.ofEntries(entry("hostGroup1", List.of()),
                entry("hostGroup2", List.of(hostAttributes("host2_1", false, null), hostAttributes("host2_2", true, null))),
                entry("hostGroup3", List.of(hostAttributes("host3_1", true, ""), hostAttributes("host3_2", true, "/rack3_2"))));

        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        ApiClusterTemplate template = underTest.getTemplate();
        ApiClusterTemplateInstantiator instantiator = new ApiClusterTemplateInstantiator();
        template.setInstantiator(instantiator);

        underTest.addHosts(hostGroupMappings);

        List<ApiClusterTemplateHostInfo> hosts = instantiator.getHosts();
        assertThat(hosts).hasSize(4);
        verifyHostInfo(hosts, "hostGroup2", "host2_1", null);
        verifyHostInfo(hosts, "hostGroup2", "host2_2", null);
        verifyHostInfo(hosts, "hostGroup3", "host3_1", null);
        verifyHostInfo(hosts, "hostGroup3", "host3_2", "/rack3_2");
    }

    @Test
    void testIfCustomServiceConfigsAreAddedWithSafetyValue() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));
        List<ApiClusterTemplateConfig> zeppelinConfig = List.of(
                new ApiClusterTemplateConfig().name("ZEPPELIN_service_env_safety_valve")
                        .value("HADOOP_CLIENT_OPTS=-Djdk.tls.maxHandshakeMessageSize=262144\nJAVA_OPTS=-Djdk.tls.maxHandshakeMessageSize=262144")
        );
        ApiClusterTemplateService zeppelin = underTest.getTemplate().getServices()
                .stream()
                .filter(service -> "ZEPPELIN".equals(service.getServiceType()))
                .findFirst().get();

        List<ApiClusterTemplateConfig> existingZeppelinConfig = zeppelin.getServiceConfigs();
        underTest.mergeCustomServiceConfigs(zeppelin, zeppelinConfig);
        List<String> actualValues = List.of(existingZeppelinConfig.get(3).getValue().split("\n"));

        assertEquals(4, zeppelin.getServiceConfigs().size());
        assertEquals("HADOOP_CLIENT_OPTS=-Djdk.tls.maxHandshakeMessageSize=262144\nJAVA_OPTS=-Djdk.tls.maxHandshakeMessageSize=262144",
                existingZeppelinConfig.get(3).getValue());
        assertEquals(2, actualValues.size());
        assertEquals("HADOOP_CLIENT_OPTS=-Djdk.tls.maxHandshakeMessageSize=262144", actualValues.getFirst());
        assertEquals("JAVA_OPTS=-Djdk.tls.maxHandshakeMessageSize=262144", actualValues.getLast());
    }

    @Test
    void testIfCustomServiceConfigsAreMerged() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));
        // config not present in template
        List<ApiClusterTemplateConfig> sparkConfigs = List.of(
                new ApiClusterTemplateConfig().name("spark_drive_log_persist_to_dfs").value("false")
        );
        // config present in template
        List<ApiClusterTemplateConfig> hiveConfigs = List.of(
                new ApiClusterTemplateConfig().name("tez_auto_reducer_parallelism").value("true"),
                new ApiClusterTemplateConfig().name("hive_service_config_safety_valve")
                        .value("<property><name>hive_server2_tez_session_lifetime</name><value>30m</value></property>")
        );
        ApiClusterTemplateService spark = underTest.getTemplate().getServices()
                .stream()
                .filter(service -> "SPARK_ON_YARN".equals(service.getServiceType()))
                .findFirst().get();
        ApiClusterTemplateService hive = underTest.getTemplate().getServices()
                .stream()
                .filter(service -> "HIVE_ON_TEZ".equals(service.getServiceType()))
                .findFirst().get();
        List<ApiClusterTemplateConfig> existingSparkConfigs = spark.getServiceConfigs();
        List<ApiClusterTemplateConfig> existingHiveConfigs = hive.getServiceConfigs();
        underTest.mergeCustomServiceConfigs(spark, sparkConfigs);
        underTest.mergeCustomServiceConfigs(hive, hiveConfigs);
        assertEquals(spark.getServiceConfigs().size(), (existingSparkConfigs == null ? 0 : existingSparkConfigs.size()) + sparkConfigs.size());
        assertEquals(hive.getServiceConfigs().size(), existingHiveConfigs.size());
        assertTrue(existingHiveConfigs.get(2).getValue().endsWith("<property><name>hive_server2_tez_session_lifetime</name><value>30m</value></property>"));
    }

    @Test
    void mergeHueSafetyValuesIniFile() {
        when(iniFileFactory.create()).thenReturn(safetyValveService, safetyValveRole);
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-existing-conf.bp"), iniFileFactory);

        ApiClusterTemplateService hue = underTest.getTemplate().getServices()
                .stream()
                .filter(service -> "HUE".equals(service.getServiceType()))
                .findFirst().get();

        List<ApiClusterTemplateConfig> serviceConfigsAppend = new ArrayList<>();
        serviceConfigsAppend.add(new ApiClusterTemplateConfig()
                .name("hue_service_safety_valve")
                .value("[desktop]\n[[knox]]\nknox_proxyhosts=foo.com"));

        String expectedSafetyValveValueService =
                "[desktop]\napp_blacklist=spark,zookeeper,hbase,impala,search,sqoop,security,pig\n[[knox]]\nknox_proxyhosts=foo.com";
        when(safetyValveService.print()).thenReturn(expectedSafetyValveValueService);

        underTest.mergeCustomServiceConfigs(hue, serviceConfigsAppend);

        verify(safetyValveService).addContent("[desktop]\n[[knox]]\nknox_proxyhosts=foo.com");
        verify(safetyValveService).addContent("[desktop]\napp_blacklist=spark,zookeeper,hbase,impala,search,sqoop,security,pig");
    }

    @Test
    void testIfSingleValuedCustomRoleConfigAreMerged() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/de.bp"));

        // config not present in template
        List<ApiClusterTemplateConfig> sparkConfigs = List.of(
                new ApiClusterTemplateConfig().name("spark-conf/spark-defaults.conf_client_config_safety_valve")
                        .value("spark.acls.enable=true"));
        List<ApiClusterTemplateRoleConfigGroup> apiClusterTemplateRoleConfigGroupList = List.of(new ApiClusterTemplateRoleConfigGroup()
                .roleType("GATEWAY").configs(sparkConfigs));

        ApiClusterTemplateService sparkOnYarn = underTest.getTemplate().getServices()
                .stream()
                .filter(service -> "SPARK_ON_YARN".equals(service.getServiceType()))
                .findFirst().get();

        underTest.mergeCustomRoleConfigs(sparkOnYarn, apiClusterTemplateRoleConfigGroupList);
        String expectedValue = "spark.hadoop.fs.s3a.ssl.channel.mode=openssl\n" +
                "spark.hadoop.mapreduce.fileoutputcommitter.algorithm.version=1\nspark.acls.enable=true";

        String finalValue = sparkOnYarn.getRoleConfigGroups().stream()
                .filter(i -> i.getRoleType().equals("GATEWAY")).findAny().get().getConfigs().get(0).getValue();

        assertEquals(expectedValue, finalValue);
    }

    @Test
    void testIfMultipleValuedCustomRoleConfigAreMerged() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/de.bp"));

        // config not present in template
        List<ApiClusterTemplateConfig> sparkConfigs = List.of(
                new ApiClusterTemplateConfig().name("spark-conf/spark-defaults.conf_client_config_safety_valve")
                        .value("spark.acls.enable=true\nspark.ui.view.acls=*\nspark.ui.view.acls.groups=*"));
        List<ApiClusterTemplateRoleConfigGroup> apiClusterTemplateRoleConfigGroupList = List.of(new ApiClusterTemplateRoleConfigGroup()
                .roleType("GATEWAY").configs(sparkConfigs));

        ApiClusterTemplateService sparkOnYarn = underTest.getTemplate().getServices()
                .stream()
                .filter(service -> "SPARK_ON_YARN".equals(service.getServiceType()))
                .findFirst().get();

        underTest.mergeCustomRoleConfigs(sparkOnYarn, apiClusterTemplateRoleConfigGroupList);
        String expectedValue = "spark.hadoop.fs.s3a.ssl.channel.mode=openssl\n" +
                "spark.hadoop.mapreduce.fileoutputcommitter.algorithm.version=1\n" +
                "spark.acls.enable=true\nspark.ui.view.acls=*\nspark.ui.view.acls.groups=*";

        String finalValue = sparkOnYarn.getRoleConfigGroups().stream()
                .filter(i -> i.getRoleType().equals("GATEWAY")).findAny().get().getConfigs().get(0).getValue();

        assertEquals(expectedValue, finalValue);
    }

    @Test
    void testIfMultipleValuedCustomRoleConfigAreMergedInDifferentManner() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/de.bp"));

        // config not present in template
        List<ApiClusterTemplateConfig> sparkConfigs = List.of(
                new ApiClusterTemplateConfig().name("spark-conf/spark-defaults.conf_client_config_safety_valve")
                        .value("spark.acls.enable=true\\nspark.ui.view.acls=*\\nspark.ui.view.acls.groups=*"));
        List<ApiClusterTemplateRoleConfigGroup> apiClusterTemplateRoleConfigGroupList = List.of(new ApiClusterTemplateRoleConfigGroup()
                .roleType("GATEWAY").configs(sparkConfigs));

        ApiClusterTemplateService sparkOnYarn = underTest.getTemplate().getServices()
                .stream()
                .filter(service -> "SPARK_ON_YARN".equals(service.getServiceType()))
                .findFirst().get();

        underTest.mergeCustomRoleConfigs(sparkOnYarn, apiClusterTemplateRoleConfigGroupList);
        String expectedValue = "spark.hadoop.fs.s3a.ssl.channel.mode=openssl\n" +
                "spark.hadoop.mapreduce.fileoutputcommitter.algorithm.version=1\n" +
                "spark.acls.enable=true\nspark.ui.view.acls=*\nspark.ui.view.acls.groups=*";

        String finalValue = sparkOnYarn.getRoleConfigGroups().stream()
                .filter(i -> i.getRoleType().equals("GATEWAY")).findAny().get().getConfigs().get(0).getValue();

        assertEquals(expectedValue, finalValue);
    }

    @Test
    void testIfCustomRoleConfigsAreMerged() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));
        // present in cluster template/blueprint
        List<ApiClusterTemplateConfig> hs2RoleConfigs = List.of(new ApiClusterTemplateConfig().name("hiveserver2_mv_files_thread").value("30"));
        List<ApiClusterTemplateRoleConfigGroup> hs2Rcg = List.of(new ApiClusterTemplateRoleConfigGroup().roleType("hiveserver2").configs(hs2RoleConfigs));
        // not present in cluster template/blueprint
        List<ApiClusterTemplateConfig> gatewayRoleConfigs = List.of(new ApiClusterTemplateConfig().name("hive_client_java_heapsize").value("6442450944"));
        List<ApiClusterTemplateRoleConfigGroup> gatewayRcg = List.of(new ApiClusterTemplateRoleConfigGroup().roleType("gateway").configs(gatewayRoleConfigs));

        ApiClusterTemplateService hive = underTest.getTemplate().getServices()
                .stream()
                .filter(service -> "HIVE_ON_TEZ".equals(service.getServiceType()))
                .findFirst().get();

        List<ApiClusterTemplateConfig> existingGatewayConfigs = hive.getRoleConfigGroups().get(0).getConfigs();
        List<ApiClusterTemplateConfig> existingHs2Configs = hive.getRoleConfigGroups().get(1).getConfigs();

        underTest.mergeCustomRoleConfigs(hive, hs2Rcg);
        underTest.mergeCustomRoleConfigs(hive, gatewayRcg);

        assertEquals(existingHs2Configs.size(), hive.getRoleConfigGroups().get(1).getConfigs().size());
        assertEquals((existingGatewayConfigs == null
                ? 0
                : existingGatewayConfigs.size()) + gatewayRoleConfigs.size(), hive.getRoleConfigGroups().get(0).getConfigs().size());
    }

    @Test
    void testMergeCustomRoleConfigsWithMultipleRCGsOfTheSameRoleType() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));

        List<ApiClusterTemplateConfig> configs = List.of(new ApiClusterTemplateConfig().name("nodemanager_java_heapsize").value("123456789"),
                new ApiClusterTemplateConfig().name("nodemanager_resource_cpu_vcores").value("8"));
        List<ApiClusterTemplateRoleConfigGroup> nodeManagerRcgs = List.of(new ApiClusterTemplateRoleConfigGroup().roleType("NODEMANAGER").configs(configs));

        ApiClusterTemplateService yarn = underTest.getServiceByType("YARN").get();

        underTest.mergeCustomRoleConfigs(yarn, nodeManagerRcgs);

        List<ApiClusterTemplateConfig> resultingWorkerConfigs = yarn.getRoleConfigGroups().stream()
                .filter(role -> role.getRefName().contains("WORKER")).findAny().get().getConfigs();
        List<ApiClusterTemplateConfig> resultingComputeConfigs = yarn.getRoleConfigGroups().stream()
                .filter(role -> role.getRefName().contains("COMPUTE")).findAny().get().getConfigs();
        assertThat(resultingWorkerConfigs).hasSameElementsAs(configs);
        assertThat(resultingComputeConfigs).hasSameElementsAs(configs);
    }

    @Test
    void testIfCustomServiceConfigsMapIsRetrievedCorrectly() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));
        Map<String, List<ApiClusterTemplateConfig>> expectedMap = new HashMap<>();
        Set<CustomConfigurationPropertyView> configs = Set.of(new CustomConfigurationPropertyView("property1", "value1", null, "service1"),
                new CustomConfigurationPropertyView("property2", "value2", "role1", "service2"),
                new CustomConfigurationPropertyView("property3", "value3", null, "service3"));
        expectedMap.put("service1", List.of(new ApiClusterTemplateConfig().name("property1").value("value1")));
        expectedMap.put("service3", List.of(new ApiClusterTemplateConfig().name("property3").value("value3")));
        assertEquals(expectedMap, underTest.getCustomServiceConfigsMap(configs));
    }

    @Test
    void testIfCustomRoleConfigsMapIsRetrievedCorrectly() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));
        Map<String, List<ApiClusterTemplateRoleConfigGroup>> expectedMap = new HashMap<>();
        Set<CustomConfigurationPropertyView> configs = Set.of(new CustomConfigurationPropertyView("property3", "value3", "role3", "service2"),
                new CustomConfigurationPropertyView("property2", "value2", "role2", "service1"),
                new CustomConfigurationPropertyView("property4", "value4", null, "service1"));
        expectedMap.put("service2", List.of(new ApiClusterTemplateRoleConfigGroup()
                .roleType("role3")
                .addConfigsItem(new ApiClusterTemplateConfig()
                .name("property3")
                .value("value3"))));
        expectedMap.put("service1", List.of(new ApiClusterTemplateRoleConfigGroup()
                .roleType("role2")
                .addConfigsItem(new ApiClusterTemplateConfig()
                .name("property2")
                .value("value2"))));
        assertEquals(expectedMap, underTest.getCustomRoleConfigsMap(configs));
    }

    private Map<String, String> hostAttributes(String hostname, boolean withRackId, String rackId) {
        Map<String, String> hostAttributes;
        if (withRackId) {
            if (rackId == null) {
                hostAttributes = new HashMap<>();
                hostAttributes.put(ClusterHostAttributes.FQDN, hostname);
                hostAttributes.put(ClusterHostAttributes.RACK_ID, null);
            } else {
                hostAttributes = Map.ofEntries(entry(ClusterHostAttributes.FQDN, hostname), entry(ClusterHostAttributes.RACK_ID, rackId));
            }
        } else {
            hostAttributes = Map.ofEntries(entry(ClusterHostAttributes.FQDN, hostname));
        }
        return hostAttributes;
    }

    private void verifyHostInfo(List<ApiClusterTemplateHostInfo> hosts, String hostTemplateRefNameExpected, String hostNameExpected, String rackIdExpected) {
        Optional<ApiClusterTemplateHostInfo> hostInfoOptional = hosts.stream()
                .filter(h -> h.getHostName().equals(hostNameExpected))
                .findFirst();
        assertThat(hostInfoOptional).isNotEmpty();

        ApiClusterTemplateHostInfo hostInfo = hostInfoOptional.get();
        assertThat(hostInfo).isNotNull();
        assertThat(hostInfo.getHostTemplateRefName()).isEqualTo(hostTemplateRefNameExpected);
        assertThat(hostInfo.getRackId()).isEqualTo(rackIdExpected);
    }

    @Test
    void getHostTemplateRoleNames() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/cdp-invalid-multi-host-template-name.bp"));
        assertEquals(5, underTest.getHostTemplateRoleNames("worker").size());
        assertEquals(1, underTest.getHostTemplateRoleNames("worker").stream().filter("hdfs-DATANODE-BASE"::equals).count());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "hosttemplate-role-config-ref-names-null",
            "hosttemplate-role-config-ref-names-missing",
            "hosttemplate-role-config-ref-names-empty"})
    void tesGetServiceComponentsByHostGroup(String caseInputFileSuffix) {
        String inputFilePath = String.format("input/%s.bp", caseInputFileSuffix);
        underTest = new CmTemplateProcessor(getBlueprintText(inputFilePath));

        Map<String, Set<ServiceComponent>> serviceComponentsByHostGroup = underTest.getServiceComponentsByHostGroup();
        assertThat(serviceComponentsByHostGroup).isNotEmpty();
        assertThat(serviceComponentsByHostGroup).containsKey("raz_scale_out");
        assertThat(serviceComponentsByHostGroup.get("raz_scale_out")).isEmpty();
    }

    private static Stream<Arguments> testIsServiceTypePresentArguments() {
        return Stream.of(
                Arguments.of(true, "input/de.bp", "HDFS"),
                Arguments.of(true, "input/de.bp", "HIVE"),
                Arguments.of(true, "input/de.bp", "HUE"),
                Arguments.of(true, "input/de.bp", "ZOOKEEPER"),
                Arguments.of(false, "input/de.bp", "LAKEHOUSE_OPTIMIZER"),
                Arguments.of(false, "input/de.bp", "FAKE_SERVICE"),
                Arguments.of(false, "input/de.bp", ""),
                Arguments.of(false, "input/de.bp", null)
        );
    }

    @MethodSource("testIsServiceTypePresentArguments")
    @ParameterizedTest
    void testIsServiceTypePresent(boolean expectedResult, String blueprint, String serviceType) {
        assertEquals(expectedResult, new CmTemplateProcessor(getBlueprintText(blueprint)).isServiceTypePresent(serviceType));
    }

    private static void assertSortedEquals(Set<?> expected, Set<?> actual) {
        assertEquals(new TreeSet<>(expected), new TreeSet<>(actual));
    }

    private static void assertSortedEquals(Map<?, ?> expected, Map<?, ?> actual) {
        assertEquals(new TreeMap<>(expected), new TreeMap<>(actual));
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

    private String templateToString(ApiClusterTemplate template) {
        return JsonUtil.writeValueAsStringSilent(template);
    }

}
