package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class HdfsVolumeConfigProviderTest {

    private final HdfsRoleConfigConfigProvider underTest = new HdfsRoleConfigConfigProvider();

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
        List<ApiClusterTemplateConfig> masterSN = roleConfigs.get("hdfs-SECONDARYNAMENODE-BASE");

        assertEquals(2, workerDN.size());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.get(0).getValue());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("0", workerDN.get(1).getValue());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.get(0).getValue());
        assertEquals("fs_checkpoint_dir_list", masterSN.get(0).getName());
        assertEquals("/hadoopfs/fs1/namesecondary", masterSN.get(0).getValue());
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
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.get(0).getValue());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("0", workerDN.get(1).getValue());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.get(0).getValue());
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
        assertEquals("/hadoopfs/fs1/namenode", masterNN.get(0).getValue());
        assertEquals("fs_checkpoint_dir_list", masterSN.get(0).getName());
        assertEquals("/hadoopfs/fs1/namesecondary", masterSN.get(0).getValue());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("/hadoopfs/root1/datanode", workerDN.get(0).getValue());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("0", workerDN.get(1).getValue());
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
        assertEquals("/hadoopfs/root1/namenode", masterNN.get(0).getValue());
        assertEquals("fs_checkpoint_dir_list", masterSN.get(0).getName());
        assertEquals("/hadoopfs/root1/namesecondary", masterSN.get(0).getValue());
        assertEquals("dfs_data_dir_list", workerDN.get(0).getName());
        assertEquals("/hadoopfs/root1/datanode", workerDN.get(0).getValue());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("0", workerDN.get(1).getValue());
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
        assertEquals("/hadoopfs/fs1/namenode", masterNN.get(0).getValue());
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
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.get(0).getValue());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("0", workerDN.get(1).getValue());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals("/hadoopfs/fs1/namenode", masterNN.get(0).getValue());
        assertEquals(2, computeDN.size());
        assertEquals("dfs_data_dir_list", computeDN.get(0).getName());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode,/hadoopfs/fs3/datanode", computeDN.get(0).getValue());
        assertEquals("dfs_datanode_failed_volumes_tolerated", computeDN.get(1).getName());
        assertEquals("0", computeDN.get(1).getValue());

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
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", workerDN.get(0).getValue());
        assertEquals("dfs_datanode_failed_volumes_tolerated", workerDN.get(1).getName());
        assertEquals("0", workerDN.get(1).getValue());
        assertEquals(1, masterNN.size());
        assertEquals("dfs_name_dir_list", masterNN.get(0).getName());
        assertEquals(2, computeDN.size());
        assertEquals("dfs_data_dir_list", computeDN.get(0).getName());
        assertEquals("/hadoopfs/root1/datanode", computeDN.get(0).getValue());
        assertEquals("dfs_datanode_failed_volumes_tolerated", computeDN.get(1).getName());
        assertEquals("0", computeDN.get(1).getValue());
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

}
