package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigProviderUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaConfigProviderUtils.CdhVersionForStreaming;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class SchemaRegistryJarStorageConfigProvider implements CmHostGroupRoleConfigProvider {

    public static final String CONFIG_JAR_STORAGE_DIRECTORY_PATH = "schema.registry.jar.storage.directory.path";

    public static final String CONFIG_JAR_STORAGE_TYPE = "schema.registry.jar.storage.type";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        // Currently, HDFS jar storage is used if there are multiple SCHEMA_REGISTRY_SERVER nodes. This should make sure
        // the Schema Registry version supports cloud storage. Ideally though this decision should be based on CDH version.
        CdhVersionForStreaming cdhVersionForStreaming = KafkaConfigProviderUtils.getCdhVersionForStreaming(source);
        if (cdhVersionForStreaming.supportsCloudJarStorage()) {
            // Use cloud storage
            return List.of(
                    config(CONFIG_JAR_STORAGE_DIRECTORY_PATH, "/schema-registry"),
                    config(CONFIG_JAR_STORAGE_TYPE, "hdfs"));
        } else {
            // Use local storage
            Integer volumeCount = Objects.nonNull(hostGroupView) && hostGroupView.getVolumeCount() > 0 ? 1 : 0;
            String localPath = buildVolumePathStringZeroVolumeHandled(volumeCount, "schema_registry");
            return List.of(
                    config(CONFIG_JAR_STORAGE_DIRECTORY_PATH, localPath));
        }
    }

    @VisibleForTesting
    static int getSchemaRegistryInstanceCount(TemplatePreparationObject source) {
        return source.getHostGroupsWithComponent(SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER).
                collect(Collectors.summingInt(HostgroupView::getNodeCount));
    }

    @Override
    public String getServiceType() {
        return SchemaRegistryRoles.SCHEMAREGISTRY;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER);
    }

    @Override
    public boolean sharedRoleType(String roleType) {
        return false;
    }
}
