package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

@RunWith(MockitoJUnitRunner.class)
public class YarnResourceManagerRoleConfigProviderTest {

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @Mock
    private BlueprintView blueprintView;

    private YarnResourceManagerRoleConfigProvider underTest = new YarnResourceManagerRoleConfigProvider();

    @Test
    public void testGetConfigsWhenRoleIsResourceManager() {
        setupMocks(true, true, "7.2.10");
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(YarnRoles.RESOURCEMANAGER, cmTemplateProcessor, source);
        assertEquals(2, serviceConfigs.size());
        assertTrue(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "resourcemanager_capacity_scheduler_configuration")));
        assertTrue(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "yarn_resourcemanager_scheduler_class")));
    }

    @Test
    public void testGetConfigsWhenRoleIsResourceManagerPlacementVersionPasses() {
        setupMocks(true, true, "7.2.11");
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(YarnRoles.RESOURCEMANAGER, cmTemplateProcessor, source);
        assertEquals(3, serviceConfigs.size());
        assertTrue(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "resourcemanager_capacity_scheduler_configuration")));
        assertTrue(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "yarn_resourcemanager_scheduler_class")));
        assertTrue(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "resourcemanager_config_safety_valve")));

        // Note: This is explicitly not referring to the constants. If the constants change, make sure to rationalize
        //  changes with the YARN script which processes these.
        ApiClusterTemplateConfig rmSafetyValve = serviceConfigs.stream().filter(
                sc -> StringUtils.equals(sc.getName(), "resourcemanager_config_safety_valve")).findFirst().get();
        assertEquals("<property>" +
                "<name>yarn.resourcemanager.am.placement-preference-with-node-attributes</name>" +
                "<value>ORDER NODES IN NodeInstanceType WITH worker &gt; compute</value></property><property>" +
                "<name>yarn.resourcemanager.non-am.placement-preference-with-node-attributes</name>" +
                "<value>ORDER NODES IN NodeInstanceType WITH compute &gt; worker</value></property>",
                rmSafetyValve.getValue());
    }

    @Test
    public void testGetConfigsWhenRoleIsNotResourceManager() {
        setupMocks(false, false, null);
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(YarnRoles.NODEMANAGER, cmTemplateProcessor, source);
        assertEquals(0, serviceConfigs.size());
        assertFalse(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "resourcemanager_capacity_scheduler_configuration")));
        assertFalse(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "yarn_resourcemanager_scheduler_class")));
    }

    @Test
    public void testIsConfigurableWhenLlapIsPresent() {
        setupMocks(true, true, null);
        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    public void testIsConfigurableWhenLlapIsNotPresent() {
        setupMocks(false, true, null);
        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    public void testIsConfigurableWhenLlapAbsentCDPVersionPasses1() {
        setupMocks(false, true, "7.2.11");
        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    public void testIsConfigurableWhenLlapAbsentCDPVersionPasses2() {
        setupMocks(false, true, "7.2.12");
        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    public void testIsConfigurableWhenLlapAbsentCDPVersionPasses3() {
        setupMocks(false, true, "7.3.0");
        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    @Test
    public void testIsConfigurableWhenLlapAbsentCDPVersionFails() {
        setupMocks(false, true, "7.2.10");
        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, source));
    }

    private void setupMocks(boolean hasLlap, boolean hasRm, String version) {
        when(blueprintView.getProcessor()).thenReturn(cmTemplateProcessor);
        when(source.getBlueprintView()).thenReturn(blueprintView);
        if (hasRm) {
            when(cmTemplateProcessor.isRoleTypePresentInService(anyString(), anyList())).thenReturn(true);
        }
        if (hasLlap) {
            when(cmTemplateProcessor.getServiceByType(HiveRoles.HIVELLAP)).thenReturn(Optional.of(mock(ApiClusterTemplateService.class)));
        }

        when(cmTemplateProcessor.getStackVersion()).thenReturn(version);
    }

}
