package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerConfigModificationServiceTest {

    @Mock
    private ClouderaManagerConfigService configService;

    @InjectMocks
    private ClouderaManagerConfigModificationService underTest;

    @Test
    void testUpdateConfig() throws Exception {
        mockConfigServiceCalls();

        Table<String, String, String> configTable = HashBasedTable.create();
        configTable.put("serviceType1", "serviceConfig1", "newValue1");
        configTable.put("serviceType2", "serviceConfig2", "newValue2");
        configTable.put("serviceType2", "roleConfigGroupConfig2", "newValue3");
        configTable.put("serviceType3", "roleConfigGroupConfig3", "newValue4");
        underTest.updateConfig(configTable, null, new Stack());

        verify(configService, times(3)).readServiceConfig(any(), any(), any());
        verify(configService, times(2)).readRoleConfigGroupConfigs(any(), any(), any());

        ArgumentCaptor<Map<String, String>> modifiedServiceConfigsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> modifiedRoleConfigGroupConfigsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(configService, times(2)).modifyServiceConfigs(any(), any(), modifiedServiceConfigsCaptor.capture(), any());
        verify(configService, times(2))
                .modifyRoleConfigGroups(any(), any(), any(), any(), modifiedRoleConfigGroupConfigsCaptor.capture());
        assertTrue(modifiedServiceConfigsCaptor.getAllValues().stream().map(Map::keySet).flatMap(Set::stream).allMatch(config ->
                Set.of("serviceConfig1", "serviceConfig2").contains(config)));
        assertTrue(modifiedRoleConfigGroupConfigsCaptor.getAllValues().stream().map(Map::keySet).flatMap(Set::stream).allMatch(config ->
                Set.of("roleConfigGroupConfig2", "roleConfigGroupConfig3").contains(config)));
    }

    @Test
    void testUpdateConfigWhenConfigMissing() {
        mockConfigServiceCalls();
        when(configService.readServiceConfig(any(), any(), eq("service2"))).thenReturn(new ApiServiceConfig()
                .addItemsItem(apiConfig("other", "old")));

        Table<String, String, String> configTable = HashBasedTable.create();
        configTable.put("serviceType1", "serviceConfig1", "newValue1");
        configTable.put("serviceType2", "serviceConfig2", "newValue2");
        configTable.put("serviceType2", "roleConfigGroupConfig2", "newValue3");
        configTable.put("serviceType3", "roleConfigGroupConfig3", "newValue4");
        assertThrows(CloudbreakServiceException.class, () -> underTest.updateConfig(configTable, null, new Stack()));

        verify(configService, times(3)).readServiceConfig(any(), any(), any());
        verify(configService, times(2)).readRoleConfigGroupConfigs(any(), any(), any());
        verify(configService, times(0)).modifyServiceConfigs(any(), any(), any(), any());
        verify(configService, times(0)).modifyRoleConfigGroups(any(), any(), any(), any(), any());
    }

    private void mockConfigServiceCalls() {
        when(configService.readServices(any(), any())).thenReturn(new ApiServiceList()
                .addItemsItem(apiService("service1", "serviceType1"))
                .addItemsItem(apiService("service2", "serviceType2"))
                .addItemsItem(apiService("service3", "serviceType3")));
        when(configService.readServiceConfig(any(), any(), eq("service1"))).thenReturn(new ApiServiceConfig()
                .addItemsItem(apiConfig("serviceConfig1", "old")));
        lenient().when(configService.readServiceConfig(any(), any(), eq("service2"))).thenReturn(new ApiServiceConfig()
                .addItemsItem(apiConfig("serviceConfig2", "old")));
        when(configService.readServiceConfig(any(), any(), eq("service3"))).thenReturn(new ApiServiceConfig()
                .addItemsItem(apiConfig("serviceConfig3", "old")));
        when(configService.readRoleConfigGroupConfigs(any(), any(), eq("service2"))).thenReturn(new ApiRoleConfigGroupList().addItemsItem(
                new ApiRoleConfigGroup().config(new ApiConfigList().addItemsItem(apiConfig("roleConfigGroupConfig2", "old")))));
        when(configService.readRoleConfigGroupConfigs(any(), any(), eq("service3"))).thenReturn(new ApiRoleConfigGroupList().addItemsItem(
                new ApiRoleConfigGroup().config(new ApiConfigList().addItemsItem(apiConfig("roleConfigGroupConfig3", "old")))));
        lenient().doNothing().when(configService).modifyServiceConfigs(any(), any(), any(Map.class), any());
        lenient().doNothing().when(configService).modifyRoleConfigGroups(any(), any(), any(), any(), any());
    }

    private ApiService apiService(String name, String type) {
        return new ApiService().name(name).type(type);
    }

    private ApiConfig apiConfig(String key, String value) {
        return new ApiConfig().name(key).value(value);
    }
}
