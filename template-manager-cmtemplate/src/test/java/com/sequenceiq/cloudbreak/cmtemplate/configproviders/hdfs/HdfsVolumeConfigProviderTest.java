package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeCount;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.preparatorWithHostGroups;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

class HdfsVolumeConfigProviderTest {

    private final HdfsVolumeConfigProvider subject = new HdfsVolumeConfigProvider();

    @Test
    void getRoleConfigsWithMultipleVolumes() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(2);

        assertEquals(List.of(config("dfs_data_dir_list", "/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode")),
                subject.getRoleConfigs(HdfsRoles.DATANODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
        assertEquals(List.of(config("dfs_name_dir_list", "/hadoopfs/fs1/namenode,/hadoopfs/fs2/namenode")),
                subject.getRoleConfigs(HdfsRoles.NAMENODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
        assertEquals(List.of(config("fs_checkpoint_dir_list", "/hadoopfs/fs1/namesecondary,/hadoopfs/fs2/namesecondary")),
                subject.getRoleConfigs(HdfsRoles.SECONDARYNAMENODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
        assertEquals(List.of(config("dfs_journalnode_edits_dir", "/hadoopfs/fs1/journalnode")),
                subject.getRoleConfigs(HdfsRoles.JOURNALNODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
    }

    @Test
    void getRoleConfigsWithSingleVolume() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);

        assertEquals(List.of(config("dfs_data_dir_list", "/hadoopfs/fs1/datanode")),
                subject.getRoleConfigs(HdfsRoles.DATANODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
        assertEquals(List.of(config("dfs_name_dir_list", "/hadoopfs/fs1/namenode")),
                subject.getRoleConfigs(HdfsRoles.NAMENODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
        assertEquals(List.of(config("fs_checkpoint_dir_list", "/hadoopfs/fs1/namesecondary")),
                subject.getRoleConfigs(HdfsRoles.SECONDARYNAMENODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
        assertEquals(List.of(config("dfs_journalnode_edits_dir", "/hadoopfs/fs1/journalnode")),
                subject.getRoleConfigs(HdfsRoles.JOURNALNODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
    }

    @Test
    void getRoleConfigsWithoutVolumes() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(0);

        assertEquals(List.of(config("dfs_data_dir_list", "/hadoopfs/root1/datanode")),
                subject.getRoleConfigs(HdfsRoles.DATANODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
        assertEquals(List.of(config("dfs_name_dir_list", "/hadoopfs/root1/namenode")),
                subject.getRoleConfigs(HdfsRoles.NAMENODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
        assertEquals(List.of(config("fs_checkpoint_dir_list", "/hadoopfs/root1/namesecondary")),
                subject.getRoleConfigs(HdfsRoles.SECONDARYNAMENODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
        assertEquals(List.of(config("dfs_journalnode_edits_dir", "/hadoopfs/root1/journalnode")),
                subject.getRoleConfigs(HdfsRoles.JOURNALNODE, hostGroup, preparatorWithHostGroups(hostGroup))
        );
    }

    @Test
    void testGetConfigAfterAddingVolumesWithMultipleVolumesDataNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(2);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.DATANODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/fs1/datanode,/hadoopfs/fs2/datanode", config.get("dfs_data_dir_list"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithMultipleVolumesNameNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(2);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.NAMENODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/fs1/namenode,/hadoopfs/fs2/namenode", config.get("dfs_name_dir_list"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithMultipleVolumesSecondaryNameNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(2);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.SECONDARYNAMENODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/fs1/namesecondary,/hadoopfs/fs2/namesecondary", config.get("fs_checkpoint_dir_list"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithMultipleVolumesJournalNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(2);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.JOURNALNODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/fs1/journalnode", config.get("dfs_journalnode_edits_dir"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithSingleVolumeDataNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.DATANODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/fs1/datanode", config.get("dfs_data_dir_list"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithSingleVolumeNameNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.NAMENODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/fs1/namenode", config.get("dfs_name_dir_list"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithSingleVolumeSecondaryNameNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.SECONDARYNAMENODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/fs1/namesecondary", config.get("fs_checkpoint_dir_list"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithSingleVolumeJournalNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.JOURNALNODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/fs1/journalnode", config.get("dfs_journalnode_edits_dir"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithZeroVolumesDataNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(0);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.DATANODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/root1/datanode", config.get("dfs_data_dir_list"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithZeroVolumesNameNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(0);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.NAMENODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/root1/namenode", config.get("dfs_name_dir_list"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithZeroVolumesSecondaryNameNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(0);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.SECONDARYNAMENODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/root1/namesecondary", config.get("fs_checkpoint_dir_list"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithZeroVolumesJournalNode() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(0);
        ServiceComponent serviceComponent = ServiceComponent.of(HdfsRoles.HDFS, HdfsRoles.JOURNALNODE);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(hostGroup, preparatorWithHostGroups(hostGroup), serviceComponent);

        assertEquals(1, config.size());
        assertEquals("/hadoopfs/root1/journalnode", config.get("dfs_journalnode_edits_dir"));
    }

}
