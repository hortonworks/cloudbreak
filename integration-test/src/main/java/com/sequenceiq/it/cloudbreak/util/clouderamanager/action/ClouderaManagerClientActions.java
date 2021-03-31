package com.sequenceiq.it.cloudbreak.util.clouderamanager.action;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.client.ClouderaManagerClient;

@Component
public class ClouderaManagerClientActions extends ClouderaManagerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClientActions.class);

    private static final String API_ROOT = "/api";

    private static final String API_V_43 = API_ROOT + "/v43";

    @Value("${integrationtest.cloudProvider:}")
    private String cloudProvider;

    public SdxInternalTestDto checkCmKnoxIDBrokerRoleConfigGroups(SdxInternalTestDto testDto, String user, String password) {
        String serverFqdn = testDto.getResponse().getStackV4Response().getCluster().getServerFqdn();
        ApiClient apiClient = getCmApiClient(serverFqdn, API_V_43, user, password);
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
            LOGGER.error("Exception when calling UsersResourceApi#readUsers2", e);
            throw new TestFailException("Exception when calling UsersResourceApi#readUsers2 at " + apiClient.getBasePath(), e);
        } catch (Exception e) {
            LOGGER.error("Can't get users' list at: '{}'!", apiClient.getBasePath());
            throw new TestFailException("Can't get users' list at: " + apiClient.getBasePath(), e);
        }
        return testDto;
    }
}
