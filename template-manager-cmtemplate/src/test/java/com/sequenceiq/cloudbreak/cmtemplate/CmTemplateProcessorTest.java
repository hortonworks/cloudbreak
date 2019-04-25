package com.sequenceiq.cloudbreak.cmtemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateInstantiator;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroupInfo;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class CmTemplateProcessorTest {

    private CmTemplateProcessor underTest;

    @Test
    public void testAddServiceConfigs() {
        getBlueprintText("input/clouderamanager.bp");
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
    public void testAddRoleConfigs() {
        getBlueprintText("input/clouderamanager.bp");
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
        getBlueprintText("input/clouderamanager.bp");
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
    public void testAddRoleConfigsWithExistingConfig() {
        getBlueprintText("input/clouderamanager.bp");
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
        assertEquals("worker_DATANODE", dfs1Config.getVariable());
        assertNull(dfs1Config.getValue());
        ApiClusterTemplateConfig dfs2Config = dnConfigs.stream().filter(c -> "dfs_data_dir_list_2".equals(c.getName())).findFirst().get();
        assertEquals("worker_DATANODE_2", dfs2Config.getVariable());
        assertNull(dfs2Config.getValue());
    }

    @Test
    public void testIsRoleTypePresentInServiceWithSingleRole() {
        getBlueprintText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("NAMENODE"));

        assertTrue(present);
    }

    @Test
    public void testIsRoleTypePresentInServiceWithMultipleRole() {
        getBlueprintText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("DATANODE", "NAMENODE"));

        assertTrue(present);
    }

    @Test
    public void testIsRoleTypePresentInServiceWithFakeRole() {
        getBlueprintText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("MYROLE"));

        assertFalse(present);
    }

    @Test
    public void testAddInstantiatorWithBaseRoles() {
        getBlueprintText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager.bp"));
        ClouderaManagerRepo clouderaManagerRepoDetails = new ClouderaManagerRepo();
        clouderaManagerRepoDetails.setVersion(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_6_3_0.getVersion());
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setClusterName("cluster");
        TemplatePreparationObject.Builder tpoBuilder = new TemplatePreparationObject.Builder().withGeneralClusterConfigs(generalClusterConfigs);
        TemplatePreparationObject templatePreparationObject = tpoBuilder.build();

        underTest.addInstantiator(clouderaManagerRepoDetails, templatePreparationObject);

        ApiClusterTemplateInstantiator instantiator = underTest.getTemplate().getInstantiator();
        List<ApiClusterTemplateRoleConfigGroupInfo> roleConfigGroups = instantiator.getRoleConfigGroups();
        List<String> refNames = roleConfigGroups.stream().map(ApiClusterTemplateRoleConfigGroupInfo::getRcgRefName).collect(Collectors.toList());

        assertEquals(2, refNames.size());
        assertTrue(refNames.containsAll(List.of("yarn-NODEMANAGER-BASE", "hdfs-DATANODE-BASE")));
    }

    @Test
    public void testAddInstantiatorWithoutBaseRoles() {
        getBlueprintText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-custom-ref.bp"));
        ClouderaManagerRepo clouderaManagerRepoDetails = new ClouderaManagerRepo();
        clouderaManagerRepoDetails.setVersion(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_6_3_0.getVersion());
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setClusterName("cluster");
        TemplatePreparationObject.Builder tpoBuilder = new TemplatePreparationObject.Builder().withGeneralClusterConfigs(generalClusterConfigs);
        TemplatePreparationObject templatePreparationObject = tpoBuilder.build();

        underTest.addInstantiator(clouderaManagerRepoDetails, templatePreparationObject);

        ApiClusterTemplateInstantiator instantiator = underTest.getTemplate().getInstantiator();
        List<ApiClusterTemplateRoleConfigGroupInfo> roleConfigGroups = instantiator.getRoleConfigGroups();

        assertNull(roleConfigGroups);
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
