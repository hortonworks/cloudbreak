package com.sequenceiq.cloudbreak.cmtemplate.configproviders.volume;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class HdfsVolumeConfigProviderTest {

    private final HdfsVolumeConfigProvider underTest = new HdfsVolumeConfigProvider();

    @Test
    public void testGetRoleTypeVariableName() {
        String variableName = underTest.getRoleTypeVariableName("master", "NAMENODE", "dfs_dir");

        assertEquals("master_namenode_dfs_dir", variableName);
    }

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("hdfs-NAMENODE-BASE");
        List<ApiClusterTemplateConfig> workerDN = roleConfigs.get("hdfs-DATANODE-BASE");

        assertEquals(1, workerDN.size());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.get(0).getVariable());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.get(0).getVariable());
    }

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroupWithCustomRefNames() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager-custom-ref.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("nn");
        List<ApiClusterTemplateConfig> workerDN = roleConfigs.get("dn");

        assertEquals(1, workerDN.size());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.get(0).getVariable());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.get(0).getVariable());
    }

    @Test
    public void testGetRoleConfigsWithOneHostGroupZeroAttachedVolumes() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("hdfs-NAMENODE-BASE");
        List<ApiClusterTemplateConfig> masterSN = roleConfigs.get("hdfs-SECONDARYNAMENODE-BASE");

        assertEquals(2, roleConfigs.size());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.get(0).getVariable());
        assertEquals("fs_checkpoint_dir_list", masterSN.get(0).getName());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.get(0).getVariable());
    }

    @Test
    public void testGetRoleConfigsWithAllHostGroupZeroAttachedVolumes() {
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(0, roleConfigs.size());
    }

    @Test
    public void testGetRoleConfigsWithNoNNOrDNInAnyHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager-no-nn-dn.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        assertTrue(roleConfigs.isEmpty());
    }

    @Test
    public void testGetRoleConfigsWithSameDataNodeRoleForMultipleGroups() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        HostgroupView compute = new HostgroupView("compute", 3, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker, compute)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager-3hg-same-DN-role.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("hdfs-NAMENODE-BASE");

        assertEquals(3, roleConfigs.size());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.get(0).getVariable());
    }

    @Test
    public void testGetRoleConfigsWithDifferentDataNodeRoleForMultipleGroups() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        HostgroupView compute = new HostgroupView("compute", 3, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker, compute)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager-3hg-different-DN-role.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("hdfs-NAMENODE-BASE");
        List<ApiClusterTemplateConfig> workerDN = roleConfigs.get("hdfs-DATANODE-BASE");
        List<ApiClusterTemplateConfig> computeDN = roleConfigs.get("hdfs-DATANODE-compute");

        assertEquals(1, workerDN.size());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.get(0).getVariable());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.get(0).getVariable());
        assertEquals(1, computeDN.size());
        assertEquals("dfs_data_dir_list", computeDN.get(0).getName());
        assertEquals("compute_datanode_dfs_data_dir_list", computeDN.get(0).getVariable());
    }

    @Test
    public void testGetRoleConfigVariables() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        roleVariables.sort(Comparator.comparing(ApiClusterTemplateVariable::getName));
        ApiClusterTemplateVariable masterNN = roleVariables.get(0);
        ApiClusterTemplateVariable masterSN = roleVariables.get(1);
        ApiClusterTemplateVariable workerDN = roleVariables.get(2);

        assertEquals(3, roleVariables.size());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.getValue());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.getName());
        assertEquals("/hadoopfs/fs1/namesecondary", masterSN.getValue());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.getValue());
    }

    @Test
    public void testGetRoleConfigVariablesWithOneHostGroupZeroAttachedVolumes() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        ApiClusterTemplateVariable masterNN = roleVariables.get(0);
        ApiClusterTemplateVariable masterSN = roleVariables.get(1);

        assertEquals(2, roleVariables.size());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.getValue());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.getName());
        assertEquals("/hadoopfs/fs1/namesecondary", masterSN.getValue());
    }

    @Test
    public void testGetRoleConfigVariablesWithAllHostGroupZeroAttachedVolumes() {
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        assertEquals(0, roleVariables.size());
    }

    @Test
    public void testGetRoleConfigVariablesWithCustomRefNames() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager-custom-ref.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        roleVariables.sort(Comparator.comparing(ApiClusterTemplateVariable::getName));
        ApiClusterTemplateVariable masterNN = roleVariables.get(0);
        ApiClusterTemplateVariable masterSN = roleVariables.get(1);
        ApiClusterTemplateVariable workerDN = roleVariables.get(2);

        assertEquals(3, roleVariables.size());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.getValue());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.getName());
        assertEquals("/hadoopfs/fs1/namesecondary", masterSN.getValue());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.getValue());
    }

    @Test
    public void testGetRoleConfigVariablesWithNoNNOrDNInAnyHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager-no-nn-dn.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        assertTrue(roleVariables.isEmpty());
    }

    @Test
    public void testGetRoleConfigVariablesWithDifferentDataNodeRoleForMultipleGroups() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        HostgroupView compute = new HostgroupView("compute", 3, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker, compute)).build();
        String inputJson = getClusterDefinitionText("input/clouderamanager-3hg-different-DN-role.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        roleVariables.sort(Comparator.comparing(ApiClusterTemplateVariable::getName));
        ApiClusterTemplateVariable computeDN = roleVariables.get(0);
        ApiClusterTemplateVariable masterNN = roleVariables.get(1);
        ApiClusterTemplateVariable masterSN = roleVariables.get(2);
        ApiClusterTemplateVariable workerDN = roleVariables.get(3);

        assertEquals("master_namenode_dfs_name_dir_list", masterNN.getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.getValue());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.getName());
        assertEquals("/hadoopfs/fs1/namesecondary", masterSN.getValue());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.getValue());
        assertEquals("compute_datanode_dfs_data_dir_list", computeDN.getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode,/hadoopfs/fs3/datanode", computeDN.getValue());
    }

    private String getClusterDefinitionText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

}
