package com.sequenceiq.cloudbreak.cmtemplate.configproviders.impala;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
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

    private static final int MAX_IMPALA_DATA_CACHE_SIZE_IN_GB = 130;

    private static final int MIN_ATTACHED_VOLUME_SIZE_TO_ENABLE_CACHE_IN_GB = 0;

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        switch (roleType) {
            case ImpalaRoles.ROLE_IMPALAD:
                Integer minAttachedVolumeSize = hostGroupView.getVolumeTemplates().stream()
                        .mapToInt(volume -> volume.getVolumeSize())
                        .min()
                        .orElse(0);

                List<ApiClusterTemplateConfig> configs = new ArrayList<>();
                configs.add(config(IMPALA_SCRATCH_DIRS_PARAM,
                        buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "impala/scratch")));

                if (minAttachedVolumeSize > MIN_ATTACHED_VOLUME_SIZE_TO_ENABLE_CACHE_IN_GB
                        && hostGroupView.getVolumeCount() > 0) {
                    long dataCacheCapacityInBytes = VolumeUtils
                            .convertGBToBytes(Math.min(minAttachedVolumeSize / 2, MAX_IMPALA_DATA_CACHE_SIZE_IN_GB / hostGroupView.getVolumeCount()));

                    configs.add(config(IMPALA_DATACACHE_ENABLED_PARAM, "true"));
                    configs.add(config(IMPALA_DATACACHE_CAPACITY_PARAM, Long.toString(dataCacheCapacityInBytes)));
                    configs.add(config(IMPALA_DATACACHE_DIRS_PARAM,
                            buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "impala/datacache")));
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
    public boolean shouldSplit(ServiceComponent serviceComponent) {
        return true;
    }
}
