package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeCount;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class SchemaRegistryVolumeConfigProviderTest {

    private final SchemaRegistryVolumeConfigProvider subject = new SchemaRegistryVolumeConfigProvider();

    @Test
    void testRoleConfigsWithMultipleVolumes() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(3);

        assertEquals(List.of(
                config("schema.registry.jar.storage.directory.path", "/hadoopfs/fs1/schema_registry")),
                subject.getRoleConfigs(SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithSingleVolume() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);

        assertEquals(List.of(
                config("schema.registry.jar.storage.directory.path", "/hadoopfs/fs1/schema_registry")),
                subject.getRoleConfigs(SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithoutVolumes() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(0);

        assertEquals(List.of(
                config("schema.registry.jar.storage.directory.path", "/hadoopfs/root1/schema_registry")),
                subject.getRoleConfigs(SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    private TemplatePreparationObject getTemplatePreparationObject(HostgroupView hostGroup) {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/kafka.bp");
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(hostGroup))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", new CmTemplateProcessor(inputJson)))
                .build();
        return preparationObject;
    }

}