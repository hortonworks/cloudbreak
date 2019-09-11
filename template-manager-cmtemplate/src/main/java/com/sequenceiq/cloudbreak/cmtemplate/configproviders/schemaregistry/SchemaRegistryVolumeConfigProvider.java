package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
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
public class SchemaRegistryVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        Integer volumeCount = Objects.nonNull(hostGroupView) && hostGroupView.getVolumeCount() > 0 ? 1 : 0;
        switch (roleType) {
            case SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER:
                return List.of(
                        config("schema.registry.jar.storage.directory.path", buildVolumePathStringZeroVolumeHandled(volumeCount, "schema_registry"))
                );
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return SchemaRegistryRoles.SCHEMAREGISTRY;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER);
    }
}
