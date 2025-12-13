package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class HiveServer2ConfigProviderTest {

    private final HiveServer2RoleConfigProvider underTest = new HiveServer2RoleConfigProvider();

    @Test
    void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> hiveserver2 = roleConfigs.get("hive-HIVESERVER2-BASE");

        assertEquals(1, hiveserver2.size());
        assertEquals("hive_hs2_config_safety_valve", hiveserver2.get(0).getName());
        String expected = "<property><name>hive.server2.wm.namespace</name><value>"
                + preparationObject.getGeneralClusterConfigs().getUuid()
                + "</value></property>";
        assertEquals(expected, hiveserver2.get(0).getValue());
    }

    @Test
    void testIsConfigurationNeededShouldReturnFalseWhenNoHiveServer2Role() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager-no-hs2.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);

        assertFalse(configNeeded);
    }

    @Test
    void testIsConfigurationNeededShouldReturnTrueWhenHiveServer2RoleProvided() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);

        assertTrue(configNeeded);
    }

    private TemplatePreparationObject getTemplatePreparationObject() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setUuid("uuid");
        return Builder.builder().withGeneralClusterConfigs(generalClusterConfigs)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
