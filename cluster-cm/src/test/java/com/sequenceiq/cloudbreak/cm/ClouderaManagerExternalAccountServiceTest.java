package com.sequenceiq.cloudbreak.cm;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.ExternalAccountsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiExternalAccountList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;

public class ClouderaManagerExternalAccountServiceTest {

    @Mock
    private ApiClient apiClient;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ExternalAccountsResourceApi externalAccountsResourceApi;

    @InjectMocks
    private ClouderaManagerExternalAccountService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateExternalAccount() throws ApiException {
        ApiExternalAccountList apiExternalAccountList = new ApiExternalAccountList();
        when(clouderaManagerApiFactory.getExternalAccountsResourceApi(apiClient)).thenReturn(externalAccountsResourceApi);
        when(externalAccountsResourceApi.readAccounts(anyString(), anyString())).thenReturn(apiExternalAccountList);

        underTest.createExternalAccount("cb-altus-access", null, "ALTUS_ACCESS_KEY", new HashMap<>(), apiClient);

        verify(externalAccountsResourceApi, times(1)).readAccounts(anyString(), anyString());
    }
}
