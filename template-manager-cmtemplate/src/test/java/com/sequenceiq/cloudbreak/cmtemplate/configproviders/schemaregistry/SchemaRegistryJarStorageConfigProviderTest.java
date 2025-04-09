package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeCount;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryJarStorageConfigProvider.CONFIG_JAR_STORAGE_DIRECTORY_PATH;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryJarStorageConfigProvider.CONFIG_JAR_STORAGE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryJarStorageConfigProvider.getSchemaRegistryInstanceCount;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.SchemaRegistryRoles.SCHEMA_REGISTRY_SERVER;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class SchemaRegistryJarStorageConfigProviderTest {

    private final SchemaRegistryJarStorageConfigProvider subject = new SchemaRegistryJarStorageConfigProvider();

    @Mock
    private BlueprintView blueprintView;

    @Mock
    private CmTemplateProcessor processor;

    @Test
    void testRoleConfigsWithMultipleVolumes() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(3);

        assertEquals(List.of(
                config("schema.registry.jar.storage.directory.path", "/hadoopfs/fs1/schema_registry")),
                subject.getRoleConfigs(SCHEMA_REGISTRY_SERVER, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithSingleVolume() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);

        assertEquals(List.of(
                config("schema.registry.jar.storage.directory.path", "/hadoopfs/fs1/schema_registry")),
                subject.getRoleConfigs(SCHEMA_REGISTRY_SERVER, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithoutVolumes() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(0);

        assertEquals(List.of(
                config("schema.registry.jar.storage.directory.path", "/hadoopfs/root1/schema_registry")),
                subject.getRoleConfigs(SCHEMA_REGISTRY_SERVER, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testGetSchemaRegistryInstanceCount() {
        assertEquals(1, getSchemaRegistryInstanceCount(getTemplatePreparationObject(1)));
        assertEquals(2, getSchemaRegistryInstanceCount(getTemplatePreparationObject(1, 1)));
        assertEquals(5, getSchemaRegistryInstanceCount(getTemplatePreparationObject(2, 2, 1)));
    }

    @Test
    void testCloudStorageIsChosenWhenCdhVersionIsAtLeast711() {
        cdhMainVersionIs("7.2.0");
        TemplatePreparationObject tpo = getTemplatePreparationObject(1);
        HostgroupView hostGroup = tpo.getHostGroupsWithComponent(SCHEMA_REGISTRY_SERVER).findFirst().get();
        assertEquals(List.of(
                config(CONFIG_JAR_STORAGE_DIRECTORY_PATH, "/schema-registry"),
                config(CONFIG_JAR_STORAGE_TYPE, "hdfs")),
                subject.getRoleConfigs(SCHEMA_REGISTRY_SERVER, hostGroup, tpo));
    }

    @Test
    void testLocalStorageIsChosenWhenSingleSchemaRegistryInstance() {
        cdhMainVersionIs("7.1.0");
        TemplatePreparationObject tpo = getTemplatePreparationObject(1);
        HostgroupView hostGroup = tpo.getHostGroupsWithComponent(SCHEMA_REGISTRY_SERVER).findFirst().get();
        assertEquals(List.of(
                config(CONFIG_JAR_STORAGE_DIRECTORY_PATH, "/hadoopfs/root1/schema_registry")),
                subject.getRoleConfigs(SCHEMA_REGISTRY_SERVER, hostGroup, tpo));
    }

    private TemplatePreparationObject getTemplatePreparationObject(Integer... instanceCountsForSchemaRegistryHostGroups) {
        List<HostgroupView> srHostGroups = Arrays.stream(instanceCountsForSchemaRegistryHostGroups)
                .map(nodeCnt -> new HostgroupView(null, 0, InstanceGroupType.CORE, nodeCnt))
                .collect(toList());
        TemplatePreparationObject tpo = mock(TemplatePreparationObject.class, withSettings().lenient());
        when(tpo.getHostGroupsWithComponent(SCHEMA_REGISTRY_SERVER)).thenAnswer(__ -> srHostGroups.stream());
        when(tpo.getBlueprintView()).thenReturn(blueprintView);
        return tpo;
    }

    private TemplatePreparationObject getTemplatePreparationObject(HostgroupView hostGroup) {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/kafka.bp");
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(hostGroup))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, new CmTemplateProcessor(inputJson)))
                .build();
        return preparationObject;
    }

    private void cdhMainVersionIs(String version) {
        when(blueprintView.getProcessor()).thenReturn(processor);
        when(processor.getVersion()).thenReturn(Optional.ofNullable(version));
    }
}