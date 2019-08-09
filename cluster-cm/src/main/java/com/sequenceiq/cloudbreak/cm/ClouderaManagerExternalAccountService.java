package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.util.ConfigUtils.makeApiConfigList;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ExternalAccountsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiExternalAccount;
import com.cloudera.api.swagger.model.ApiExternalAccountList;

@Service
public class ClouderaManagerExternalAccountService {

    public ApiExternalAccount createExternalAccount(String accountName, String displayName, String typeName,
            Map<String, String> configs, ApiClient client) throws ApiException {
        ExternalAccountsResourceApi externalAccountsResourceApi = new ExternalAccountsResourceApi(client);
        ApiExternalAccountList externalAccountList = externalAccountsResourceApi.readAccounts(typeName, "FULL");
        boolean accountFound = false;
        if (externalAccountList.getItems() != null && !externalAccountList.getItems().isEmpty()) {
            for (ApiExternalAccount externalAccount : externalAccountList.getItems()) {
                if (accountName.equals(externalAccount.getName())) {
                    accountFound = true;
                }
            }
        }
        ApiExternalAccount apiExternalAccount = new ApiExternalAccount();
        apiExternalAccount.setName(accountName);
        apiExternalAccount.setDisplayName(displayName);
        apiExternalAccount.setTypeName(typeName);
        ApiConfigList accountConfigList = makeApiConfigList(configs);
        apiExternalAccount.setAccountConfigs(accountConfigList);
        if (!accountFound) {
            externalAccountsResourceApi.createAccount(apiExternalAccount);
        }
        return apiExternalAccount;
    }

}
