package com.sequenceiq.cloudbreak.cm;

import static org.mockito.ArgumentMatchers.any;
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

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.client.ApiResponse;
import com.cloudera.api.swagger.model.ApiExternalAccount;
import com.cloudera.api.swagger.model.ApiExternalAccountList;

public class ClouderaManagerExternalAccountServiceTest {

    @InjectMocks
    private ClouderaManagerExternalAccountService underTest;

    @Mock
    private ApiClient apiClient;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateExternalAccount() throws ApiException {
        // GIVEN
        ApiExternalAccount apiExternalAccount = new ApiExternalAccount();
        ApiExternalAccountList apiExternalAccountList = new ApiExternalAccountList();
        ApiResponse readAccountsResponse = new ApiResponse<>(0, null, apiExternalAccountList);
        ApiResponse response = new ApiResponse<>(0, null, apiExternalAccount);
        when(apiClient.execute(any(), any()))
                .thenReturn(readAccountsResponse)
                .thenReturn(response);
        when(apiClient.escapeString(anyString())).thenReturn(anyString());
        // WHEN
        underTest.createExternalAccount("cb-altus-access", null, "ALTUS_ACCESS_KEY", new HashMap<>(), apiClient);
        // THEN
        verify(apiClient, times(2)).execute(any(), any());
    }
}
