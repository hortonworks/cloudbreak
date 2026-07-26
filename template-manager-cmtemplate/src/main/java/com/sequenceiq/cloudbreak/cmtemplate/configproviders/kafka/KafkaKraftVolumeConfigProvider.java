package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class KafkaKraftVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    private static final String KRAFT_VOLUME_DIRECTORY = "kraft";

    private static final String LOG_DIRS = "log.dirs";

    private static final String METADATA_LOG_DIR = "metadata.log.dir";

    private static final String KRAFT_PROPERTIES_ROLE_SAFETY_VALVE = "kraft.properties_role_safety_valve";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        int volumeCount = Objects.nonNull(hostGroupView) ? hostGroupView.getVolumeCount() : 0;
        if (KafkaRoles.KAFKA_KRAFT.equals(roleType)) {
            String metadataLogDir = buildSingleVolumePath(volumeCount, KRAFT_VOLUME_DIRECTORY);
            String logDirs = buildVolumePathStringZeroVolumeHandled(volumeCount, KRAFT_VOLUME_DIRECTORY);
            return List.of(
                    config(METADATA_LOG_DIR, metadataLogDir),
                    config(KRAFT_PROPERTIES_ROLE_SAFETY_VALVE, LOG_DIRS + "=" + logDirs)
            );
        }
        return List.of();
    }

    @Override
    public String getServiceType() {
        return KafkaRoles.KAFKA_SERVICE;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(KafkaRoles.KAFKA_KRAFT);
    }

    @Override
    public boolean sharedRoleType(String roleType) {
        return false;
    }
}
