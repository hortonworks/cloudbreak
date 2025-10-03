package com.sequenceiq.cloudbreak.cmtemplate.configproviders.impala;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildEphemeralVolumePathString;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.VolumeUtils;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class ImpalaVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    private static final String IMPALA_DATACACHE_ENABLED_PARAM = "datacache_enabled";

    private static final String IMPALA_DATACACHE_DIRS_PARAM = "datacache_dirs";

    private static final String IMPALA_DATACACHE_CAPACITY_PARAM = "datacache_capacity";

    private static final String IMPALA_SCRATCH_DIRS_PARAM = "scratch_dirs";

    private static final int MAX_IMPALA_DATA_CACHE_SIZE_IN_GB = 300;

    private static final int MIN_ATTACHED_VOLUME_SIZE_TO_ENABLE_CACHE_IN_GB = 0;

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        switch (roleType) {
            case ImpalaRoles.ROLE_IMPALAD:
                Integer minAttachedVolumeSize = hostGroupView.getVolumeTemplates().stream()
                        .mapToInt(volume -> volume.getVolumeSize())
                        .min()
                        .orElse(0);
                int volumeCount = 0;
                int temporaryStorageVolumeCount = 0;
                int temporaryStorageVolumeSize = 0;
                if (hostGroupView != null) {
                    volumeCount = hostGroupView.getVolumeCount();
                    if (hostGroupView.getTemporaryStorageVolumeCount() != null) {
                        temporaryStorageVolumeCount = hostGroupView.getTemporaryStorageVolumeCount();
                    }
                    if (hostGroupView.getTemporaryStorageVolumeSize() != null) {
                        temporaryStorageVolumeSize = hostGroupView.getTemporaryStorageVolumeSize();
                    }
                }

                List<ApiClusterTemplateConfig> configs = new ArrayList<>();

                if (checkTemporaryStorage(hostGroupView, temporaryStorageVolumeCount)) {
                    configs.add(config(IMPALA_SCRATCH_DIRS_PARAM,
                            buildEphemeralVolumePathString(temporaryStorageVolumeCount, "impala/scratch")));
                } else {
                    configs.add(config(IMPALA_SCRATCH_DIRS_PARAM,
                            buildVolumePathStringZeroVolumeHandled(volumeCount, "impala/scratch")));
                }

                if (minAttachedVolumeSize > MIN_ATTACHED_VOLUME_SIZE_TO_ENABLE_CACHE_IN_GB
                        && volumeCount >= 0) {
                    configs.add(config(IMPALA_DATACACHE_ENABLED_PARAM, "true"));

                    if (checkTemporaryStorage(hostGroupView, temporaryStorageVolumeCount)) {
                        long temporaryStorageDataCacheCapacityInBytes = VolumeUtils
                                .convertGBToBytes(temporaryStorageVolumeSize / 2);

                        configs.add(config(IMPALA_DATACACHE_CAPACITY_PARAM, Long.toString(temporaryStorageDataCacheCapacityInBytes)));

                        configs.add(config(IMPALA_DATACACHE_DIRS_PARAM,
                                buildEphemeralVolumePathString(temporaryStorageVolumeCount, "impala/datacache")));
                    } else {
                        long dataCacheCapacityInBytes = VolumeUtils
                                .convertGBToBytes(Math.min(minAttachedVolumeSize / 2, MAX_IMPALA_DATA_CACHE_SIZE_IN_GB / volumeCount));

                        configs.add(config(IMPALA_DATACACHE_CAPACITY_PARAM, Long.toString(dataCacheCapacityInBytes)));

                        configs.add(config(IMPALA_DATACACHE_DIRS_PARAM,
                                buildVolumePathStringZeroVolumeHandled(volumeCount, "impala/datacache")));
                    }
                }
                return configs;

            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return ImpalaRoles.SERVICE_IMPALA;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(ImpalaRoles.ROLE_IMPALAD);
    }

    @Override
    public boolean sharedRoleType(String roleType) {
        return false;
    }

    @Override
    public Map<String, String> getConfigAfterAddingVolumes(HostgroupView hostgroupView, TemplatePreparationObject source, ServiceComponent serviceComponent) {
        Map<String, String> config = new HashMap<>();

        List<ApiClusterTemplateConfig> roleConfigs = getRoleConfigs(serviceComponent.getComponent(), hostgroupView, source);
        for (ApiClusterTemplateConfig roleConfig : roleConfigs) {
            config.put(roleConfig.getName(), roleConfig.getValue());
        }

        return config;
    }

    private boolean checkTemporaryStorage(HostgroupView hostGroupView, Integer temporaryStorageVolumeCount) {
        return hostGroupView != null && hostGroupView.getTemporaryStorage() == TemporaryStorage.EPHEMERAL_VOLUMES && temporaryStorageVolumeCount != 0;
    }
}

