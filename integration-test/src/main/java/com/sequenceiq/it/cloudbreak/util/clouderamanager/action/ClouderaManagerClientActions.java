package com.sequenceiq.it.cloudbreak.util.clouderamanager.action;

import static java.lang.String.format;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.client.ClouderaManagerClient;

@Component
public class ClouderaManagerClientActions extends ClouderaManagerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClientActions.class);

    private static final String V_43 = "/v43";

    @Value("${integrationtest.cloudProvider:}")
    private String cloudProvider;

    public SdxInternalTestDto checkCmKnoxIDBrokerRoleConfigGroups(SdxInternalTestDto testDto, String user, String password) {
        String serverFqdn = testDto.getResponse().getStackV4Response().getCluster().getServerFqdn();
        ApiClient apiClient = getCmApiClient(serverFqdn, testDto.getName(), V_43, user, password);
        // CHECKSTYLE:OFF
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = new RoleConfigGroupsResourceApi(apiClient);
        // CHECKSTYLE:ON
        try {
            ApiConfigList knoxConfigs = roleConfigGroupsResourceApi.readConfig(testDto.getName(), "knox-IDBROKER-BASE",
                    "knox", "full");
            knoxConfigs.getItems().stream()
                    .forEach(knoxConfig -> {
                        String knoxConfigName = knoxConfig.getName();
                        String mappingsFromKnoxConfig = knoxConfig.getValue();
                        if (String.join("_", "idbroker", cloudProvider, "group", "mapping").equalsIgnoreCase(knoxConfigName)) {
                            if (!mappingsFromKnoxConfig.contains("_c_cm_admins_")) {
                                LOGGER.error("{} does not contains the expected 'CM Admins' mapping!", knoxConfigName);
                                throw new TestFailException(String.format("%s does not contains the expected 'CM Admins' mapping!", knoxConfigName));
                            } else {
                                Log.log(LOGGER, format(" '%s' contains the expected '%s' mapping. ", knoxConfigName, mappingsFromKnoxConfig));
                            }
                        } else if (String.join("_", "idbroker", cloudProvider, "user", "mapping").equalsIgnoreCase(knoxConfigName)) {
                            if (!mappingsFromKnoxConfig.contains("hive")) {
                                LOGGER.error("{} does not contains the expected 'Hive' mapping!", knoxConfigName);
                                throw new TestFailException(String.format("%s does not contains the expected 'Hive' mapping!", knoxConfigName));
                            } else {
                                Log.log(LOGGER, format(" '%s' contains the expected ['%s'] mappings. ", knoxConfigName, mappingsFromKnoxConfig));
                            }
                        }
                    });
            if (knoxConfigs.getItems().isEmpty()) {
                LOGGER.error("IDBroker mappings are NOT exist!");
                throw new TestFailException("IDBroker mappings are NOT exist!");
            }
        } catch (ApiException e) {
            LOGGER.error("Exception when calling RoleConfigGroupsResourceApi#readConfig. Response: {}", e.getResponseBody(), e);
            String message = format("Exception when calling RoleConfigGroupsResourceApi#readConfig at %s. Response: %s",
                    apiClient.getBasePath(), e.getResponseBody());
            throw new TestFailException(message, e);
        } catch (Exception e) {
            LOGGER.error("Can't get users' list at: '{}'!", apiClient.getBasePath());
            throw new TestFailException("Can't get users' list at: " + apiClient.getBasePath(), e);
        }
        return testDto;
    }

    public DistroXTestDto checkCmYarnNodemanagerRoleConfigGroups(DistroXTestDto testDto, String user, String password) {
        String serverIp = testDto.getResponse().getCluster().getServerIp();
        ApiClient apiClient = getCmApiClientWithTimeoutDisabled(serverIp, testDto.getName(), V_43, user, password);
        return checkCmYarnNodemanagerRoleConfigGroups(apiClient, testDto, user, password);
    }

    public DistroXTestDto checkCmHdfsNamenodeRoleConfigGroups(DistroXTestDto testDto, String user, String password, Set<String> mountPoints) {
        String serverIp = testDto.getResponse().getCluster().getServerIp();
        ApiClient apiClient = getCmApiClientWithTimeoutDisabled(serverIp, testDto.getName(), V_43, user, password);
        // CHECKSTYLE:OFF
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = new RoleConfigGroupsResourceApi(apiClient);
        // CHECKSTYLE:ON
        try {
            ApiConfigList hdfsConfigs = roleConfigGroupsResourceApi.readConfig(testDto.getName(), "hdfs-NAMENODE-BASE",
                    "hdfs", "summary");
            hdfsConfigs.getItems()
                    .forEach(config -> {
                        String hdfsConfigName = config.getName();
                        String mappingsFromHdfsConfig = config.getValue();
                        if ("dfs_name_dir_list".equalsIgnoreCase(hdfsConfigName)) {
                            if (mountPoints.stream().anyMatch(mappingsFromHdfsConfig::startsWith)) {
                                LOGGER.error("{} contains ephemeral volume mapping '{}'!", hdfsConfigName, mappingsFromHdfsConfig);
                                throw new TestFailException(String.format("%s contains ephemeral volume mapping '%s'!", hdfsConfigName,
                                        mappingsFromHdfsConfig));
                            } else {
                                Log.log(LOGGER, format(" '%s' does not contain the ephemeral mapping '%s', as expected. ", hdfsConfigName,
                                        mappingsFromHdfsConfig));
                            }
                        }
                    });
            if (hdfsConfigs.getItems().isEmpty()) {
                LOGGER.error("Namenode mappings are NOT exist!");
                throw new TestFailException("Namenode mappings are NOT exist!");
            }
        } catch (ApiException e) {
            LOGGER.error("Exception when calling RoleConfigGroupsResourceApi#readConfig. Response: {}", e.getResponseBody(), e);
            String message = format("Exception when calling RoleConfigGroupsResourceApi#readConfig at %s. Response: %s",
                    apiClient.getBasePath(), e.getResponseBody());
            throw new TestFailException(message, e);
        } catch (Exception e) {
            LOGGER.error("Can't read config at: '{}'!", apiClient.getBasePath());
            throw new TestFailException("Can't read config at: " + apiClient.getBasePath(), e);
        }
        return testDto;
    }

    public DistroXTestDto checkCmHdfsDatanodeRoleConfigGroups(DistroXTestDto testDto, String user, String password, Set<String> mountPoints) {
        String serverIp = testDto.getResponse().getCluster().getServerIp();
        ApiClient apiClient = getCmApiClientWithTimeoutDisabled(serverIp, testDto.getName(), V_43, user, password);
        // CHECKSTYLE:OFF
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = new RoleConfigGroupsResourceApi(apiClient);
        // CHECKSTYLE:ON
        try {
            ApiConfigList hdfsConfigs = roleConfigGroupsResourceApi.readConfig(testDto.getName(), "hdfs-DATANODE-BASE",
                    "hdfs", "summary");
            hdfsConfigs.getItems()
                    .forEach(config -> {
                        String hdfsConfigName = config.getName();
                        String mappingsFromHdfsConfig = config.getValue();
                        if ("dfs_data_dir_list".equalsIgnoreCase(hdfsConfigName)) {
                            if (mountPoints.stream().anyMatch(mappingsFromHdfsConfig::startsWith)) {
                                LOGGER.error("{} contains ephemeral volume mapping '{}'!", hdfsConfigName, mappingsFromHdfsConfig);
                                throw new TestFailException(String.format("%s contains ephemeral volume mapping '%s'!", hdfsConfigName,
                                        mappingsFromHdfsConfig));
                            } else {
                                Log.log(LOGGER, format(" '%s' does not contain the ephemeral mapping '%s', as expected. ", hdfsConfigName,
                                        mappingsFromHdfsConfig));
                            }
                        }
                    });
            if (hdfsConfigs.getItems().isEmpty()) {
                LOGGER.error("Datanode mappings are NOT exist!");
                throw new TestFailException("Datanode mappings are NOT exist!");
            }
        } catch (ApiException e) {
            LOGGER.error("Exception when calling RoleConfigGroupsResourceApi#readConfig. Response: {}", e.getResponseBody(), e);
            String message = format("Exception when calling RoleConfigGroupsResourceApi#readConfig at %s. Response: %s",
                    apiClient.getBasePath(), e.getResponseBody());
            throw new TestFailException(message, e);
        } catch (Exception e) {
            LOGGER.error("Can't read config at: '{}'!", apiClient.getBasePath());
            throw new TestFailException("Can't read config at: " + apiClient.getBasePath(), e);
        }
        return testDto;
    }

    public DistroXTestDto checkCmYarnNodemanagerRoleConfigGroupsDirect(DistroXTestDto testDto, String user, String password) {
        String serverIp = testDto.getResponse().getCluster().getServerIp();
        ApiClient apiClient = getCmApiClientWithTimeoutDisabledDirect(serverIp, testDto.getName(), V_43, user, password);
        return checkCmYarnNodemanagerRoleConfigGroups(apiClient, testDto, user, password);
    }

    private DistroXTestDto checkCmYarnNodemanagerRoleConfigGroups(ApiClient apiClient, DistroXTestDto testDto, String user, String password) {
        // CHECKSTYLE:OFF
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = new RoleConfigGroupsResourceApi(apiClient);
        // CHECKSTYLE:ON
        try {
            ApiConfigList knoxConfigs = roleConfigGroupsResourceApi.readConfig(testDto.getName(), "yarn-NODEMANAGER-BASE",
                    "yarn", "full");
            knoxConfigs.getItems().stream()
                    .forEach(knoxConfig -> {
                        String knoxConfigName = knoxConfig.getName();
                        String mappingsFromKnoxConfig = knoxConfig.getValue();
                        if ("yarn_nodemanager_local_dirs".equalsIgnoreCase(knoxConfigName)) {
                            if (!mappingsFromKnoxConfig.startsWith("/hadoopfs/ephfs")) {
                                LOGGER.error("{} does not contains the expected '/hadoopfs/ephfs...' mapping!", knoxConfigName);
                                throw new TestFailException(String.format("%s does not contains the expected '/hadoopfs/ephfs...' mapping!", knoxConfigName));
                            } else {
                                Log.log(LOGGER, format(" '%s' contains the expected '%s' mapping. ", knoxConfigName, mappingsFromKnoxConfig));
                            }
                        }
                    });
            if (knoxConfigs.getItems().isEmpty()) {
                LOGGER.error("Nodemanager mappings are NOT exist!");
                throw new TestFailException("Nodemanager mappings are NOT exist!");
            }
        } catch (ApiException e) {
            LOGGER.error("Exception when calling RoleConfigGroupsResourceApi#readConfig. Response: {}", e.getResponseBody(), e);
            String message = format("Exception when calling RoleConfigGroupsResourceApi#readConfig at %s. Response: %s",
                    apiClient.getBasePath(), e.getResponseBody());
            throw new TestFailException(message, e);
        } catch (Exception e) {
            LOGGER.error("Can't get role configs at: '{}'!", apiClient.getBasePath());
            throw new TestFailException("Can't get role configs at: " + apiClient.getBasePath(), e);
        }
        return testDto;
    }
}
