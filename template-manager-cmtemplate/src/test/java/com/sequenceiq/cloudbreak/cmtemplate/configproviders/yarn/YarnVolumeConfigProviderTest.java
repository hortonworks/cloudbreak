package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeCount;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeTemplatesAndTemporaryStorage;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.preparatorWithHostGroups;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
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

    @Test
    void getRoleConfigsWithMultipleEphemeralVolumes() {
        HostgroupView worker = hostGroupWithVolumeTemplatesAndTemporaryStorage(2,
                Sets.newHashSet(new VolumeTemplate()), TemporaryStorage.EPHEMERAL_VOLUMES, 3, 300);

        List<ApiClusterTemplateConfig> roleConfigs = subject.getRoleConfigs(YarnRoles.NODEMANAGER, worker, preparatorWithHostGroups(worker));

        assertEquals(
                List.of(
                        config("yarn_nodemanager_local_dirs", "/hadoopfs/ephfs1/nodemanager,/hadoopfs/ephfs2/nodemanager,/hadoopfs/ephfs3/nodemanager"),
                        config("yarn_nodemanager_log_dirs", "/hadoopfs/ephfs1/nodemanager/log,/hadoopfs/ephfs2/nodemanager/log," +
                                "/hadoopfs/ephfs3/nodemanager/log")
                ),
                roleConfigs
        );
    }

    @Test
    void getRoleConfigsWithMultipleAttachedVolumes() {
        HostgroupView worker = hostGroupWithVolumeTemplatesAndTemporaryStorage(2,
                Sets.newHashSet(new VolumeTemplate()), TemporaryStorage.ATTACHED_VOLUMES, 0, 0);

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
    void testGetConfigAfterAddingVolumesWithMultipleVolumes() {
        HostgroupView worker = hostGroupWithVolumeCount(2);
        ServiceComponent serviceComponent = ServiceComponent.of(YarnRoles.YARN, YarnRoles.NODEMANAGER);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(worker, preparatorWithHostGroups(worker), serviceComponent);

        assertEquals(2, config.size());
        assertEquals("/hadoopfs/fs1/nodemanager,/hadoopfs/fs2/nodemanager", config.get("yarn_nodemanager_local_dirs"));
        assertEquals("/hadoopfs/fs1/nodemanager/log,/hadoopfs/fs2/nodemanager/log", config.get("yarn_nodemanager_log_dirs"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithEphemeralVolumes() {
        HostgroupView worker = hostGroupWithVolumeTemplatesAndTemporaryStorage(2,
                Sets.newHashSet(new VolumeTemplate()), TemporaryStorage.EPHEMERAL_VOLUMES, 3, 300);
        ServiceComponent serviceComponent = ServiceComponent.of(YarnRoles.YARN, YarnRoles.NODEMANAGER);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(worker, preparatorWithHostGroups(worker), serviceComponent);

        assertEquals(2, config.size());
        assertEquals("/hadoopfs/ephfs1/nodemanager,/hadoopfs/ephfs2/nodemanager,/hadoopfs/ephfs3/nodemanager",
                config.get("yarn_nodemanager_local_dirs"));
        assertEquals("/hadoopfs/ephfs1/nodemanager/log,/hadoopfs/ephfs2/nodemanager/log,/hadoopfs/ephfs3/nodemanager/log",
                config.get("yarn_nodemanager_log_dirs"));
    }

    @Test
    void testGetConfigAfterAddingVolumesWithZeroVolumes() {
        HostgroupView worker = hostGroupWithVolumeCount(0);
        ServiceComponent serviceComponent = ServiceComponent.of(YarnRoles.YARN, YarnRoles.NODEMANAGER);
        Map<String, String> config = subject.getConfigAfterAddingVolumes(worker, preparatorWithHostGroups(worker), serviceComponent);

        assertEquals(2, config.size());
        assertEquals("/hadoopfs/root1/nodemanager", config.get("yarn_nodemanager_local_dirs"));
        assertEquals("/hadoopfs/root1/nodemanager/log", config.get("yarn_nodemanager_log_dirs"));
    }
}
