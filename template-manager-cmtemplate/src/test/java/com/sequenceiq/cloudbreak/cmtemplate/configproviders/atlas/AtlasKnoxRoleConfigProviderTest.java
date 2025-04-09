package com.sequenceiq.cloudbreak.cmtemplate.configproviders.atlas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@SuppressWarnings("ConstantConditions")
class AtlasKnoxRoleConfigProviderTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    private final EntitlementService entitlementService = mock(EntitlementService.class);

    private final ExposedServiceCollector exposedServiceCollector = mock(ExposedServiceCollector.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtlasKnoxRoleConfigProvider underTest = new AtlasKnoxRoleConfigProvider(entitlementService, exposedServiceCollector, objectMapper);

    @Test
    void atlasKnoxWireEncryptionDisable() {
        wireEncryptionDisable();
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/sdx-md.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        TemplatePreparationObject source = new Builder()
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .withStackType(StackType.DATALAKE)
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, source);
            List<ApiClusterTemplateConfig> atlasConfig = roleConfigs.get("atlas-ATLAS_SERVER-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(atlasConfig);
            assertTrue(configMap.isEmpty());
        });
    }

    @Test
    void atlasKnoxWireEncryptionEnable() {
        wireEncryptionEnable();
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/sdx-md.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        TemplatePreparationObject source = new Builder()
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .withStackType(StackType.DATALAKE)
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, source);
            List<ApiClusterTemplateConfig> atlasConfig = roleConfigs.get("atlas-ATLAS_SERVER-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(atlasConfig);
            assertEquals("true", configMap.get("atlas.entity.audit.differential").getValue());
        });
    }

    @Test
    void atlasKnoxWireEncryptionEnabledWithNewerVersion() {
        wireEncryptionEnable();
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/sdx-md.bp");
        inputJson = inputJson.replace("\"cdhVersion\": \"7.2.10\",", "\"cdhVersion\": \"7.2.18\",");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(inputJson, "7.2.17", "1.0", null, cmTemplateProcessor);
        TemplatePreparationObject source = new Builder()
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(blueprintView)
                .withStackType(StackType.DATALAKE)
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, source);
            List<ApiClusterTemplateConfig> atlasConfig = roleConfigs.get("atlas-ATLAS_SERVER-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(atlasConfig);
            assertNull(configMap.get("atlas.entity.audit.differential"));
            assertEquals(0, configMap.size());
        });
    }

    @Test
    void aatlasKnoxWireEncryptionEnabledWithRequiredVersion() {
        wireEncryptionEnable();
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/sdx-md.bp");
        inputJson = inputJson.replace("\"cdhVersion\": \"7.2.10\",", "\"cdhVersion\": \"7.2.15\",");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.CORE, 2);
        HostgroupView quorum = new HostgroupView("quorum", 0, InstanceGroupType.CORE, 3);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(inputJson, "7.2.15", "1.0", null, cmTemplateProcessor);
        TemplatePreparationObject source = new Builder()
                .withHostgroupViews(Set.of(gateway, master, quorum, worker))
                .withBlueprintView(blueprintView)
                .withStackType(StackType.DATALAKE)
                .build();

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> {
            Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, source);
            List<ApiClusterTemplateConfig> atlasConfig = roleConfigs.get("atlas-ATLAS_SERVER-BASE");
            Map<String, ApiClusterTemplateConfig> configMap = cmTemplateProcessor.mapByName(atlasConfig);
            assertEquals("true", configMap.get("atlas.entity.audit.differential").getValue());
            assertEquals(1, configMap.size());
        });
    }

    @Test
    public void isDatalakeVersionSupportedEmptyBluePrint() {
        assertThrows(IllegalArgumentException.class, () -> underTest.isDatalakeVersionSupported(""));
    }

    @Test
    public void isDatalakeVersionSupportedInvalidBluePrint() {
        assertThrows(RuntimeException.class, () -> underTest.isDatalakeVersionSupported("{\"version\":\"a\"}"));
    }

    @Test
    public void isDatalakeVersionSupportedVersion() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/sdx-md.bp");
        assertTrue(() -> underTest.isDatalakeVersionSupported(inputJson), "Unsupported DataLake version!");
    }

    @Test
    public void isDatalakeVersionSupportedVersionEqualsWithMinimumVersion() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/sdx-md_equals_with_minimum_version.bp");
        assertTrue(() -> underTest.isDatalakeVersionSupported(inputJson), "Unsupported DataLake version!");
    }

    @Test
    public void isDatalakeVersionSupportedUnsupportedVersion() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/sdx-md_lower_version.bp");
        assertFalse(() -> underTest.isDatalakeVersionSupported(inputJson), "Unsupported DataLake version!");
    }

    private void wireEncryptionEnable() {
        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(true);
    }

    private void wireEncryptionDisable() {
        when(entitlementService.isWireEncryptionEnabled(anyString())).thenReturn(false);
    }

}