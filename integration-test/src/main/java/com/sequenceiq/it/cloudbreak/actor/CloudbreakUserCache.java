package com.sequenceiq.it.cloudbreak.actor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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

    @Value("${integrationtest.ums.jsonSecret.destinationPath:}")
    private String fetchedFilePath;

    @Value("${integrationtest.outputdir:.}")
    private String workspace;

    @PostConstruct
    private void initRealUmsUserCache() {
        UmsUserStoreConfig umsUserStore = findUmsStore();
        if (umsUserStore.getFilePresent()) {
            Map<String, Map<String, List<CloudbreakUser>>> fetchedUserStore;
            List<CloudbreakUser> cloudbreakUsers;
            Set<String> accountIds = new HashSet<>();
            String accountId;

            LOGGER.info("Real UMS users are initializing by deployment: {} and account: {}.", realUmsUserDeployment, realUmsUserAccount);
            try {
                if (umsUserStore.getAtCustomPath()) {
                    fetchedUserStore = JsonUtil.readValue(
                            FileReaderUtils.readFileFromCustomPath(umsUserStore.getCustomFilePath()),
                            new TypeReference<Map<String, Map<String, List<CloudbreakUser>>>>() { });
                } else {
                    fetchedUserStore = JsonUtil.readValue(
                            FileReaderUtils.readFileFromClasspathQuietly(umsUserStore.getClassFilePath()),
                            new TypeReference<Map<String, Map<String, List<CloudbreakUser>>>>() { });
                }

                cloudbreakUsers = fetchedUserStore.entrySet().stream()
                        .filter(mowEnvs -> mowEnvs.getKey().equalsIgnoreCase(realUmsUserDeployment))
                        .flatMap(selectedEnv -> selectedEnv.getValue().entrySet().stream()
                                .filter(mowAccs -> mowAccs.getKey().equalsIgnoreCase(realUmsUserAccount))
                                .flatMap(selectedAcc -> selectedAcc.getValue().stream())).collect(Collectors.toList());
                cloudbreakUsers.forEach(user -> accountIds.add(Objects.requireNonNull(Crn.fromString(user.getCrn())).getAccountId()));
            } catch (Exception e) {
                throw new TestFailException(" Can't read UMS user store! It's possible you did run 'make fetch-secrets'. ", e);
            }
            if (CollectionUtils.isEmpty(accountIds)) {
                LOGGER.error(" Cannot gather account Ids from the initialized real UMS user CRNs. ");
                throw new TestFailException("Cannot gather account Ids from the initialized real UMS user CRNs.");
            } else {
                LOGGER.info(" Gathered account Ids based on the initialized real UMS user CRNs:: {}", accountIds);
                accountId = accountIds.stream().findFirst().orElseThrow(() -> new TestFailException(String.format("Account Id Not Found in:: %s",
                        accountIds)));
                setUsersByAccount(Map.of(accountId, cloudbreakUsers));
            }
            usersByAccount.values().stream().flatMap(Collection::stream).forEach(user -> {
                LOGGER.info(" Initialized real UMS user in account ({}):: \nDisplay name: {} \nCrn: {} \nAccess key: {} \nSecret key: {} \nAdmin: {} ",
                        accountId, user.getDisplayName(), user.getCrn(), user.getAccessKey(), user.getSecretKey(), user.getAdmin());
                CloudbreakUser.validateRealUmsUser(user);
            });
        } else {
            LOGGER.info("UMS user store is not available at path: '{}'. So initialization of real UMS user cache is not possible!",
                    umsUserStore.getFetchedFilePath());
        }
    }

    private UmsUserStoreConfig findUmsStore() {
        UmsUserStoreConfig umsUserStoreConfig = new UmsUserStoreConfig();
        umsUserStoreConfig.setWorkspace(workspace);
        umsUserStoreConfig.setFetchedFilePath(fetchedFilePath);
        umsUserStoreConfig.setAtCustomPath(false);
        umsUserStoreConfig.setFilePresent(false);
        if (StringUtils.isEmpty(umsUserStoreConfig.getFetchedFilePath())) {
            LOGGER.info("UMS user store json file destination path has not been set via 'integrationtest.ums.jsonSecret.destinationPath'!");
        } else {
            String[] credentialsFilePathElements = StringUtils.split(umsUserStoreConfig.getFetchedFilePath(), "/");
            String credentialsFile = StringUtils.substringAfterLast(umsUserStoreConfig.getFetchedFilePath(), "/");
            String credentialsFolder = credentialsFilePathElements[credentialsFilePathElements.length - 2];
            umsUserStoreConfig.setClassFilePath(StringUtils.join(List.of(credentialsFolder, credentialsFile), "/"));
            try {
                Stream<Path> foundFiles = Files.find(Paths.get(umsUserStoreConfig.getWorkspace()), Integer.MAX_VALUE,
                        (filePath, fileAttr) -> filePath.toString().endsWith(credentialsFile));
                umsUserStoreConfig.setCustomFilePath(foundFiles
                        .filter(path -> path.getFileName().toString().equalsIgnoreCase(credentialsFile))
                        .findFirst().get());
                if (Files.exists(umsUserStoreConfig.getCustomFilePath())) {
                    LOGGER.info("Found UMS user store file at custom path: '{}'", umsUserStoreConfig.getCustomFilePath().toAbsolutePath().normalize());
                    umsUserStoreConfig.setAtCustomPath(true);
                    umsUserStoreConfig.setFilePresent(true);
                }
            } catch (NoSuchElementException e) {
                if (new ClassPathResource(umsUserStoreConfig.getClassFilePath()).exists()) {
                    LOGGER.info("Found UMS user store file at classpath: '{}'", umsUserStoreConfig.getClassFilePath());
                    umsUserStoreConfig.setFilePresent(true);
                } else {
                    LOGGER.info("Cannot find UMS user store file neither at custom path: '{}', nor at classpath: '{}'!",
                            Paths.get(umsUserStoreConfig.getWorkspace()).toAbsolutePath().normalize(), umsUserStoreConfig.getClassFilePath());
                }
            } catch (Exception e) {
                LOGGER.info("Cannot find UMS user store file at path: '{}', because of: {}", Paths.get(umsUserStoreConfig.getWorkspace()).toAbsolutePath()
                                .normalize(), e.getMessage(), e);
            }
        }
        return umsUserStoreConfig;
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