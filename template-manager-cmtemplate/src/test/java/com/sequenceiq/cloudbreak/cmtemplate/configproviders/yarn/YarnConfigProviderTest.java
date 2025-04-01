package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
public class YarnConfigProviderTest {

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    private YarnConfigProvider underTest = new YarnConfigProvider();

    @Test
    public void testGetConfigsWhenLlapIsPresent() {
        when(cmTemplateProcessor.getServiceByType(eq(HiveRoles.HIVELLAP))).thenReturn(Optional.of(new ApiClusterTemplateService()));
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, null);
        assertEquals(1, serviceConfigs.size());
        assertTrue(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "yarn_service_config_safety_valve")));
    }

    @Test
    public void testGetConfigsWhenLlapIsNotPresent() {
        when(cmTemplateProcessor.getServiceByType(eq(HiveRoles.HIVELLAP))).thenReturn(Optional.empty());
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, null);
        assertEquals(0, serviceConfigs.size());
        assertFalse(serviceConfigs.stream().anyMatch(sc -> StringUtils.equals(sc.getName(), "yarn_service_config_safety_valve")));
    }

    @Test
    public void testGetRoleConfigsForGatewayRole() {
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(YarnRoles.GATEWAY, cmTemplateProcessor, null);
        assertEquals(1, roleConfigs.size());
        assertEquals("mapreduce_client_env_safety_valve", roleConfigs.get(0).getName());
        assertEquals("HADOOP_OPTS=\"-Dorg.wildfly.openssl.path=/usr/lib64 ${HADOOP_OPTS}\"", roleConfigs.get(0).getValue());
    }

    @Test
    public void testGetRoleConfigsForNonGatewayRole() {
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(YarnRoles.JOBHISTORY, cmTemplateProcessor, null);
        assertEquals(0, roleConfigs.size());
    }

}
