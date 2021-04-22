package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.util.ConfigUtils.makeApiConfig;
import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.MgmtRolesResourceApi;
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.config.CmConfigService;
import com.sequenceiq.cloudbreak.cm.database.DatabaseProperties;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

/**
 * Sets up management services for the given Cloudera Manager server.
 */
@Service
public class ClouderaManagerMgmtSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerMgmtSetupService.class);

    private static final String MGMT_SERVICE = "MGMT";

    private static final List<String> BLACKLISTED_ROLE_TYPES = ImmutableList.of(
            ClouderaManagerMgmtTelemetryService.TELEMETRYPUBLISHER,
            "NAVIGATOR",
            "NAVIGATORMETASERVER",
            "ACTIVITYMONITOR",
            "REPORTSMANAGER");

    private static final String GENERATE_CREDENTIALS_COMMAND_NAME = "GenerateCredentials";

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private ClouderaManagerLicenseService licenseService;

    @Inject
    private ClouderaManagerMgmtTelemetryService telemetryService;

    @Inject
    private DatabaseCommon databaseCommon;

    @Inject
    private CmConfigService cmConfigService;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    /**
     * Sets up the management services using the given Cloudera Manager apiClient.
     *
     * @param stack      the stack
     * @param apiClient     the CM API apiClient
     * @param cmHostRef  reference to the CM host
     * @param rdsConfigs the set of all database configs
     * @param telemetry  telemetry (logging/workload/billing etc.) details
     * @param sdxContextName sdx name holder
     * @param sdxStackCrn sdx stack crn holder
     * @param proxyConfig ccm proxy configuration holder
     * @throws ApiException if there's a problem setting up management services
     */
    public void setupMgmtServices(Stack stack, ApiClient apiClient, ApiHostRef cmHostRef, Telemetry telemetry,
            String sdxContextName, String sdxStackCrn, ProxyConfig proxyConfig)
            throws ApiException {
        LOGGER.debug("Setting up Cloudera Management Services.");
        licenseService.validateClouderaManagerLicense(stack.getCreator());
        MgmtServiceResourceApi mgmtServiceResourceApi = clouderaManagerApiFactory.getMgmtServiceResourceApi(apiClient);
        MgmtRolesResourceApi mgmtRolesResourceApi = clouderaManagerApiFactory.getMgmtRolesResourceApi(apiClient);

        ApiService mgmtService = new ApiService();
        mgmtService.setName(MGMT_SERVICE);
        mgmtService.setType(MGMT_SERVICE);

        setupCMS(mgmtServiceResourceApi, mgmtService);

        ApiRoleList mgmtRoles = new ApiRoleList();
        List<String> roleTypes = mgmtServiceResourceApi.listRoleTypes().getItems();
        for (String roleType : roleTypes) {
            if (!BLACKLISTED_ROLE_TYPES.contains(roleType)) {
                LOGGER.debug("Role type {} is not on black list. Adding it to management roles for host {}.", roleType, cmHostRef.getHostname());
                ApiRole apiRole = new ApiRole();
                apiRole.setName(roleType);
                apiRole.setType(roleType);
                apiRole.setHostRef(cmHostRef);
                mgmtRoles.addItemsItem(apiRole);
            }
        }
        cmConfigService.setConfigs(stack, mgmtRoles);
        telemetryService.setupTelemetryRole(stack, apiClient, cmHostRef, mgmtRoles, telemetry);
        createMgmtRoles(mgmtRolesResourceApi, mgmtRoles);
        telemetryService.updateTelemetryConfigs(stack, apiClient, telemetry, sdxContextName, sdxStackCrn, proxyConfig);
        waitForGenerateCredentialsToFinish(stack, apiClient);
        setUpAutoConfiguration(mgmtServiceResourceApi);
    }

    private void waitForGenerateCredentialsToFinish(Stack stack, ApiClient apiClient) throws ApiException {
        LOGGER.debug("Wait if Generate Credentials command is still active.");
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
        ApiCommandList apiCommandList = clouderaManagerResourceApi.listActiveCommands(DataView.SUMMARY.name());
        Optional<BigDecimal> generateCredentialsCommandId = apiCommandList.getItems().stream()
                .filter(toGenerateCredentialsCommand()).map(ApiCommand::getId).findFirst();
        generateCredentialsCommandId.ifPresent(pollCredentialGeneration(stack, apiClient));
    }

    private Consumer<BigDecimal> pollCredentialGeneration(Stack stack, ApiClient client) {
        return id -> {
            LOGGER.debug("Generate Credentials command is still active.");
            clouderaManagerPollingServiceProvider.startPollingCmGenerateCredentials(stack, client, id);
        };
    }

    private Predicate<ApiCommand> toGenerateCredentialsCommand() {
        return apiCommand -> GENERATE_CREDENTIALS_COMMAND_NAME.equals(apiCommand.getName());
    }

    private void createMgmtRoles(MgmtRolesResourceApi mgmtRolesResourceApi, ApiRoleList mgmtRoles) throws ApiException {
        LOGGER.debug("Creating management roles.");
        try {
            mgmtRolesResourceApi.createRoles(mgmtRoles);
        } catch (ApiException ex) {
            if (ex.getResponseBody() == null || !ex.getResponseBody().contains("The maximum number of instances")) {
                throw ex;
            }
        }
    }

    private void setUpAutoConfiguration(MgmtServiceResourceApi mgmtServiceResourceApi) throws ApiException {
        LOGGER.debug("Setting up auto configuration for management services.");
        mgmtServiceResourceApi.autoConfigure();
        LOGGER.debug("Management service auto configuration finished.");
    }

    private void setupCMS(MgmtServiceResourceApi mgmtServiceResourceApi, ApiService mgmtService) throws ApiException {
        try {
            mgmtServiceResourceApi.setupCMS(mgmtService);
        } catch (ApiException ex) {
            if (!ex.getResponseBody().contains("CMS instance already exists.")) {
                throw ex;
            }
        }
    }

    /**
     * Generate the default internal name of the mgmt RCGs as defined by CM.
     */
    @VisibleForTesting
    String getBaseRoleConfigGroupName(String configuration) {
        requireNonNull(configuration);
        return Joiner.on("-").join("MGMT", configuration, "BASE");
    }

    /**
     * Builds an API config list given the database configuration. The prefix is added onto
     * the beginning of the database config names to get the accurate database configuration key
     * to assign a value to.
     *
     * @param prefix    the prefix to use to generate database config keys
     * @param rdsConfig the database configuration
     * @return an API config list
     */
    @VisibleForTesting
    ApiConfigList buildApiConfigList(String prefix, RDSConfig rdsConfig) {
        String connectionUrl = rdsConfig.getConnectionURL();

        // We can safely retrieve these values since they should have passed validation
        DatabaseCommon.JdbcConnectionUrlFields connectionUrlFields = databaseCommon.parseJdbcConnectionUrl(connectionUrl);
        String type = connectionUrlFields.getVendorDriverId();
        String hostAndPort = connectionUrlFields.getHostAndPort();
        String databaseName = connectionUrlFields.getDatabase().get();

        ApiConfigList result = new ApiConfigList();
        result.addItemsItem(makeApiConfig(getDatabaseApiConfigName(prefix, DatabaseProperties.TYPE_PROPERTY),
                type));
        result.addItemsItem(makeApiConfig(getDatabaseApiConfigName(prefix, DatabaseProperties.HOST_PROPERTY),
                hostAndPort));
        result.addItemsItem(makeApiConfig(getDatabaseApiConfigName(prefix, DatabaseProperties.NAME_PROPERTY),
                databaseName));
        result.addItemsItem(makeApiConfig(getDatabaseApiConfigName(prefix, DatabaseProperties.USER_PROPERTY),
                rdsConfig.getConnectionUserName()));
        result.addItemsItem(makeApiConfig(getDatabaseApiConfigName(prefix, DatabaseProperties.PASSWORD_PROPERTY),
                rdsConfig.getConnectionPassword()));

        return result;
    }

    private String getDatabaseApiConfigName(String prefix, String propertyName) {
        return String.format("%s_database_%s", prefix, propertyName);
    }
}
