package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeTemplatesAndTemporaryStorage;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.preparatorWithHostGroups;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

public class HbaseVolumeConfigProviderTest {

    private final HbaseVolumeConfigProvider underTest = new HbaseVolumeConfigProvider();

    @Test
    void getRoleConfigsWithMultipleEphemeralVolumes() {
        HostgroupView worker = hostGroupWithVolumeTemplatesAndTemporaryStorage(2,
                Sets.newHashSet(new VolumeTemplate()), TemporaryStorage.EPHEMERAL_VOLUMES, 3);

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(HbaseRoles.REGIONSERVER, worker, preparatorWithHostGroups(worker));

        assertEquals(
                List.of(
                        config("hbase_bucketcache_ioengine", "files:/hadoopfs/ephfs1/hbase_cache,/hadoopfs/ephfs2/hbase_cache,/hadoopfs/ephfs3/hbase_cache")
                ),
                roleConfigs
        );
    }

    @Test
    void getRoleConfigsWithMultipleAttachedVolumes() {
        HostgroupView worker = hostGroupWithVolumeTemplatesAndTemporaryStorage(2,
                Sets.newHashSet(new VolumeTemplate()), TemporaryStorage.ATTACHED_VOLUMES, 0);

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(HbaseRoles.REGIONSERVER, worker, preparatorWithHostGroups(worker));

        assertEquals(List.of(), roleConfigs);
    }
}
