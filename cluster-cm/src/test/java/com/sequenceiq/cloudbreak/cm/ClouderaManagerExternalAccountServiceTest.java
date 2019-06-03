package com.sequenceiq.cloudbreak.cm;

import static org.mockito.ArgumentMatchers.any;
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
        ApiResponse response = new ApiResponse<>(0, null, apiExternalAccount);
        when(apiClient.execute(any(), any())).thenReturn(response);
        // WHEN
        underTest.createExternalAccount(null, null, null, new HashMap<>(), apiClient);
        // THEN
        verify(apiClient, times(1)).execute(any(), any());
    }
}
