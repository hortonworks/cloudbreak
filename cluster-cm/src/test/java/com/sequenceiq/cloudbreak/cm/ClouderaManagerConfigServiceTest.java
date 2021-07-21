package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerConfigServiceTest {

    private static final String VERSION_7_0_1 = "7.0.1";

    private static final String VERSION_7_1_0 = "7.1.0";

    private static final String TEST_CLUSTER_NAME = "test-cluster-name";

    private static final ApiClient API_CLIENT = new ApiClient();

    private static final String NIFI_SERVICE = "NIFI";

    private static final String NIFI_ROLE = "NIFI-ROLE";

    private static final String NIFI_SERVICE_TYPE = "NIFI-SERVICE-TYPE";

    private static final String NIFI_CONFIG_GROUP = "NIFI-CONFIG-GROUP";

    private static final String CONFIG_NAME = "config-name";

    private static final String DEFAULT_VALUE = "/var/lib";

    private static final String CONFIG_VALUE = "/hadoop/fs1";

    private static final String CONFIG_VIEW = "full";

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @InjectMocks
    private ClouderaManagerConfigService underTest;

    @Test
    public void testDisableKnoxAutorestartIfCmVersionAtLeast() throws ApiException {
        setUpCMVersion(VERSION_7_1_0);

        ServicesResourceApi serviceResourceApi = mock(ServicesResourceApi.class);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(serviceResourceApi);

        String knoxName = "knox-e07";
        ApiServiceList apiServiceList = new ApiServiceList()
                .addItemsItem(new ApiService().name("hbase-a63").type("HBASE"))
                .addItemsItem(new ApiService().name(knoxName).type("KNOX"));
        when(serviceResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);

        underTest.disableKnoxAutorestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, new ApiClient(), TEST_CLUSTER_NAME);

        ArgumentCaptor<ApiServiceConfig> apiServiceConfigArgumentCaptor = ArgumentCaptor.forClass(ApiServiceConfig.class);
        verify(serviceResourceApi, times(1))
                .updateServiceConfig(eq(TEST_CLUSTER_NAME), eq(knoxName), eq(""), apiServiceConfigArgumentCaptor.capture());

        ApiServiceConfig actualBody = apiServiceConfigArgumentCaptor.getValue();
        assertFalse(actualBody.getItems().isEmpty());
        ApiConfig actualApiConfig = actualBody.getItems().get(0);
        assertEquals(ClouderaManagerConfigService.KNOX_AUTORESTART_ON_STOP, actualApiConfig.getName());
        assertEquals(Boolean.FALSE.toString(), actualApiConfig.getValue());
    }

    private void setUpCMVersion(String version) throws ApiException {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        ApiVersionInfo version701 = new ApiVersionInfo().version(version);
        when(clouderaManagerResourceApi.getVersion()).thenReturn(version701);
    }

    @Test
    public void testDisableKnoxAutorestartIfCmVersionAtLeastWhenKnoxIsMissing() throws ApiException {
        setUpCMVersion(VERSION_7_1_0);

        ServicesResourceApi serviceResourceApi = mock(ServicesResourceApi.class);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(serviceResourceApi);

        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name("hbase-a63").type("HBASE"));
        when(serviceResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);

        underTest.disableKnoxAutorestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, new ApiClient(), TEST_CLUSTER_NAME);

        verify(serviceResourceApi, never()).updateServiceConfig(any(), any(), any(), any());
    }

    @Test
    public void testDisableKnoxAutorestartIfCmVersionAtLeastWithLowerVersion() throws ApiException {
        setUpCMVersion(VERSION_7_0_1);

        underTest.disableKnoxAutorestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, new ApiClient(), TEST_CLUSTER_NAME);
        verify(clouderaManagerApiFactory, never()).getServicesResourceApi(any());
    }

    @Test
    public void testModifyServiceConfigValue() throws ApiException {
        String hueType = "HUE";
        String hueName = "hue-1";
        String configName = "config_setting";
        String configValue = "new-config-value";
        ServicesResourceApi serviceResourceApi = mock(ServicesResourceApi.class);
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(hueName).type(hueType));

        when(serviceResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(serviceResourceApi);

        underTest.modifyServiceConfigValue(new ApiClient(), TEST_CLUSTER_NAME, hueType, configName, configValue);

        ArgumentCaptor<ApiServiceConfig> apiServiceConfigArgumentCaptor = ArgumentCaptor.forClass(ApiServiceConfig.class);
        verify(serviceResourceApi, times(1))
                .updateServiceConfig(eq(TEST_CLUSTER_NAME), eq(hueName), eq(""), apiServiceConfigArgumentCaptor.capture());

        ApiServiceConfig actualBody = apiServiceConfigArgumentCaptor.getValue();
        assertFalse(actualBody.getItems().isEmpty());
        ApiConfig actualApiConfig = actualBody.getItems().get(0);
        assertEquals(configName, actualApiConfig.getName());
        assertEquals(configValue, actualApiConfig.getValue());
    }

    @Test
    public void testModifyServiceConfigValueServiceMissing() throws ApiException {
        String hueType = "HUE";
        String configName = "config_setting";
        String configValue = "new-config-value";
        ServicesResourceApi serviceResourceApi = mock(ServicesResourceApi.class);
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name("hbase-1").type("HBASE"));

        when(serviceResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(serviceResourceApi);

        underTest.modifyServiceConfigValue(new ApiClient(), TEST_CLUSTER_NAME, hueType, configName, configValue);

        verify(serviceResourceApi, never()).updateServiceConfig(any(), any(), any(), any());
    }

    @Test
    public void testGetRoleConfigValueByServiceTypeShouldReturnTheConfigValue() throws ApiException {
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = Mockito.mock(RoleConfigGroupsResourceApi.class);
        ServicesResourceApi servicesResourceApi = Mockito.mock(ServicesResourceApi.class);
        when(clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(API_CLIENT)).thenReturn(roleConfigGroupsResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(API_CLIENT)).thenReturn(servicesResourceApi);
        List<ApiService> services = List.of(createApiService(NIFI_SERVICE, NIFI_SERVICE_TYPE), createApiService("SPARK", "SPARK-ROLE"));
        when(servicesResourceApi.readServices(eq(TEST_CLUSTER_NAME), any())).thenReturn(createApiServiceList(services));

        ApiRoleConfigGroupList configGroupList = createApiRoleConfigGroups(List.of(createConfigGroup(NIFI_CONFIG_GROUP, NIFI_ROLE),
                createConfigGroup("SPARK-GROUP", "SPARK-ROLE")));
        when(roleConfigGroupsResourceApi.readRoleConfigGroups(TEST_CLUSTER_NAME, NIFI_SERVICE)).thenReturn(configGroupList);

        ApiConfigList roleConfig = createApiConfigList(List.of(createConfig(CONFIG_VALUE, null)));
        when(roleConfigGroupsResourceApi.readConfig(TEST_CLUSTER_NAME, NIFI_CONFIG_GROUP, NIFI_SERVICE, CONFIG_VIEW)).thenReturn(roleConfig);

        Optional<String> actual = underTest.getRoleConfigValueByServiceType(API_CLIENT, TEST_CLUSTER_NAME, NIFI_ROLE, NIFI_SERVICE_TYPE, CONFIG_NAME);

        assertEquals(Optional.of(CONFIG_VALUE), actual);
        verify(clouderaManagerApiFactory).getRoleConfigGroupsResourceApi(API_CLIENT);
        verify(clouderaManagerApiFactory).getServicesResourceApi(API_CLIENT);
        verify(servicesResourceApi).readServices(eq(TEST_CLUSTER_NAME), any());
        verify(roleConfigGroupsResourceApi).readRoleConfigGroups(TEST_CLUSTER_NAME, NIFI_SERVICE);
        verify(roleConfigGroupsResourceApi).readConfig(TEST_CLUSTER_NAME, NIFI_CONFIG_GROUP, NIFI_SERVICE, CONFIG_VIEW);
    }

    @Test
    public void testGetRoleConfigValueByServiceTypeShouldReturnTheConfigValueWhenTheValueIsNullAndDefaultValuePresent() throws ApiException {
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = Mockito.mock(RoleConfigGroupsResourceApi.class);
        ServicesResourceApi servicesResourceApi = Mockito.mock(ServicesResourceApi.class);
        when(clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(API_CLIENT)).thenReturn(roleConfigGroupsResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(API_CLIENT)).thenReturn(servicesResourceApi);
        List<ApiService> services = List.of(createApiService(NIFI_SERVICE, NIFI_SERVICE_TYPE), createApiService("SPARK", "SPARK-ROLE"));
        when(servicesResourceApi.readServices(eq(TEST_CLUSTER_NAME), any())).thenReturn(createApiServiceList(services));
        ApiRoleConfigGroupList configGroupList = createApiRoleConfigGroups(List.of(createConfigGroup(NIFI_CONFIG_GROUP, NIFI_ROLE),
                createConfigGroup("SPARK-GROUP", "SPARK-ROLE")));
        when(roleConfigGroupsResourceApi.readRoleConfigGroups(TEST_CLUSTER_NAME, NIFI_SERVICE)).thenReturn(configGroupList);
        ApiConfigList roleConfig = createApiConfigList(List.of(createConfig(null, DEFAULT_VALUE)));
        when(roleConfigGroupsResourceApi.readConfig(TEST_CLUSTER_NAME, NIFI_CONFIG_GROUP, NIFI_SERVICE, CONFIG_VIEW)).thenReturn(roleConfig);

        Optional<String> actual = underTest.getRoleConfigValueByServiceType(API_CLIENT, TEST_CLUSTER_NAME, NIFI_ROLE, NIFI_SERVICE_TYPE, CONFIG_NAME);

        assertEquals(Optional.of(DEFAULT_VALUE), actual);
        verify(clouderaManagerApiFactory).getRoleConfigGroupsResourceApi(API_CLIENT);
        verify(clouderaManagerApiFactory).getServicesResourceApi(API_CLIENT);
        verify(servicesResourceApi).readServices(eq(TEST_CLUSTER_NAME), any());
        verify(roleConfigGroupsResourceApi).readRoleConfigGroups(TEST_CLUSTER_NAME, NIFI_SERVICE);
        verify(roleConfigGroupsResourceApi).readConfig(TEST_CLUSTER_NAME, NIFI_CONFIG_GROUP, NIFI_SERVICE, CONFIG_VIEW);
    }

    @Test
    public void testGetRoleConfigValueByServiceTypeShouldReturnOptionalEmptyWhenServiceTypeNotFound() throws ApiException {
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = Mockito.mock(RoleConfigGroupsResourceApi.class);
        ServicesResourceApi servicesResourceApi = Mockito.mock(ServicesResourceApi.class);
        when(clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(API_CLIENT)).thenReturn(roleConfigGroupsResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(API_CLIENT)).thenReturn(servicesResourceApi);
        List<ApiService> services = List.of(createApiService("SPARK", "SPARK-ROLE"));
        when(servicesResourceApi.readServices(eq(TEST_CLUSTER_NAME), any())).thenReturn(createApiServiceList(services));

        Optional<String> actual = underTest.getRoleConfigValueByServiceType(API_CLIENT, TEST_CLUSTER_NAME, NIFI_ROLE, NIFI_SERVICE_TYPE, CONFIG_NAME);

        assertEquals(Optional.empty(), actual);
        verify(clouderaManagerApiFactory).getRoleConfigGroupsResourceApi(API_CLIENT);
        verify(clouderaManagerApiFactory).getServicesResourceApi(API_CLIENT);
        verify(servicesResourceApi).readServices(eq(TEST_CLUSTER_NAME), any());
        verifyNoInteractions(roleConfigGroupsResourceApi);
        verifyNoInteractions(roleConfigGroupsResourceApi);
    }

    @Test
    public void testGetRoleConfigValueByServiceTypeShouldReturnOptionalEmptyWhenRoleTypeNotFound() throws ApiException {
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = Mockito.mock(RoleConfigGroupsResourceApi.class);
        ServicesResourceApi servicesResourceApi = Mockito.mock(ServicesResourceApi.class);
        when(clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(API_CLIENT)).thenReturn(roleConfigGroupsResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(API_CLIENT)).thenReturn(servicesResourceApi);
        List<ApiService> services = List.of(createApiService(NIFI_SERVICE, NIFI_SERVICE_TYPE), createApiService("SPARK", "SPARK-ROLE"));
        when(servicesResourceApi.readServices(eq(TEST_CLUSTER_NAME), any())).thenReturn(createApiServiceList(services));
        ApiRoleConfigGroupList configGroupList = createApiRoleConfigGroups(List.of(createConfigGroup("SPARK-GROUP", "SPARK-ROLE")));
        when(roleConfigGroupsResourceApi.readRoleConfigGroups(TEST_CLUSTER_NAME, NIFI_SERVICE)).thenReturn(configGroupList);

        Optional<String> actual = underTest.getRoleConfigValueByServiceType(API_CLIENT, TEST_CLUSTER_NAME, NIFI_ROLE, NIFI_SERVICE_TYPE, CONFIG_NAME);

        assertEquals(Optional.empty(), actual);
        verify(clouderaManagerApiFactory).getRoleConfigGroupsResourceApi(API_CLIENT);
        verify(clouderaManagerApiFactory).getServicesResourceApi(API_CLIENT);
        verify(servicesResourceApi).readServices(eq(TEST_CLUSTER_NAME), any());
        verify(roleConfigGroupsResourceApi, times(1)).readRoleConfigGroups(TEST_CLUSTER_NAME, NIFI_SERVICE);
    }

    @Test
    public void testGetRoleConfigValueByServiceTypeShouldReturnOptionalEmptyWhenTheConfigNotFound() throws ApiException {
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = Mockito.mock(RoleConfigGroupsResourceApi.class);
        ServicesResourceApi servicesResourceApi = Mockito.mock(ServicesResourceApi.class);
        when(clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(API_CLIENT)).thenReturn(roleConfigGroupsResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(API_CLIENT)).thenReturn(servicesResourceApi);
        List<ApiService> services = List.of(createApiService(NIFI_SERVICE, NIFI_SERVICE_TYPE), createApiService("SPARK", "SPARK-ROLE"));
        when(servicesResourceApi.readServices(eq(TEST_CLUSTER_NAME), any())).thenReturn(createApiServiceList(services));
        ApiRoleConfigGroupList configGroupList = createApiRoleConfigGroups(List.of(createConfigGroup(NIFI_CONFIG_GROUP, NIFI_ROLE),
                createConfigGroup("SPARK-GROUP", "SPARK-ROLE")));
        when(roleConfigGroupsResourceApi.readRoleConfigGroups(TEST_CLUSTER_NAME, NIFI_SERVICE)).thenReturn(configGroupList);
        ApiConfigList roleConfig = createApiConfigList(Collections.emptyList());
        when(roleConfigGroupsResourceApi.readConfig(TEST_CLUSTER_NAME, NIFI_CONFIG_GROUP, NIFI_SERVICE, CONFIG_VIEW)).thenReturn(roleConfig);

        Optional<String> actual = underTest.getRoleConfigValueByServiceType(API_CLIENT, TEST_CLUSTER_NAME, NIFI_ROLE, NIFI_SERVICE_TYPE, CONFIG_NAME);

        assertEquals(Optional.empty(), actual);
        verify(clouderaManagerApiFactory).getRoleConfigGroupsResourceApi(API_CLIENT);
        verify(clouderaManagerApiFactory).getServicesResourceApi(API_CLIENT);
        verify(servicesResourceApi).readServices(eq(TEST_CLUSTER_NAME), any());
        verify(roleConfigGroupsResourceApi).readRoleConfigGroups(TEST_CLUSTER_NAME, NIFI_SERVICE);
        verify(roleConfigGroupsResourceApi).readConfig(TEST_CLUSTER_NAME, NIFI_CONFIG_GROUP, NIFI_SERVICE, CONFIG_VIEW);
    }

    private ApiConfigList createApiConfigList(List<ApiConfig> apiConfigs) {
        ApiConfigList apiConfigList = new ApiConfigList();
        apiConfigList.setItems(apiConfigs);
        return apiConfigList;
    }

    private ApiConfig createConfig(String value, String defaultValue) {
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setName(CONFIG_NAME);
        apiConfig.setValue(value);
        apiConfig.setDefault(defaultValue);
        return apiConfig;
    }

    private ApiServiceList createApiServiceList(List<ApiService> services) {
        ApiServiceList apiServiceList = new ApiServiceList();
        apiServiceList.items(services);
        return apiServiceList;
    }

    private ApiService createApiService(String serviceName, String serviceType) {
        ApiService apiService = new ApiService();
        apiService.name(serviceName);
        apiService.setType(serviceType);
        return apiService;
    }

    private ApiRoleConfigGroupList createApiRoleConfigGroups(List<ApiRoleConfigGroup> apiRoleConfigGroups) {
        ApiRoleConfigGroupList apiRoleConfigGroupList = new ApiRoleConfigGroupList();
        apiRoleConfigGroupList.items(apiRoleConfigGroups);
        return apiRoleConfigGroupList;
    }

    private ApiRoleConfigGroup createConfigGroup(String configGroupName, String roleType) {
        ApiRoleConfigGroup configGroup = new ApiRoleConfigGroup();
        configGroup.setName(configGroupName);
        configGroup.setRoleType(roleType);
        return configGroup;
    }
}
