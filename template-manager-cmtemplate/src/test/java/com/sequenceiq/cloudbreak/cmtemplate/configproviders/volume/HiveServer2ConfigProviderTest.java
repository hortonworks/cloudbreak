package com.sequenceiq.cloudbreak.cmtemplate.configproviders.volume;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class HiveServer2ConfigProviderTest {

    private final HiveServer2ConfigProvider underTest = new HiveServer2ConfigProvider();

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> hiveserver2 = roleConfigs.get("hive-HIVESERVER2-BASE");

        assertEquals(1, hiveserver2.size());
        assertEquals("hive_hs2_config_safety_valve", hiveserver2.get(0).getName());
        assertEquals("hive-hive_server2_wm_namespace", hiveserver2.get(0).getVariable());
    }

    @Test
    public void testGetRoleConfigVariables() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateVariable> roleVariables = underTest.getRoleConfigVariables(cmTemplateProcessor, preparationObject);

        roleVariables.sort(Comparator.comparing(ApiClusterTemplateVariable::getName));
        ApiClusterTemplateVariable hiveserver2Namespace = roleVariables.get(0);

        assertEquals(1, roleVariables.size());
        assertEquals("hive-hive_server2_wm_namespace", hiveserver2Namespace.getName());
        assertEquals("<property><name>hive.server2.wm.namespace</name><value>" + preparationObject.getGeneralClusterConfigs().getUuid() + "</value></property>",
                hiveserver2Namespace.getValue());
    }

    @Test
    public void testIsConfigurationNeededShouldReturnFalseWhenNoHiveServer2Role() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager-no-hs2.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);

        assertFalse(configNeeded);
    }

    @Test
    public void testIsConfigurationNeededShouldReturnTrueWhenHiveServer2RoleProvided() {
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
