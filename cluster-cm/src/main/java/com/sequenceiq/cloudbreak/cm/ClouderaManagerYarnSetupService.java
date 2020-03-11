package com.sequenceiq.cloudbreak.cm;

import java.util.List;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
public class ClouderaManagerYarnSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerYarnSetupService.class);

    private static final List<String> YARN_ALL_HOSTS_SUPPRESSION_NAMES = List.of("host_memswap_thresholds", "host_clock_offset_thresholds");

    private static final List<String> YARN_SERVICE_ROLE_SUPPRESSION_NAMES = List.of("process_swap_memory_thresholds");

    private static final String YARN_SUPPRESSION_VALUE = "{\"critical\":\"never\",\"warning\":\"never\"}";

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public void suppressWarnings(Stack stack, ApiClient apiClient) {
        try {
            long start = System.currentTimeMillis();

            ApiConfigList allHostsApiConfigList = getSuppressionApiConfigList(YARN_ALL_HOSTS_SUPPRESSION_NAMES);
            clouderaManagerApiFactory.getAllHostsResourceApi(apiClient)
                    .updateConfig("Suppress Yarn warnings for all hosts", allHostsApiConfigList);

            String clusterName = stack.getCluster().getName();
            ApiConfigList serviceRoleApiConfigList = getSuppressionApiConfigList(YARN_SERVICE_ROLE_SUPPRESSION_NAMES);
            RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(apiClient);
            ApiServiceList apiServiceList = clouderaManagerApiFactory.getServicesResourceApi(apiClient)
                    .readServices(clusterName, null);
            for (ApiService apiService : apiServiceList.getItems()) {
                String apiServiceName = apiService.getName();
                ApiRoleList apiRoleList = rolesResourceApi.readRoles(clusterName, apiServiceName, null, null);
                for (ApiRole apiRole : apiRoleList.getItems()) {
                    try {
                        String apiRoleName = apiRole.getName();
                        rolesResourceApi.updateRoleConfig(
                                clusterName,
                                apiRoleName,
                                apiServiceName,
                                String.format("Suppress Yarn warnings for service %s and role %s", apiServiceName, apiRoleName),
                                serviceRoleApiConfigList);
                    } catch (ApiException e) {
                        // some service roles are not supporting the suppression configurations and respond with bad request
                        if (e.getCode() != HttpStatus.SC_BAD_REQUEST) {
                            throw e;
                        }
                    }
                }
            }
            LOGGER.info("Suppressed Yarn warnings in {} ms", System.currentTimeMillis() - start);
        } catch (ApiException e) {
            LOGGER.info("Failed to suppress Yarn warnings.", e);
        }
    }

    private ApiConfigList getSuppressionApiConfigList(List<String> suppressionNames) {
        ApiConfigList apiConfigList = new ApiConfigList();
        suppressionNames.forEach(suppression -> {
            ApiConfig apiConfig = new ApiConfig();
            apiConfig.setName(suppression);
            apiConfig.setValue(YARN_SUPPRESSION_VALUE);
            apiConfig.setSensitive(false);

            apiConfigList.addItemsItem(apiConfig);
        });
        return apiConfigList;
    }
}
