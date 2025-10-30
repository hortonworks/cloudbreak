package com.sequenceiq.cloudbreak.cm;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterKraftMigrationStatusService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
@Scope("prototype")
public class ClouderaManagerClusterKraftMigrationStatusService implements ClusterKraftMigrationStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClusterKraftMigrationStatusService.class);

    private static final String FULL_VIEW = "FULL";

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final String KAFKA_BROKER_ROLE_TYPE = "KAFKA_BROKER";

    private static final String KRAFT_ROLE_TYPE = "KRAFT";

    private static final String METADATA_STORE_CONFIG = "metadata.store";

    private static final String KRAFT_METADATA_STORE_VALUE = "KRaft";

    private static final String KAFKA_PROPERTIES_ROLE_SAFETY_VALVE_CONFIG = "kafka.properties_role_safety_valve";

    private static final String KRAFT_PROPERTIES_ROLE_SAFETY_VALVE_CONFIG = "kraft.properties_role_safety_valve";

    private static final String ZOOKEEPER_METADATA_MIGRATION_ENABLE_TRUE = "zookeeper.metadata.migration.enable=true";

    private final StackDtoDelegate stack;

    private final HttpClientConfig clientConfig;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerConfigService configService;

    private ApiClient client;

    ClouderaManagerClusterKraftMigrationStatusService(StackDtoDelegate stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        ClusterView cluster = stack.getCluster();
        String cloudbreakClusterManagerUser = cluster.getCloudbreakClusterManagerUser();
        String cloudbreakClusterManagerPassword = cluster.getCloudbreakClusterManagerPassword();
        try {
            client = clouderaManagerApiClientProvider
                    .getV31Client(stack.getGatewayPort(), cloudbreakClusterManagerUser, cloudbreakClusterManagerPassword, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
    }

    @Override
    public KraftMigrationStatus getKraftMigrationStatus() {
        String clusterName = stack.getCluster().getName();
        String serviceName = getKafkaServiceName(clusterName);

        try {
            return determineStatusFromConfigs(clusterName, serviceName);
        } catch (ApiException e) {
            LOGGER.warn("Exception occurred while retrieving KRaft migration status", e);
            throw new ClouderaManagerOperationFailedException("Exception occurred while retrieving KRaft migration status", e);
        }
    }

    private String getKafkaServiceName(String clusterName) {
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        return configService.getServiceName(clusterName, KAFKA_SERVICE_TYPE, servicesResourceApi)
                .orElseThrow(() -> {
                    LOGGER.warn("Failed to get KRaft migration status. No {} service type found for cluster {}", KAFKA_SERVICE_TYPE, clusterName);
                    return new ClouderaManagerOperationFailedException(
                            String.format("Failed to get KRaft migration status. No %s service type found for cluster %s", KAFKA_SERVICE_TYPE, clusterName));
                });
    }

    private KraftMigrationStatus determineStatusFromConfigs(String clusterName, String serviceName) throws ApiException {
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(client);

        String kraftRoleConfigGroupName = configService.getRoleConfigGroupNameByTypeAndServiceName(
                KRAFT_ROLE_TYPE, clusterName, serviceName, roleConfigGroupsResourceApi);
        String kafkaBrokerConfigGroupName = configService.getRoleConfigGroupNameByTypeAndServiceName(
                KAFKA_BROKER_ROLE_TYPE, clusterName, serviceName, roleConfigGroupsResourceApi);

        ApiConfigList kafkaBrokerConfigList = roleConfigGroupsResourceApi.readConfig(clusterName, kafkaBrokerConfigGroupName, serviceName, FULL_VIEW);
        ApiConfigList kraftConfigList = roleConfigGroupsResourceApi.readConfig(clusterName, kraftRoleConfigGroupName, serviceName, FULL_VIEW);

        ApiConfig metadataStoreConfig = retrieveConfig(kafkaBrokerConfigList, METADATA_STORE_CONFIG);
        ApiConfig kafkaBrokerConfig = retrieveConfig(kafkaBrokerConfigList, KAFKA_PROPERTIES_ROLE_SAFETY_VALVE_CONFIG);
        ApiConfig kraftConfig = retrieveConfig(kraftConfigList, KRAFT_PROPERTIES_ROLE_SAFETY_VALVE_CONFIG);

        boolean brokerMigrationEnabled = isMigrationEnabled(kafkaBrokerConfig);
        boolean kraftMigrationEnabled = isMigrationEnabled(kraftConfig);
        boolean kraftMetadataStoreEnabled = isKraftMetadataStoreEnabled(metadataStoreConfig);

        return determineKraftMigrationStatus(brokerMigrationEnabled, kraftMigrationEnabled, kraftMetadataStoreEnabled);
    }

    private KraftMigrationStatus determineKraftMigrationStatus(boolean brokerMigrationEnabled,
            boolean kraftMigrationEnabled,
            boolean kraftMetadataStoreEnabled) {
        if (kraftMetadataStoreEnabled) {
            return determineKraftBasedStatus(brokerMigrationEnabled, kraftMigrationEnabled);
        } else {
            return determineZookeeperBasedStatus(brokerMigrationEnabled, kraftMigrationEnabled);
        }
    }

    private KraftMigrationStatus determineKraftBasedStatus(boolean brokerMigrationEnabled, boolean kraftMigrationEnabled) {
        if (!brokerMigrationEnabled && !kraftMigrationEnabled) {
            return KraftMigrationStatus.KRAFT_INSTALLED;
        } else if (brokerMigrationEnabled && kraftMigrationEnabled) {
            return KraftMigrationStatus.BROKERS_IN_KRAFT;
        }
        return KraftMigrationStatus.NOT_APPLICABLE;
    }

    private KraftMigrationStatus determineZookeeperBasedStatus(boolean brokerMigrationEnabled, boolean kraftMigrationEnabled) {
        if (!brokerMigrationEnabled && !kraftMigrationEnabled) {
            return KraftMigrationStatus.ZOOKEEPER_INSTALLED;
        } else if (!brokerMigrationEnabled && kraftMigrationEnabled) {
            return KraftMigrationStatus.PRE_MIGRATION;
        } else if (brokerMigrationEnabled && kraftMigrationEnabled) {
            return KraftMigrationStatus.BROKERS_IN_MIGRATION;
        }
        return KraftMigrationStatus.NOT_APPLICABLE;
    }

    private ApiConfig retrieveConfig(ApiConfigList configList, String configName) {
        return configList.getItems().stream()
                .filter(apiConfig -> apiConfig.getName().equals(configName))
                .findFirst()
                .orElse(null);
    }

    private boolean isMigrationEnabled(ApiConfig config) {
        if (config == null || config.getValue() == null) {
            return false;
        }

        return config.getValue().contains(ZOOKEEPER_METADATA_MIGRATION_ENABLE_TRUE);
    }

    private boolean isKraftMetadataStoreEnabled(ApiConfig config) {
        if (config == null || config.getValue() == null) {
            return false;
        }

        return KRAFT_METADATA_STORE_VALUE.equals(config.getValue());
    }
}
