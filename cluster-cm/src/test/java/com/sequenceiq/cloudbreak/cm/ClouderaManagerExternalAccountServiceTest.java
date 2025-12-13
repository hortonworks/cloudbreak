package com.sequenceiq.cloudbreak.cm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ExternalAccountsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiExternalAccount;
import com.cloudera.api.swagger.model.ApiExternalAccountList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerExternalAccountServiceTest {

    private static final String ACCOUNT = "cb-altus-access";

    @Mock
    private ApiClient apiClient;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ExternalAccountsResourceApi externalAccountsResourceApi;

    @InjectMocks
    private ClouderaManagerExternalAccountService underTest;

    @Test
    void testCreateExternalAccount() throws ApiException {
        ApiExternalAccountList apiExternalAccountList = new ApiExternalAccountList();
        when(clouderaManagerApiFactory.getExternalAccountsResourceApi(apiClient)).thenReturn(externalAccountsResourceApi);
        when(externalAccountsResourceApi.readAccounts(anyString(), anyString())).thenReturn(apiExternalAccountList);

        underTest.createExternalAccount(ACCOUNT, null, "ALTUS_ACCESS_KEY", new HashMap<>(), apiClient);

        verify(externalAccountsResourceApi).readAccounts(anyString(), anyString());
        verify(externalAccountsResourceApi).createAccount(any());
        verify(externalAccountsResourceApi, never()).updateAccount(any());
    }

    @Test
    void testUpdateExternalAccount() throws ApiException {
        ApiExternalAccountList apiExternalAccountList = new ApiExternalAccountList();
        ApiExternalAccount item = new ApiExternalAccount();
        item.setName(ACCOUNT);
        apiExternalAccountList.addItemsItem(item);
        when(clouderaManagerApiFactory.getExternalAccountsResourceApi(apiClient)).thenReturn(externalAccountsResourceApi);
        when(externalAccountsResourceApi.readAccounts(anyString(), anyString())).thenReturn(apiExternalAccountList);

        underTest.createExternalAccount(ACCOUNT, null, "ALTUS_ACCESS_KEY", new HashMap<>(), apiClient);

        verify(externalAccountsResourceApi, times(1)).readAccounts(anyString(), anyString());
        verify(externalAccountsResourceApi).updateAccount(any());
        verify(externalAccountsResourceApi, never()).createAccount(any());
    }
}
