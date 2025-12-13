package com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie.OozieRoleConfigProviderTest.getBlueprintText;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie.OozieRoleConfigProviderTest.getTemplatePreparationObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@ExtendWith(MockitoExtension.class)
class OozieHAConfigProviderTest {

    private final OozieHAConfigProvider underTest = new OozieHAConfigProvider();

    @Test
    void testGetServiceConfigsWithSingleRolesPerHostGroup() {
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor, 1, false, "7.2.2");

        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));
    }

    @Test
    void testGetServiceConfigsWithOozieHA() {
        String inputJson = getBlueprintText("input/de-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor, 2, false, "7.2.2");

        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(3, serviceConfigs.size());

        assertEquals("oozie_load_balancer", serviceConfigs.get(0).getName());
        assertEquals("master0.blah.timbuk2.dev.cldr.", serviceConfigs.get(0).getValue());

        assertEquals("oozie_load_balancer_http_port", serviceConfigs.get(1).getName());
        assertEquals("11000", serviceConfigs.get(1).getValue());

        assertEquals("oozie_load_balancer_https_port", serviceConfigs.get(2).getName());
        assertEquals("11443", serviceConfigs.get(2).getValue());
    }

    @Test
    void testGetServiceConfigsWithOozieHAWithSSlTrue() {
        String inputJson = getBlueprintText("input/de-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor, 2, true, "7.2.2");

        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(3, serviceConfigs.size());

        assertEquals("oozie_load_balancer", serviceConfigs.get(0).getName());
        assertEquals("master0.blah.timbuk2.dev.cldr.", serviceConfigs.get(0).getValue());

        assertEquals("oozie_load_balancer_http_port", serviceConfigs.get(1).getName());
        assertEquals("11000", serviceConfigs.get(1).getValue());

        assertEquals("oozie_load_balancer_https_port", serviceConfigs.get(2).getName());
        assertEquals("11443", serviceConfigs.get(2).getValue());
    }

    @Test
    void testGetServiceConfigsWithNoOozie() {
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor, 1, false, "7.2.2");

        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));
    }
}