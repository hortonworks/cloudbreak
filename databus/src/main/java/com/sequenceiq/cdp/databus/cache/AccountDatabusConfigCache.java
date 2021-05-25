package com.sequenceiq.cdp.databus.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

public class AccountDatabusConfigCache {

    private final Map<String, AtomicReference<DataBusCredential>> accountCredentialsCache = new ConcurrentHashMap<>();

    public DataBusCredential getCredential(String accountId) {
        return accountCredentialsCache.get(accountId).get();
    }

    public boolean containsCredentialKey(String accountId) {
        return accountCredentialsCache.containsKey(accountId);
    }

    public void putCredential(String accountId, DataBusCredential dataBusCredential) {
        accountCredentialsCache.put(accountId, new AtomicReference<>(dataBusCredential));
    }

    public void removeCredential(String accountId) {
        accountCredentialsCache.remove(accountId);
    }
}
