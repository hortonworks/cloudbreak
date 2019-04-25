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
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("hdfs-NAMENODE-BASE");
        List<ApiClusterTemplateConfig> workerDN = roleConfigs.get("hdfs-DATANODE-BASE");

        assertEquals(2, workerDN.size());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.get(0).getVariable());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("worker_datanode_dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getVariable());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.get(0).getVariable());
    }

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroupWithCustomRefNames() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager-custom-ref.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("nn");
        List<ApiClusterTemplateConfig> workerDN = roleConfigs.get("dn");

        assertEquals(2, workerDN.size());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.get(0).getVariable());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("worker_datanode_dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getVariable());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.get(0).getVariable());
    }

    @Test
    public void testGetRoleConfigsWithOneHostGroupZeroAttachedVolumes() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("hdfs-NAMENODE-BASE");
        List<ApiClusterTemplateConfig> masterSN = roleConfigs.get("hdfs-SECONDARYNAMENODE-BASE");
        List<ApiClusterTemplateConfig> workerDN = roleConfigs.get("hdfs-DATANODE-BASE");

        assertEquals(3, roleConfigs.size());
        assertEquals(1, masterNN.size());
        assertEquals(1, masterSN.size());
        assertEquals(2, workerDN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.get(0).getVariable());
        assertEquals("fs_checkpoint_dir_list", masterSN.get(0).getName());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.get(0).getVariable());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.get(0).getVariable());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("worker_datanode_dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getVariable());
    }

    @Test
    public void testGetRoleConfigsWithAllHostGroupZeroAttachedVolumes() {
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("hdfs-NAMENODE-BASE");
        List<ApiClusterTemplateConfig> masterSN = roleConfigs.get("hdfs-SECONDARYNAMENODE-BASE");
        List<ApiClusterTemplateConfig> workerDN = roleConfigs.get("hdfs-DATANODE-BASE");

        assertEquals(3, roleConfigs.size());
        assertEquals(1, masterNN.size());
        assertEquals(1, masterSN.size());
        assertEquals(2, workerDN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.get(0).getVariable());
        assertEquals("fs_checkpoint_dir_list", masterSN.get(0).getName());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.get(0).getVariable());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.get(0).getVariable());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("worker_datanode_dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getVariable());
    }

    @Test
    public void testGetRoleConfigsWithNoNNOrDNInAnyHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager-no-nn-dn.bp");
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
        String inputJson = getBlueprintText("input/clouderamanager-3hg-same-DN-role.bp");
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
        String inputJson = getBlueprintText("input/clouderamanager-3hg-different-DN-role.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("hdfs-NAMENODE-BASE");
        List<ApiClusterTemplateConfig> workerDN = roleConfigs.get("hdfs-DATANODE-BASE");
        List<ApiClusterTemplateConfig> computeDN = roleConfigs.get("hdfs-DATANODE-compute");

        assertEquals(2, workerDN.size());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.get(0).getVariable());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("worker_datanode_dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getVariable());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.get(0).getVariable());
        assertEquals(2, computeDN.size());
        assertEquals("dfs_data_dir_list", computeDN.get(0).getName());
        assertEquals("compute_datanode_dfs_data_dir_list", computeDN.get(0).getVariable());
        assertEquals("dfs_datanode_failed_volumes_tolerated", computeDN.get(1).getName());
        assertEquals("compute_datanode_dfs_datanode_failed_volumes_tolerated", computeDN.get(1).getVariable());

    }

    @Test
    public void testGetRoleConfigsWithDifferentDataNodeRoleForMultipleGroupsNoComputeDisks() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        HostgroupView compute = new HostgroupView("compute", 0, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker, compute)).build();
        String inputJson = getBlueprintText("input/clouderamanager-3hg-different-DN-role.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterNN = roleConfigs.get("hdfs-NAMENODE-BASE");
        List<ApiClusterTemplateConfig> workerDN = roleConfigs.get("hdfs-DATANODE-BASE");
        List<ApiClusterTemplateConfig> computeDN = roleConfigs.get("hdfs-DATANODE-compute");

        assertEquals(2, workerDN.size());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.get(0).getVariable());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("worker_datanode_dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getVariable());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.get(0).getVariable());
        assertEquals(2, computeDN.size());
        assertTrue(computeDN.stream().map(ApiClusterTemplateConfig::getName).anyMatch("dfs_data_dir_list"::equals));
        assertEquals("compute_datanode_dfs_data_dir_list", computeDN.stream().filter(conf -> "dfs_data_dir_list".equals(conf.getName()))
                .findFirst().get().getVariable());
        assertTrue(computeDN.stream().map(ApiClusterTemplateConfig::getName).anyMatch("dfs_datanode_failed_volumes_tolerated"::equals));
        assertEquals("compute_datanode_dfs_datanode_failed_volumes_tolerated", computeDN.stream()
                .filter(conf -> "dfs_datanode_failed_volumes_tolerated".equals(conf.getName()))
                .findFirst().get().getVariable());
    }

    @Test
    public void testGetRoleConfigVariables() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        roleVariables.sort(Comparator.comparing(ApiClusterTemplateVariable::getName));
        ApiClusterTemplateVariable masterNN = roleVariables.get(0);
        ApiClusterTemplateVariable masterSN = roleVariables.get(1);
        ApiClusterTemplateVariable workerDN = roleVariables.get(2);
        ApiClusterTemplateVariable workerDNFailedVolumes = roleVariables.get(3);

        assertEquals(4, roleVariables.size());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.getValue());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.getName());
        assertEquals("/hadoopfs/fs1/namesecondary", masterSN.getValue());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.getValue());
        assertEquals("worker_datanode_dfs_datanode_failed_volumes_tolerated", workerDNFailedVolumes.getName());
        assertEquals("0", workerDNFailedVolumes.getValue());
    }

    @Test
    public void testGetRoleConfigVariablesWithOneHostGroupZeroAttachedVolumes() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        roleVariables.sort(Comparator.comparing(ApiClusterTemplateVariable::getName));
        ApiClusterTemplateVariable masterNN = roleVariables.get(0);
        ApiClusterTemplateVariable masterSN = roleVariables.get(1);
        ApiClusterTemplateVariable workerDN = roleVariables.get(2);
        ApiClusterTemplateVariable workerFailedVolumes = roleVariables.get(3);

        assertEquals(4, roleVariables.size());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.getValue());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.getName());
        assertEquals("/hadoopfs/fs1/namesecondary", masterSN.getValue());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.getName());
        assertEquals("/hadoopfs/root1/datanode", workerDN.getValue());
        assertEquals("worker_datanode_dfs_datanode_failed_volumes_tolerated", workerFailedVolumes.getName());
        assertEquals("0", workerFailedVolumes.getValue());
    }

    @Test
    public void testGetRoleConfigVariablesWithAllHostGroupZeroAttachedVolumes() {
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        roleVariables.sort(Comparator.comparing(ApiClusterTemplateVariable::getName));
        ApiClusterTemplateVariable masterNN = roleVariables.get(0);
        ApiClusterTemplateVariable masterSN = roleVariables.get(1);
        ApiClusterTemplateVariable workerDN = roleVariables.get(2);
        ApiClusterTemplateVariable workerFailedVolumes = roleVariables.get(3);

        assertEquals(4, roleVariables.size());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.getName());
        assertEquals("/hadoopfs/root1/namenode", masterNN.getValue());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.getName());
        assertEquals("/hadoopfs/root1/namesecondary", masterSN.getValue());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.getName());
        assertEquals("/hadoopfs/root1/datanode", workerDN.getValue());
        assertEquals("worker_datanode_dfs_datanode_failed_volumes_tolerated", workerFailedVolumes.getName());
        assertEquals("0", workerFailedVolumes.getValue());
    }

    @Test
    public void testGetRoleConfigVariablesWithCustomRefNames() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager-custom-ref.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        roleVariables.sort(Comparator.comparing(ApiClusterTemplateVariable::getName));
        ApiClusterTemplateVariable masterNN = roleVariables.get(0);
        ApiClusterTemplateVariable masterSN = roleVariables.get(1);
        ApiClusterTemplateVariable workerDN = roleVariables.get(2);
        ApiClusterTemplateVariable workerDNFailedVolumes = roleVariables.get(3);

        assertEquals(4, roleVariables.size());
        assertEquals("master_namenode_dfs_name_dir_list", masterNN.getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.getValue());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.getName());
        assertEquals("/hadoopfs/fs1/namesecondary", masterSN.getValue());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.getValue());
        assertEquals("worker_datanode_dfs_datanode_failed_volumes_tolerated", workerDNFailedVolumes.getName());
        assertEquals("0", workerDNFailedVolumes.getValue());
    }

    @Test
    public void testGetRoleConfigVariablesWithNoNNOrDNInAnyHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager-no-nn-dn.bp");
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
        String inputJson = getBlueprintText("input/clouderamanager-3hg-different-DN-role.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        roleVariables.sort(Comparator.comparing(ApiClusterTemplateVariable::getName));
        ApiClusterTemplateVariable computeDN = roleVariables.get(0);
        ApiClusterTemplateVariable computeVolTolerate = roleVariables.get(1);
        ApiClusterTemplateVariable masterNN = roleVariables.get(2);
        ApiClusterTemplateVariable masterSN = roleVariables.get(3);
        ApiClusterTemplateVariable workerDN = roleVariables.get(4);

        assertEquals("master_namenode_dfs_name_dir_list", masterNN.getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.getValue());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.getName());
        assertEquals("/hadoopfs/fs1/namesecondary", masterSN.getValue());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.getValue());
        assertEquals("compute_datanode_dfs_data_dir_list", computeDN.getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode,/hadoopfs/fs3/datanode", computeDN.getValue());
        assertEquals("compute_datanode_dfs_datanode_failed_volumes_tolerated", computeVolTolerate.getName());
        assertEquals("0", computeVolTolerate.getValue());
    }

    @Test
    public void testGetRoleConfigVariablesWithDifferentDataNodeRoleForMultipleGroupsNoComputeDisks() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        HostgroupView compute = new HostgroupView("compute", 0, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker, compute)).build();
        String inputJson = getBlueprintText("input/clouderamanager-3hg-different-DN-role.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        roleVariables.sort(Comparator.comparing(ApiClusterTemplateVariable::getName));
        ApiClusterTemplateVariable computeDN = roleVariables.get(0);
        ApiClusterTemplateVariable computeFailedVolumes = roleVariables.get(1);
        ApiClusterTemplateVariable masterNN = roleVariables.get(2);
        ApiClusterTemplateVariable masterSN = roleVariables.get(3);
        ApiClusterTemplateVariable workerDN = roleVariables.get(4);

        assertEquals("master_namenode_dfs_name_dir_list", masterNN.getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.getValue());
        assertEquals("master_secondarynamenode_fs_checkpoint_dir_list", masterSN.getName());
        assertEquals("/hadoopfs/fs1/namesecondary", masterSN.getValue());
        assertEquals("worker_datanode_dfs_data_dir_list", workerDN.getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.getValue());
        assertEquals("compute_datanode_dfs_data_dir_list", computeDN.getName());
        assertEquals("/hadoopfs/root1/datanode", computeDN.getValue());
        assertEquals("compute_datanode_dfs_datanode_failed_volumes_tolerated", computeFailedVolumes.getName());
        assertEquals("0", computeFailedVolumes.getValue());
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

}
