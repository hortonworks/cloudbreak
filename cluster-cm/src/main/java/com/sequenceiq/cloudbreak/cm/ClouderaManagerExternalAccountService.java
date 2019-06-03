package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.util.ConfigUtils.makeApiConfigList;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ExternalAccountsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiExternalAccount;

@Service
public class ClouderaManagerExternalAccountService {

    public ApiExternalAccount createExternalAccount(String accountName, String displayName, String typeName,
            Map<String, String> configs, ApiClient client) throws ApiException {
        ExternalAccountsResourceApi externalAccountsResourceApi = new ExternalAccountsResourceApi(client);
        ApiExternalAccount apiExternalAccount = new ApiExternalAccount();
        apiExternalAccount.setName(accountName);
        apiExternalAccount.setDisplayName(displayName);
        apiExternalAccount.setTypeName(typeName);
        ApiConfigList accountConfigList = makeApiConfigList(configs);
        apiExternalAccount.setAccountConfigs(accountConfigList);
        externalAccountsResourceApi.createAccount(apiExternalAccount);
        return apiExternalAccount;
    }

}
