package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.preparatorWithHostGroups;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeCount;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

class YarnVolumeConfigProviderTest {

    private final YarnVolumeConfigProvider subject = new YarnVolumeConfigProvider();

    @Test
    void getRoleConfigsWithMultipleVolumes() {
        HostgroupView worker = hostGroupWithVolumeCount(2);

        List<ApiClusterTemplateConfig> roleConfigs = subject.getRoleConfigs(YarnRoles.NODEMANAGER, worker, preparatorWithHostGroups(worker));

        assertEquals(
                List.of(
                        config("yarn_nodemanager_local_dirs", "/hadoopfs/fs1/nodemanager,/hadoopfs/fs2/nodemanager"),
                        config("yarn_nodemanager_log_dirs", "/hadoopfs/fs1/nodemanager/log,/hadoopfs/fs2/nodemanager/log")
                ),
                roleConfigs
        );
    }

    @Test
    void getRoleConfigsWithoutVolumes() {
        HostgroupView worker = hostGroupWithVolumeCount(0);

        List<ApiClusterTemplateConfig> roleConfigs = subject.getRoleConfigs(YarnRoles.NODEMANAGER, worker, preparatorWithHostGroups(worker));

        assertEquals(
                List.of(
                        config("yarn_nodemanager_local_dirs", "/hadoopfs/root1/nodemanager"),
                        config("yarn_nodemanager_log_dirs", "/hadoopfs/root1/nodemanager/log")
                ),
                roleConfigs
        );
    }

}
