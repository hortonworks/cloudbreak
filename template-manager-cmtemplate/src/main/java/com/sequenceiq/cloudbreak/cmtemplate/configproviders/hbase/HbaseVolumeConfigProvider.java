package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildEphemeralVolumePathString;

import java.util.List;
import java.util.Set;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

public class HbaseVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    private static final String BUCKETCACHE_IOENGINE = "hbase_bucketcache_ioengine";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        if (HbaseRoles.REGIONSERVER.equals(roleType)) {
            if (hostGroupView != null && hostGroupView.getTemporaryStorage() == TemporaryStorage.EPHEMERAL_VOLUMES) {
                Integer temporaryStorageVolumeCount = hostGroupView.getTemporaryStorageVolumeCount();
                if (temporaryStorageVolumeCount != 0) {
                    return List.of(
                            config(BUCKETCACHE_IOENGINE, "files:" + buildEphemeralVolumePathString(temporaryStorageVolumeCount, "hbase_cache")));
                }
            }
        }
        return List.of();
    }

    @Override
    public String getServiceType() {
        return HbaseRoles.HBASE;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(HbaseRoles.REGIONSERVER);
    }

    @Override
    public boolean sharedRoleType(String roleType) {
        return false;
    }
}
