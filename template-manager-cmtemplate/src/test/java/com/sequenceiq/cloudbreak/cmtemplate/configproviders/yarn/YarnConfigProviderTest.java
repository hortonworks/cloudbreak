package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles;

@ExtendWith(MockitoExtension.class)
class YarnConfigProviderTest {

    @Mock
    private HdfsConfigHelper hdfsConfigHelper;

    @InjectMocks
    private YarnConfigProvider underTest;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

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
    public void testGetRoleConfigsForGatewayRoleNonHybrid() {
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(YarnRoles.GATEWAY, cmTemplateProcessor, null);
        assertEquals(1, roleConfigs.size());
        assertEquals("mapreduce_client_env_safety_valve", roleConfigs.get(0).getName());
        assertEquals("HADOOP_OPTS=\"-Dorg.wildfly.openssl.path=/usr/lib64 ${HADOOP_OPTS}\"", roleConfigs.get(0).getValue());
    }

    @Test
    public void testGetRoleConfigsForGatewayRoleHybrid() {
        when(cmTemplateProcessor.isHybridDatahub(null)).thenReturn(true);
        when(hdfsConfigHelper.getHdfsUrl(cmTemplateProcessor, null)).thenReturn(Optional.of("hdfs://nshybrid"));

        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(YarnRoles.GATEWAY, cmTemplateProcessor, null);

        assertEquals(2, roleConfigs.size());
        assertEquals("mapreduce_client_env_safety_valve", roleConfigs.get(0).getName());
        assertEquals("HADOOP_OPTS=\"-Dorg.wildfly.openssl.path=/usr/lib64 ${HADOOP_OPTS}\"", roleConfigs.get(0).getValue());
        assertEquals("mapreduce_application_framework_path", roleConfigs.get(1).getName());
        assertEquals("hdfs://nshybrid/user/yarn/mapreduce/mr-framework/{version}-mr-framework.tar.gz#mr-framework", roleConfigs.get(1).getValue());
    }

    @Test
    public void testGetRoleConfigsForNonGatewayRole() {
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(YarnRoles.JOBHISTORY, cmTemplateProcessor, null);
        assertEquals(0, roleConfigs.size());
    }

}
