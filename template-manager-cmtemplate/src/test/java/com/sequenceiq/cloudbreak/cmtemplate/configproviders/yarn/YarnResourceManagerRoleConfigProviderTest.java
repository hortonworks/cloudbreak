package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

@RunWith(MockitoJUnitRunner.class)
public class YarnResourceManagerRoleConfigProviderTest {

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    private YarnResourceManagerRoleConfigProvider underTest = new YarnResourceManagerRoleConfigProvider();

    @Test
    public void testGetConfigsWhenRoleIsResourceManager() {
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(YarnRoles.RESOURCEMANAGER, null);
        assertEquals(2, serviceConfigs.size());
        assertTrue(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "resourcemanager_capacity_scheduler_configuration")));
        assertTrue(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "yarn_resourcemanager_scheduler_class")));
    }

    @Test
    public void testGetConfigsWhenRoleIsNotResourceManager() {
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getRoleConfigs(YarnRoles.NODEMANAGER, null);
        assertEquals(0, serviceConfigs.size());
        assertFalse(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "resourcemanager_capacity_scheduler_configuration")));
        assertFalse(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "yarn_resourcemanager_scheduler_class")));
    }

    @Test
    public void testIsConfigurableWhenLlapIsPresent() {
        when(cmTemplateProcessor.getServiceByType(eq(HiveRoles.HIVELLAP))).thenReturn(Optional.of(new ApiClusterTemplateService()));
        when(cmTemplateProcessor.isRoleTypePresentInService(anyString(), anyList())).thenReturn(true);
        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, null));
    }

    @Test
    public void testIsConfigurableWhenLlapIsNotPresent() {
        when(cmTemplateProcessor.getServiceByType(eq(HiveRoles.HIVELLAP))).thenReturn(Optional.empty());
        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, null));
    }

}
