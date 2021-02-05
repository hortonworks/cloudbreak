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
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;

public class ClouderaManagerConfigServiceTest {

    private static final String VERSION_7_0_2 = "7.0.2";

    private static final String VERSION_7_0_1 = "7.0.1";

    private static final String VERSION_7_1_0 = "7.1.0";

    private static final String TEST_CLUSTER_NAME = "test-cluster-name";

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @InjectMocks
    private ClouderaManagerConfigService underTest;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);
    }

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
        String hueName = "hue-1";
        String configName = "config_setting";
        String configValue = "new-config-value";
        ServicesResourceApi serviceResourceApi = mock(ServicesResourceApi.class);
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name("hbase-1").type("HBASE"));

        when(serviceResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(serviceResourceApi);

        underTest.modifyServiceConfigValue(new ApiClient(), TEST_CLUSTER_NAME, hueType, configName, configValue);

        verify(serviceResourceApi, never()).updateServiceConfig(any(), any(), any(), any());
    }
}
