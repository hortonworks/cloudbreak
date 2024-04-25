package com.sequenceiq.cloudbreak.cm.config.modification;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerConfigService;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerServiceManagementService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerConfigModificationServiceTest {

    @Mock
    private ClouderaManagerConfigService configService;

    @Mock
    private ClouderaManagerServiceManagementService clouderaManagerServiceManagementService;

    @InjectMocks
    private ClouderaManagerConfigModificationService underTest;

    @Test
    void testServiceNames() {
        mockReadServices();

        List<CmConfig> configs = getConfigs();
        List<String> result = underTest.getServiceNames(configs, null, new Stack());

        assertTrue(result.containsAll(List.of("service1", "service2", "service3")));
    }

    @Test
    void testUpdateConfig() throws Exception {
        mockConfigServiceCalls();

        List<CmConfig> configs = getConfigs();
        underTest.updateConfigs(configs, null, new Stack());

        verify(configService, times(3)).readServiceConfig(any(), any(), any());
        verify(configService, times(2)).readRoleConfigGroupConfigs(any(), any(), any());

        ArgumentCaptor<Map<String, String>> modifiedServiceConfigsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> modifiedRoleConfigGroupConfigsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(configService, times(2)).modifyServiceConfigs(any(), any(), modifiedServiceConfigsCaptor.capture(), any());
        verify(configService, times(2))
                .modifyRoleConfigGroup(any(), any(), any(), any(), modifiedRoleConfigGroupConfigsCaptor.capture());
        assertTrue(modifiedServiceConfigsCaptor.getAllValues().stream().map(Map::keySet).flatMap(Set::stream).allMatch(config ->
                Set.of("serviceConfig1", "serviceConfig2").contains(config)));
        assertTrue(modifiedRoleConfigGroupConfigsCaptor.getAllValues().stream().map(Map::keySet).flatMap(Set::stream).allMatch(config ->
                Set.of("roleConfigGroupConfig2", "roleConfigGroupConfig3").contains(config)));
    }

    @Test
    void testUpdateConfigWhenConfigMissing() throws Exception {
        mockReadServices();
        when(configService.readServiceConfig(any(), any(), eq("service1"))).thenReturn(new ApiServiceConfig()
                .addItemsItem(apiConfig("other", "old")));
        lenient().when(configService.readServiceConfig(any(), any(), eq("service2"))).thenReturn(new ApiServiceConfig()
                .addItemsItem(apiConfig("other", "old")));
        when(configService.readServiceConfig(any(), any(), eq("service3"))).thenReturn(new ApiServiceConfig()
                .addItemsItem(apiConfig("other", "old")));
        when(configService.readRoleConfigGroupConfigs(any(), any(), eq("service1"))).thenReturn(new ApiRoleConfigGroupList().addItemsItem(
                new ApiRoleConfigGroup().name("roleConfigGroup2").config(new ApiConfigList().addItemsItem(apiConfig("other", "old")))));
        when(configService.readRoleConfigGroupConfigs(any(), any(), eq("service2"))).thenReturn(new ApiRoleConfigGroupList().addItemsItem(
                new ApiRoleConfigGroup().name("roleConfigGroup2").config(new ApiConfigList().addItemsItem(apiConfig("other", "old")))));
        when(configService.readRoleConfigGroupConfigs(any(), any(), eq("service3"))).thenReturn(new ApiRoleConfigGroupList().addItemsItem(
                new ApiRoleConfigGroup().name("roleConfigGroup3").config(new ApiConfigList().addItemsItem(apiConfig("other", "old")))));

        List<CmConfig> configs = getConfigs();
        underTest.updateConfigs(configs, null, new Stack());

        verify(configService, times(3)).readServiceConfig(any(), any(), any());
        verify(configService, times(3)).readRoleConfigGroupConfigs(any(), any(), any());
        verify(configService, times(0)).modifyServiceConfigs(any(), any(), any(), any());
        verify(configService, times(0)).modifyRoleConfigGroup(any(), any(), any(), any(), any());
    }

    @Test
    void testUpdateConfigWhenServiceMissing() throws Exception {
        mockConfigServiceCalls();

        List<CmConfig> configs = getConfigs();
        configs.add(new CmConfig(new CmServiceType("unknownService"), "anycolumn", "anyconfigvalue"));
        underTest.updateConfigs(configs, null, new Stack());

        verify(configService, times(3)).readServiceConfig(any(), any(), any());
        verify(configService, times(2)).readRoleConfigGroupConfigs(any(), any(), any());
        verify(configService, times(2)).modifyServiceConfigs(any(), any(), any(), any());
        verify(configService, times(2)).modifyRoleConfigGroup(any(), any(), any(), any(), any());
        verify(configService, times(0)).modifyServiceConfigs(any(), any(), any(), eq("unknownService"));
        verify(configService, times(0)).modifyRoleConfigGroup(any(), any(), eq("unknownService"), any(), any());
    }

    private void mockConfigServiceCalls() {
        mockReadServices();
        when(configService.readServiceConfig(any(), any(), eq("service1"))).thenReturn(new ApiServiceConfig()
                .addItemsItem(apiConfig("serviceConfig1", "old")));
        lenient().when(configService.readServiceConfig(any(), any(), eq("service2"))).thenReturn(new ApiServiceConfig()
                .addItemsItem(apiConfig("serviceConfig2", "old")));
        when(configService.readServiceConfig(any(), any(), eq("service3"))).thenReturn(new ApiServiceConfig()
                .addItemsItem(apiConfig("serviceConfig3", "old")));
        when(configService.readRoleConfigGroupConfigs(any(), any(), eq("service2"))).thenReturn(new ApiRoleConfigGroupList().addItemsItem(
                new ApiRoleConfigGroup().name("roleConfigGroup2").config(new ApiConfigList().addItemsItem(apiConfig("roleConfigGroupConfig2", "old")))));
        when(configService.readRoleConfigGroupConfigs(any(), any(), eq("service3"))).thenReturn(new ApiRoleConfigGroupList().addItemsItem(
                new ApiRoleConfigGroup().name("roleConfigGroup3").config(new ApiConfigList().addItemsItem(apiConfig("roleConfigGroupConfig3", "old")))));
        lenient().doNothing().when(configService).modifyServiceConfigs(any(), any(), any(Map.class), any());
        lenient().doNothing().when(configService).modifyRoleConfigGroup(any(), any(), any(), any(), any());
    }

    private void mockReadServices() {
        when(clouderaManagerServiceManagementService.readServices(any(), any())).thenReturn(new ApiServiceList()
                .addItemsItem(apiService("service1", "serviceType1"))
                .addItemsItem(apiService("service2", "serviceType2"))
                .addItemsItem(apiService("service3", "serviceType3")));
    }

    private ApiService apiService(String name, String type) {
        return new ApiService().name(name).type(type);
    }

    private ApiConfig apiConfig(String key, String value) {
        return new ApiConfig().name(key).value(value);
    }

    private List<CmConfig> getConfigs() {
        List<CmConfig> configs = Lists.newArrayList();
        configs.add(new CmConfig(new CmServiceType("serviceType1"), "serviceConfig1", "newValue1"));
        configs.add(new CmConfig(new CmServiceType("serviceType2"), "serviceConfig2", "newValue2"));
        configs.add(new CmConfig(new CmServiceType("serviceType2"), "roleConfigGroupConfig2", "newValue3"));
        configs.add(new CmConfig(new CmServiceType("serviceType3"), "roleConfigGroupConfig3", "newValue4"));
        return configs;
    }
}
