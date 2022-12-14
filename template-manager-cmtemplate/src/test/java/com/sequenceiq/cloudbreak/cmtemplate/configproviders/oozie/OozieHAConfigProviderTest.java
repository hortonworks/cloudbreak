package com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie.OozieRoleConfigProviderTest.getBlueprintText;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie.OozieRoleConfigProviderTest.getTemplatePreparationObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@RunWith(MockitoJUnitRunner.class)
public class OozieHAConfigProviderTest {

    private final OozieHAConfigProvider underTest = new OozieHAConfigProvider();

    @Test
    public void testGetServiceConfigsWithSingleRolesPerHostGroup() {
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor, 1, false);

        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));
    }

    @Test
    public void testGetServiceConfigsWithOozieHA() {
        String inputJson = getBlueprintText("input/de-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor, 2, false);

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
    public void testGetServiceConfigsWithOozieHAWithSSlTrue() {
        String inputJson = getBlueprintText("input/de-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor, 2, true);

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
    public void testGetServiceConfigsWithNoOozie() {
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor, 1, false);

        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));
    }
}