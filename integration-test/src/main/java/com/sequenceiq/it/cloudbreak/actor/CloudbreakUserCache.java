package com.sequenceiq.it.cloudbreak.actor;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class CloudbreakUserCache {

    private static volatile CloudbreakUserCache instance;

    private static Object mutex = new Object();

    private List<CloudbreakUser> users;

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
        if (users == null) {
            initUsers();
        }
        return users.stream().filter(u -> u.getDisplayName().equals(name)).findFirst().get();
    }

    public void initUsers() {
        String userConfigPath = "ums-users/api-credentials.json";
        try {
            this.users = JsonUtil.readValue(
                    FileReaderUtils.readFileFromClasspathQuietly(userConfigPath), new TypeReference<>() {
                    });
        } catch (IOException e) {
            throw new RuntimeException(String.format("Can't read file: %s It's possible you did run make fetch-secrets", userConfigPath));
        }
        users.stream().forEach(u -> CloudbreakUser.validateRealUmsUser(u));
    }
}
