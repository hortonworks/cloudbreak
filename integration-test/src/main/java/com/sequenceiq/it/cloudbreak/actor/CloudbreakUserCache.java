package com.sequenceiq.it.cloudbreak.actor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class CloudbreakUserCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUserCache.class);

    private Map<String, List<CloudbreakUser>> usersByAccount;

    private Set<String> accountIds;

    @Value("${integrationtest.ums.accountKey:}")
    private String realUmsUserAccount;

    @Value("${integrationtest.ums.deploymentKey:}")
    private String realUmsUserDeployment;

    @PostConstruct
    private void initRealUmsUserCache() {
        String userConfigPath;
        String authUserConfigPath = "ums-users/api-credentials.json";
        String l0UserConfigPath = "ums-users/l0-api-credentials.json";
        Map<String, Map<String, List<CloudbreakUser>>> fetchedUserStore;
        List<CloudbreakUser> cloudbreakUsers;
        Set<String> accountIds = new HashSet<>();
        String accountId;

        if (new ClassPathResource(l0UserConfigPath).exists()) {
            userConfigPath = l0UserConfigPath;
        } else {
            userConfigPath = authUserConfigPath;
        }

        if (new ClassPathResource(userConfigPath).exists()) {
            LOGGER.info("Real UMS users are initializing by deployment: {} and account: {}. User store is present at: {} path", realUmsUserDeployment,
                    realUmsUserAccount, userConfigPath);
            try {
                fetchedUserStore = JsonUtil.readValue(
                        FileReaderUtils.readFileFromClasspathQuietly(userConfigPath), new TypeReference<Map<String, Map<String, List<CloudbreakUser>>>>() { });
                cloudbreakUsers = fetchedUserStore.entrySet().stream()
                        .filter(mowEnvs -> mowEnvs.getKey().equalsIgnoreCase(realUmsUserDeployment))
                        .flatMap(selectedEnv -> selectedEnv.getValue().entrySet().stream()
                                .filter(mowAccs -> mowAccs.getKey().equalsIgnoreCase(realUmsUserAccount))
                                .flatMap(selectedAcc -> selectedAcc.getValue().stream())).collect(Collectors.toList());
                cloudbreakUsers.forEach(user -> accountIds.add(Objects.requireNonNull(Crn.fromString(user.getCrn())).getAccountId()));
            } catch (Exception e) {
                throw new TestFailException(String.format(" Can't read UMS user store: %s It's possible you did run 'make fetch-secrets'",
                        userConfigPath), e);
            }
            if (CollectionUtils.isEmpty(accountIds)) {
                LOGGER.error(" Cannot gather account Ids from the initialized real UMS user CRNs. ");
                throw new TestFailException("Cannot gather account Ids from the initialized real UMS user CRNs.");
            } else {
                LOGGER.info(" Gathered account Ids based on the initialized real UMS user CRNs:: {}", accountIds);
                accountId = accountIds.stream().findFirst().orElseThrow(() -> new TestFailException(String.format("Account Id Not Found in:: %s", accountIds)));
                setUsersByAccount(Map.of(accountId, cloudbreakUsers));
            }
            usersByAccount.values().stream().flatMap(Collection::stream).forEach(user -> {
                LOGGER.info(" Initialized real UMS user in account ({}):: \nDisplay name: {} \nCrn: {} \nAccess key: {} \nSecret key: {} \nAdmin: {} ",
                        accountId, user.getDisplayName(), user.getCrn(), user.getAccessKey(), user.getSecretKey(), user.getAdmin());
                CloudbreakUser.validateRealUmsUser(user);
            });
        } else {
            LOGGER.info("UMS user store [{}] is not available. So initialization of real UMS user cache is not possible!", userConfigPath);
        }
    }

    private void setUsersByAccount(Map<String, List<CloudbreakUser>> users) {
        this.usersByAccount = users;
    }

    public CloudbreakUser getUserByDisplayName(String name) {
        if (MapUtils.isEmpty(usersByAccount)) {
            initRealUmsUserCache();
        }
        if (isInitialized()) {
            CloudbreakUser user = usersByAccount.values().stream().flatMap(Collection::stream)
                    .filter(u -> u.getDisplayName().equals(name)).findFirst()
                    .orElseThrow(() -> new TestFailException(String.format("There is no real UMS user with::%n name: %s%n deployment: %s%n account: %s%n",
                            name, realUmsUserDeployment, realUmsUserAccount)));
            LOGGER.info(" Real UMS user has been found:: \nDisplay name: {} \nCrn: {} \nAccess key: {} \nSecret key: {} \nAdmin: {} ",
                    user.getDisplayName(), user.getCrn(), user.getAccessKey(), user.getSecretKey(), user.getAdmin());
            return user;
        } else {
            throw new TestFailException("Cannot get real UMS user by name, because of 'ums-users/api-credentials.json' is not available.");
        }
    }

    public CloudbreakUser getAdminByAccountId(String accountId) {
        if (!usersByAccount.containsKey(accountId)) {
            throw new TestFailException("Real UMS '" + accountId + "' account ID is missing from 'ums-users/api-credentials.json' file." +
                    " Please revise your account ID.");
        } else {
            LOGGER.info("Getting the requested real UMS admin by account Id: {}", accountId);
            try {
                CloudbreakUser adminUser = usersByAccount.values().stream().flatMap(Collection::stream)
                        .filter(CloudbreakUser::getAdmin).findFirst()
                        .orElseThrow(() -> new TestFailException(String.format("There is no real UMS admin in account: %s", accountId)));
                LOGGER.info(" Real UMS account admin has been found:: \nDisplay name: {} \nCrn: {} \nAccess key: {} \nSecret key: {} \nAdmin: {} ",
                        adminUser.getDisplayName(), adminUser.getCrn(), adminUser.getAccessKey(), adminUser.getSecretKey(), adminUser.getAdmin());
                return adminUser;
            } catch (Exception e) {
                throw new TestFailException(String.format("Cannot get the real UMS admin in account: %s, because of: %s", accountId, e.getMessage()), e);
            }
        }
    }

    public boolean isInitialized() {
        return MapUtils.isNotEmpty(usersByAccount);
    }
}