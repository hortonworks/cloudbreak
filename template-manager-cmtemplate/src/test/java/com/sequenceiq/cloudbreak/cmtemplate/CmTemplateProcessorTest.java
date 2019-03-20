package com.sequenceiq.cloudbreak.cmtemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class CmTemplateProcessorTest {

    private CmTemplateProcessor underTest;

    @Test
    public void testAddServiceConfigs() {
        getClusterDefinitionText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getClusterDefinitionText("input/clouderamanager.bp"));
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
        getClusterDefinitionText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getClusterDefinitionText("input/clouderamanager.bp"));
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
        getClusterDefinitionText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getClusterDefinitionText("input/clouderamanager.bp"));
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
        getClusterDefinitionText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getClusterDefinitionText("input/clouderamanager-existing-conf.bp"));
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
        getClusterDefinitionText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getClusterDefinitionText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("NAMENODE"));

        assertTrue(present);
    }

    @Test
    public void testIsRoleTypePresentInServiceWithMultipleRole() {
        getClusterDefinitionText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getClusterDefinitionText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("DATANODE", "NAMENODE"));

        assertTrue(present);
    }

    @Test
    public void testIsRoleTypePresentInServiceWithFakeRole() {
        getClusterDefinitionText("input/clouderamanager.bp");
        underTest = new CmTemplateProcessor(getClusterDefinitionText("input/clouderamanager.bp"));

        boolean present = underTest.isRoleTypePresentInService("HDFS", List.of("MYROLE"));

        assertFalse(present);
    }

    private String getClusterDefinitionText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
