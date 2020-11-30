package com.sequenceiq.redbeams.converter.spi;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType.NONE;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SecurityGroup;
import com.sequenceiq.redbeams.domain.stack.SslConfig;
import com.sequenceiq.redbeams.service.EnvironmentService;

@Component
public class DBStackToDatabaseStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackToDatabaseStackConverter.class);

    @Inject
    private EnvironmentService environmentService;

    public DatabaseStack convert(DBStack dbStack) {
        Network network = buildNetwork(dbStack);
        DatabaseServer databaseServer = buildDatabaseServer(dbStack);
        return new DatabaseStack(network, databaseServer, getUserDefinedTags(dbStack), dbStack.getTemplate());
    }

    private Network buildNetwork(DBStack dbStack) {
        com.sequenceiq.redbeams.domain.stack.Network dbStackNetwork = dbStack.getNetwork();
        if (dbStackNetwork == null) {
            return null;
        }
        Json attributes = dbStackNetwork.getAttributes();
        Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
        return new Network(null, params);
    }

    private DatabaseServer buildDatabaseServer(DBStack dbStack) {
        com.sequenceiq.redbeams.domain.stack.DatabaseServer dbStackDatabaseServer = dbStack.getDatabaseServer();
        if (dbStackDatabaseServer == null) {
            return null;
        }

        Map<String, Object> params = buildParameters(dbStack);
        SecurityGroup securityGroup = dbStackDatabaseServer.getSecurityGroup();

        DatabaseServer.Builder builder = DatabaseServer.builder()
                .serverId(dbStackDatabaseServer.getName())
                .flavor(dbStackDatabaseServer.getInstanceType())
                .engine(getDatabaseEngine(dbStackDatabaseServer))
                .connectionDriver(dbStackDatabaseServer.getConnectionDriver())
                .rootUserName(dbStackDatabaseServer.getRootUserName())
                .rootPassword(dbStackDatabaseServer.getRootPassword())
                .port(dbStackDatabaseServer.getPort())
                .useSslEnforcement(determineSslEnforcement(dbStack))
                .storageSize(dbStackDatabaseServer.getStorageSize())
                .security(securityGroup == null ? null : new Security(Collections.emptyList(), securityGroup.getSecurityGroupIds()))
                // TODO / FIXME converter caller decides this?
                .status(CREATE_REQUESTED)
                .location(dbStack.getRegion())
                .highAvailability(dbStack.isHa())
                .params(params);

        return builder.build();
    }

    private DatabaseEngine getDatabaseEngine(com.sequenceiq.redbeams.domain.stack.DatabaseServer dbStackDatabaseServer) {
        DatabaseEngine engine;
        switch (dbStackDatabaseServer.getDatabaseVendor()) {
            case POSTGRES:
                engine = DatabaseEngine.POSTGRESQL;
                break;
            case MYSQL:
            case MARIADB:
            case MSSQL:
            case ORACLE11:
            case ORACLE12:
            case SQLANYWHERE:
            default:
                throw new BadRequestException("Unsupported database vendor " + dbStackDatabaseServer.getDatabaseVendor());
        }
        return engine;
    }

    private boolean determineSslEnforcement(DBStack dbStack) {
        SslConfig sslConfig = dbStack.getSslConfig();
        return sslConfig != null && !NONE.equals(sslConfig.getSslCertificateType());
    }

    private Map<String, String> getUserDefinedTags(DBStack dbStack) {
        Map<String, String> result = Maps.newHashMap();
        try {
            if (dbStack.getTags() != null && dbStack.getTags().getValue() != null) {
                StackTags stackTag = dbStack.getTags().get(StackTags.class);
                Map<String, String> userDefined = stackTag.getUserDefinedTags();
                Map<String, String> defaultTags = stackTag.getDefaultTags();
                if (defaultTags != null) {
                    result.putAll(defaultTags);
                }
                if (userDefined != null) {
                    result.putAll(userDefined);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read JSON tags, skipping", e);
        }
        return result;
    }

    private Map<String, Object> buildParameters(DBStack stack) {
        Json attributes = stack.getDatabaseServer().getAttributes();
        Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();

        if (CloudPlatform.AZURE.name().equals(stack.getCloudPlatform()) && !stack.getParameters().containsKey(RESOURCE_GROUP_NAME_PARAMETER)) {
            DetailedEnvironmentResponse environment = measure(() -> environmentService.getByCrn(stack.getEnvironmentId()),
                    LOGGER, "Environment properties were queried under {} ms for environment {}", stack.getEnvironmentId());

            Optional<AzureResourceGroup> resourceGroupOptional = getResourceGroupFromEnv(environment);

            if (resourceGroupOptional.isPresent() && !ResourceGroupUsage.MULTIPLE.equals(resourceGroupOptional.get().getResourceGroupUsage())) {
                AzureResourceGroup resourceGroup = resourceGroupOptional.get();
                String resourceGroupName = resourceGroup.getName();
                ResourceGroupUsage resourceGroupUsage = resourceGroup.getResourceGroupUsage();
                Map<String, Object> resourceGroupParameters = Map.of(
                        RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName,
                        RESOURCE_GROUP_USAGE_PARAMETER, resourceGroupUsage.name());
                return Stream.of(params, resourceGroupParameters)
                        .flatMap(map -> map.entrySet().stream())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (existingOne, newOne) -> existingOne));
            } else {
                return params;
            }
        } else {
            return params;
        }
    }

    private Optional<AzureResourceGroup> getResourceGroupFromEnv(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceGroup);
    }

}
