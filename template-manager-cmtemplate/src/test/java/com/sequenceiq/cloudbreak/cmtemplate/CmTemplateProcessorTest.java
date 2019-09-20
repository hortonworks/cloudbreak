package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.configVar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateInstantiator;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroupInfo;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.GatewayRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class CmTemplateProcessorTest {

    private CmTemplateProcessor underTest;

    @Test
    public void testAddServiceConfigs() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        configs.add(new ApiClusterTemplateConfig().name("hive_metastore_database_type").variable("hive-hive_metastore_database_type"));

        underTest.addServiceConfigs("HIVE", List.of("HIVEMETASTORE"), configs);

        ApiClusterTemplateService service = underTest.getTemplate().getServices().stream().filter(srv -> "HIVE".equals(srv.getServiceType())).findAny().get();
        List<ApiClusterTemplateConfig> serviceConfigs = service.getServiceConfigs();
        assertEquals(1, serviceConfigs.size());
        assertEquals("hive_metastore_database_type", serviceConfigs.get(0).getName());
        assertEquals("hive-hive_metastore_database_type", serviceConfigs.get(0).getVariable());
    }

    @Test
    public void addExistingServiceConfigs() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-existing-conf.bp"));
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        configs.add(new ApiClusterTemplateConfig().name("redaction_policy_enabled").value("true"));
        configs.add(new ApiClusterTemplateConfig().name("not_present_in_template").value("some_value"));

        underTest.addServiceConfigs("HDFS", List.of("NAMENODE"), configs);

        ApiClusterTemplateService service = underTest.getTemplate().getServices().stream().filter(srv -> "HDFS".equals(srv.getServiceType())).findAny().get();
        List<ApiClusterTemplateConfig> serviceConfigs = service.getServiceConfigs();
        assertEquals(2, serviceConfigs.size());
        assertEquals("redaction_policy_enabled", serviceConfigs.get(0).getName());
        assertEquals("false", serviceConfigs.get(0).getValue());
        assertEquals(configs.get(1), serviceConfigs.get(1));
    }

    @Test
    public void testAddRoleConfigs() {
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
    public void testAddRoleConfigsWithNoMatchinRefName() {
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
    public void addExistingRoleConfigs() {
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
    public void testIsRoleTypePresentInServiceWithSingleRole() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("NAMENODE"));

        assertTrue(present);
    }

    @Test
    public void testIsRoleTypePresentInServiceWithMultipleRole() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("DATANODE", "NAMENODE"));

        assertTrue(present);
    }

    @Test
    public void testIsRoleTypePresentInServiceWithFakeRole() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("MYROLE"));

        assertFalse(present);
    }

    @Test
    public void testAddInstantiatorWithBaseRoles() {
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
    public void testAddInstantiatorWithoutBaseRoles() {
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
    public void addInstantiatorKeepsCustomClusterName() {
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
    public void mapRoleRefsToServiceComponents() {
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
    public void testGetAllComponents() {
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
    public void testGetServiceComponentsByHostGroup() {
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
    public void testGetComponentsInHostGroup() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        Set<String> expected = Set.of("REGIONSERVER", "DATANODE", "GATEWAY", "IMPALAD", "NODEMANAGER");

        Set<String> actual = underTest.getComponentsInHostGroup("worker");

        assertSortedEquals(expected, actual);
    }

    @Test
    public void testGetHostGroupsWithComponent() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        assertEquals(Set.of(), underTest.getHostGroupsWithComponent("FAKE_COMPONENT"));
        assertEquals(Set.of("master"), underTest.getHostGroupsWithComponent("HIVEMETASTORE"));
        assertEquals(Set.of("worker"), underTest.getHostGroupsWithComponent("DATANODE"));
        assertEquals(Set.of("master", "worker"), underTest.getHostGroupsWithComponent("GATEWAY"));
    }

    @Test
    public void testExtendTemplateWithAdditionalServicesWithNoAdditionalServices() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        String originalTemplate = templateToString(underTest.getTemplate());

        underTest.extendTemplateWithAdditionalServices(Map.of());

        String actualTemplate = templateToString(underTest.getTemplate());
        assertEquals(originalTemplate, actualTemplate);
    }

    @Test
    public void testExtendTemplateWithAdditionalServicesWithKnoxService() {
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
    public void testExtendTemplateWithAdditionalServicesWithKnoxServiceAndMultipleGateway() {
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
    public void danglingVariablesAreRemoved() {
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
    public void getCardinalityByHostGroup() {
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
    public void recommendGateway() {
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
    public void getHostTemplates() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/cdp-invalid-multi-host-template-name.bp"));
        assertEquals(3, underTest.getHostTemplateNames().size());
        assertEquals(2, underTest.getHostTemplateNames().stream().filter("master"::equals).count());
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
