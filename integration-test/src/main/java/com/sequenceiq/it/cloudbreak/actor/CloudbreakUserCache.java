package com.sequenceiq.it.cloudbreak.actor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class CloudbreakUserCache {

    private static volatile CloudbreakUserCache instance;

    private static Object mutex = new Object();

    private Map<String, List<CloudbreakUser>> usersByAccount;

    private CloudbreakUserCache() {
    }

    public static CloudbreakUserCache getInstance() {
        CloudbreakUserCache result = instance;
        if (result == null) {
            synchronized (mutex) {
                result = instance;
                if (result == null) {
                    result = new CloudbreakUserCache();
                    instance = result;
                }
            }
        }
        return result;
    }

    public CloudbreakUser getByName(String name) {
        if (usersByAccount == null) {
            initUsers();
        }
        return usersByAccount.values().stream().flatMap(Collection::stream)
                .filter(u -> u.getDisplayName().equals(name)).findFirst().get();
    }

    public void initUsers() {
        String userConfigPath = "ums-users/api-credentials.json";
        try {
            this.usersByAccount = JsonUtil.readValue(
                    FileReaderUtils.readFileFromClasspathQuietly(userConfigPath), new TypeReference<>() {
                    });
        } catch (IOException e) {
            throw new RuntimeException(String.format("Can't read file: %s It's possible you did run make fetch-secrets", userConfigPath));
        }
        usersByAccount.values().stream().flatMap(Collection::stream).forEach(u -> CloudbreakUser.validateRealUmsUser(u));
    }

    public String getAdminAccessKeyByAccountId(String accountId) {
        return usersByAccount.get(accountId).stream().filter(CloudbreakUser::getAdmin).findFirst()
                .orElseThrow(() -> new TestFailException(String.format("There is no account admin test user for account %s", accountId))).getAccessKey();
    }

    public boolean isInitialized() {
        return usersByAccount != null;
    }
}
