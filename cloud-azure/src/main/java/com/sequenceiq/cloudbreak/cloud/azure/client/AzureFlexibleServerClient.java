package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.postgresqlflexibleserver.models.CapabilityStatus;
import com.azure.resourcemanager.postgresqlflexibleserver.models.FlexibleServerCapability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerState;
import com.sequenceiq.cloudbreak.cloud.azure.resource.domain.AzureCoordinate;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public class AzureFlexibleServerClient extends AbstractAzureServiceClient {

    public static final ServerState UNKNOWN = ServerState.fromString("UNKNOWN");

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureFlexibleServerClient.class);

    private final PostgreSqlManager postgreSqlFlexibleManager;

    public AzureFlexibleServerClient(PostgreSqlManager postgreSqlFlexibleManager, AzureExceptionHandler azureExceptionHandler,
            AzureListResultFactory azureListResultFactory) {
        super(azureExceptionHandler, azureListResultFactory);
        this.postgreSqlFlexibleManager = postgreSqlFlexibleManager;
    }

    public void startFlexibleServer(String resourceGroupName, String serverName) {
        handleException(() -> postgreSqlFlexibleManager.servers().start(resourceGroupName, serverName));
    }

    public void stopFlexibleServer(String resourceGroupName, String serverName) {
        handleException(() -> postgreSqlFlexibleManager.servers().stop(resourceGroupName, serverName));
    }

    public ServerState getFlexibleServerStatus(String resourceGroupName, String serverName) {
        Server server = getFlexibleServer(resourceGroupName, serverName);
        if (server == null) {
            LOGGER.debug("Flexible server not found with name {} in resourcegroup {}", serverName, resourceGroupName);
            return UNKNOWN;
        } else {
            LOGGER.debug("Flexible server status on Azure is {}", server.state());
            return server.state() != null ? server.state() : UNKNOWN;
        }
    }

    public Map<Region, Optional<FlexibleServerCapability>> getFlexibleServerCapabilityMap(Map<Region, AzureCoordinate> regions) {
        return regions.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, regionEntry -> getFlexibleServerCapability(regionEntry.getValue().getKey())));
    }

    public Optional<FlexibleServerCapability> getFlexibleServerCapability(AzureCoordinate azureCoordinate) {
        return getFlexibleServerCapability(azureCoordinate.getKey());
    }

    public void updateAdministratorLoginPassword(String resourceGroupName, String serverName, String newPassword) {
        Server server = getFlexibleServer(resourceGroupName, serverName);
        if (server == null) {
            String message = String.format("Flexible server not found with name %s in resource group %s", serverName, resourceGroupName);
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        } else {
            handleException(() -> server.update().withAdministratorLoginPassword(newPassword).apply());
        }
    }

    private Optional<FlexibleServerCapability> getFlexibleServerCapability(String locationName) {
        try {
            List<FlexibleServerCapability> flexibleServerCapabilities = getAzureListResultFactory().create(
                            postgreSqlFlexibleManager.locationBasedCapabilities().execute(locationName))
                    .getStream()
                    .filter(capability -> !CapabilityStatus.DISABLED.equals(capability.status()))
                    .toList();
            if (flexibleServerCapabilities.size() > 1) {
                LOGGER.warn("There are more than one flexible server capability has been provided, will use the first one." +
                                " Provided flexible server capabilites in region {}: {}", locationName,
                        flexibleServerCapabilities.stream().map(FlexibleServerCapability::name).collect(Collectors.joining(",")));
            }
            return flexibleServerCapabilities.isEmpty() ? Optional.empty() : Optional.of(flexibleServerCapabilities.get(0));
        } catch (ManagementException managementException) {
            LOGGER.debug("We were not able to query flexible supported capability because of: " + managementException.getMessage());
            return Optional.empty();
        }
    }

    public Server getFlexibleServer(String resourceGroupName, String serverName) {
        return handleException(() -> postgreSqlFlexibleManager.servers().getByResourceGroup(resourceGroupName, serverName));
    }
}
