package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_2;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;

public class ClouderaManagerConfigServiceTest {

    private static final String VERSION_7_0_2 = "7.0.2";

    private static final String VERSION_7_0_1 = "7.0.1";

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @InjectMocks
    private ClouderaManagerConfigService underTest;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSetCdpEnvironmentWhenCmVersion702() throws ApiException {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        ApiVersionInfo version702 = new ApiVersionInfo().version(VERSION_7_0_2);
        when(clouderaManagerResourceApi.getVersion()).thenReturn(version702);

        underTest.setCdpEnvironmentIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_0_2, new ApiClient(), new HttpClientConfig(""));

        ArgumentCaptor<ApiConfigList> apiConfigListCaptor = ArgumentCaptor.forClass(ApiConfigList.class);
        verify(clouderaManagerResourceApi).updateConfig(eq(""), apiConfigListCaptor.capture());
        List<ApiConfigList> capturedApiConfigList = apiConfigListCaptor.getAllValues();
        assertThat(capturedApiConfigList, hasSize(1));
        assertThat(capturedApiConfigList.get(0).getItems(), hasSize(1));
        ApiConfig apiConfig = capturedApiConfigList.get(0).getItems().get(0);
        assertEquals(apiConfig.getName(), "cdp_environment");
        assertEquals(apiConfig.getValue(), "PUBLIC_CLOUD");
    }

    @Test
    public void testSetCdpEnvironmentWhenCmVersion701() throws ApiException {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        ApiVersionInfo version701 = new ApiVersionInfo().version(VERSION_7_0_1);
        when(clouderaManagerResourceApi.getVersion()).thenReturn(version701);

        underTest.setCdpEnvironmentIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_0_2, new ApiClient(), new HttpClientConfig(""));

        verify(clouderaManagerResourceApi, never()).updateConfig(any(), any());
    }
}
