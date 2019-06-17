package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeCount;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.preparatorWithHostGroups;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

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

}
