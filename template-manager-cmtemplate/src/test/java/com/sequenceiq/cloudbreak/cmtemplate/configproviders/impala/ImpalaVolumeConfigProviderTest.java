package com.sequenceiq.cloudbreak.cmtemplate.configproviders.impala;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeTemplates;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.preparatorWithHostGroups;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

class ImpalaVolumeConfigProviderTest {

    private final ImpalaVolumeConfigProvider subject = new ImpalaVolumeConfigProvider();

    @Test
    void testRoleConfigsWithAttachedVolumes100GB() {
        HostgroupView worker = hostGroupWithVolumeTemplates(2, getVolumeTemplates(100));
        List<ApiClusterTemplateConfig> roleConfigs = subject.getRoleConfigs(ImpalaRoles.ROLE_IMPALAD, worker, preparatorWithHostGroups(worker));

        assertEquals(
                List.of(
                        config("scratch_dirs", "/hadoopfs/fs1/impala/scratch,/hadoopfs/fs2/impala/scratch"),
                        config("datacache_enabled", "true"),
                        config("datacache_capacity", "53687091200"),
                        config("datacache_dirs", "/hadoopfs/fs1/impala/datacache,/hadoopfs/fs2/impala/datacache")),
                roleConfigs
        );
    }

    @Test
    void testRoleConfigsWithAttachedVolumes200GB() {
        HostgroupView worker = hostGroupWithVolumeTemplates(3, getVolumeTemplates(200));
        List<ApiClusterTemplateConfig> roleConfigs = subject.getRoleConfigs(ImpalaRoles.ROLE_IMPALAD, worker, preparatorWithHostGroups(worker));

        assertEquals(
                List.of(
                        config("scratch_dirs", "/hadoopfs/fs1/impala/scratch,/hadoopfs/fs2/impala/scratch,/hadoopfs/fs3/impala/scratch"),
                        config("datacache_enabled", "true"),
                        config("datacache_capacity", "46170898432"),
                        config("datacache_dirs", "/hadoopfs/fs1/impala/datacache,/hadoopfs/fs2/impala/datacache,/hadoopfs/fs3/impala/datacache")),
                roleConfigs
        );
    }

    @Test
    void testRoleConfigsWithAttachedVolumesGreaterThanMaxImapalaCacheVolumeSize() {
        HostgroupView worker = hostGroupWithVolumeTemplates(3, getVolumeTemplates(1000));
        List<ApiClusterTemplateConfig> roleConfigs = subject.getRoleConfigs(ImpalaRoles.ROLE_IMPALAD, worker, preparatorWithHostGroups(worker));

        assertEquals(
                List.of(
                        config("scratch_dirs", "/hadoopfs/fs1/impala/scratch,/hadoopfs/fs2/impala/scratch,/hadoopfs/fs3/impala/scratch"),
                        config("datacache_enabled", "true"),
                        config("datacache_capacity", "46170898432"),
                        config("datacache_dirs", "/hadoopfs/fs1/impala/datacache,/hadoopfs/fs2/impala/datacache,/hadoopfs/fs3/impala/datacache")),
                roleConfigs
        );
    }

    @Test
    void testRoleConfigsWithAttachedVolumeCountZero() {

        HostgroupView worker = hostGroupWithVolumeTemplates(0, getVolumeTemplates(0));
        List<ApiClusterTemplateConfig> roleConfigs = subject.getRoleConfigs(ImpalaRoles.ROLE_IMPALAD, worker, preparatorWithHostGroups(worker));

        assertEquals(
                List.of(
                        config("scratch_dirs", "/hadoopfs/root1/impala/scratch")),
                roleConfigs
        );
    }

    protected Set<VolumeTemplate> getVolumeTemplates(int attachedVolumeSize) {
        Template rootStorageTemplate = new Template();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeSize(attachedVolumeSize);
        volumeTemplate.setTemplate(rootStorageTemplate);
        return Sets.newHashSet(volumeTemplate);
    }
}
