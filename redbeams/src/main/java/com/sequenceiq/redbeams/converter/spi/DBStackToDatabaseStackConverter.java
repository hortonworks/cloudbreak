package com.sequenceiq.redbeams.converter.spi;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ENCRYPTION_KEY_RESOURCE_GROUP_NAME;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ENCRYPTION_KEY_URL;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ENCRYPTION_USER_MANAGED_IDENTITY;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType.NONE;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

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
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SecurityGroup;
import com.sequenceiq.redbeams.domain.stack.SslConfig;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.network.NetworkService;
import com.sequenceiq.redbeams.service.sslcertificate.SslConfigService;

@Component
public class DBStackToDatabaseStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackToDatabaseStackConverter.class);

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private NetworkService networkService;

    @Inject
    private SslConfigService sslConfigService;

    public DatabaseStack convert(DBStack dbStack) {
        Network network = buildNetwork(dbStack);
        DatabaseServer databaseServer = buildDatabaseServer(dbStack);
        return new DatabaseStack(network, databaseServer, getUserDefinedTags(dbStack), dbStack.getTemplate());
    }

    public Network buildNetwork(DBStack dbStack) {
        if (dbStack.getNetwork() == null) {
            return null;
        } else {
            return networkService.findById(dbStack.getNetwork())
                    .map(network -> {
                        Json attributes = network.getAttributes();
                        Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
                        return new Network(null, params);
                    })
                    .orElse(null);
        }
    }

    public DatabaseServer buildDatabaseServer(DBStack dbStack) {
        com.sequenceiq.redbeams.domain.stack.DatabaseServer dbStackDatabaseServer = dbStack.getDatabaseServer();
        if (dbStackDatabaseServer == null) {
            return null;
        }

        Map<String, Object> params = buildParameters(dbStack);
        SecurityGroup securityGroup = dbStackDatabaseServer.getSecurityGroup();

        DatabaseServer.Builder builder = DatabaseServer.builder()
                .withServerId(dbStackDatabaseServer.getName())
                .withFlavor(dbStackDatabaseServer.getInstanceType())
                .withEngine(getDatabaseEngine(dbStackDatabaseServer))
                .withConnectionDriver(dbStackDatabaseServer.getConnectionDriver())
                .withRootUserName(dbStackDatabaseServer.getRootUserName())
                .withRootPassword(dbStackDatabaseServer.getRootPassword())
                .withPort(dbStackDatabaseServer.getPort())
                .withUseSslEnforcement(determineSslEnforcement(dbStack))
                .withStorageSize(dbStackDatabaseServer.getStorageSize())
                .withSecurity(securityGroup == null ? null : new Security(Collections.emptyList(), securityGroup.getSecurityGroupIds()))
                // TODO / FIXME converter caller decides this?
                .withStatus(CREATE_REQUESTED)
                .withLocation(dbStack.getRegion())
                .withHighAvailability(dbStack.isHa())
                .withParams(params);

        return builder.build();
    }

    private DatabaseEngine getDatabaseEngine(com.sequenceiq.redbeams.domain.stack.DatabaseServer dbStackDatabaseServer) {
        return switch (dbStackDatabaseServer.getDatabaseVendor()) {
            case POSTGRES -> DatabaseEngine.POSTGRESQL;
            default -> throw new BadRequestException("Unsupported database vendor " + dbStackDatabaseServer.getDatabaseVendor());
        };
    }

    private boolean determineSslEnforcement(DBStack dbStack) {
        Optional<SslConfig> sslConfig = sslConfigService.fetchById(dbStack.getSslConfig());
        return sslConfig.isPresent() && !NONE.equals(sslConfig.get().getSslCertificateType());
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

        if (CloudPlatform.AZURE.name().equals(stack.getCloudPlatform())) {
            DetailedEnvironmentResponse environment = getDetailedEnvironmentResponse(stack);
            if (!stack.getParameters().containsKey(RESOURCE_GROUP_NAME_PARAMETER)) {
                Optional<AzureResourceGroup> resourceGroupOptional = getResourceGroupFromEnv(environment);

                if (resourceGroupOptional.isPresent() && !ResourceGroupUsage.MULTIPLE.equals(resourceGroupOptional.get().getResourceGroupUsage())) {
                    AzureResourceGroup resourceGroup = resourceGroupOptional.get();
                    String resourceGroupName = resourceGroup.getName();
                    ResourceGroupUsage resourceGroupUsage = resourceGroup.getResourceGroupUsage();
                    Map<String, Object> resourceGroupParameters = Map.of(
                            RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName,
                            RESOURCE_GROUP_USAGE_PARAMETER, resourceGroupUsage.name());
                    params = getMergedMap(params, resourceGroupParameters);
                }
            }
            if (azureEncryptionParametersPresent(environment)) {
                Map<String, Object> encryptionParameters = new HashMap<>();
                String encryptionKeyUrlFromEnv = getEncryptionKeyUrlFromEnv(environment);
                if (encryptionKeyUrlFromEnv != null) {
                    encryptionParameters.put(ENCRYPTION_KEY_URL, encryptionKeyUrlFromEnv);
                }
                String encryptionKeyResourceGroupNameFromEnv = getEncryptionKeyResourceGroupNameFromEnv(environment);
                if (encryptionKeyResourceGroupNameFromEnv != null) {
                    encryptionParameters.put(ENCRYPTION_KEY_RESOURCE_GROUP_NAME, encryptionKeyResourceGroupNameFromEnv);
                }
                String encryptionManagedIdentity = getEncryptionManagedIdentity(environment);
                if (encryptionManagedIdentity != null) {
                    encryptionParameters.put(ENCRYPTION_USER_MANAGED_IDENTITY, encryptionManagedIdentity);
                }
                params = getMergedMap(params, encryptionParameters);
            }
        } else if (CloudPlatform.GCP.name().equals(stack.getCloudPlatform())) {
            DetailedEnvironmentResponse environment = getDetailedEnvironmentResponse(stack);
            Optional<String> key = getGcpEncryptionKeyFromEnv(environment);
            if (key.isPresent()) {
                Map<String, Object> encryptionParameters = Map.of(
                        VOLUME_ENCRYPTION_KEY_ID, key.get());
                params = getMergedMap(params, encryptionParameters);
            }
        } else if (CloudPlatform.AWS.name().equals(stack.getCloudPlatform())) {
            DetailedEnvironmentResponse environment = getDetailedEnvironmentResponse(stack);
            Optional<String> key = getEncryptionKeyArnFromEnv(environment);
            if (key.isPresent()) {
                Map<String, Object> awsEncryptionParameters = Map.of(
                        VOLUME_ENCRYPTION_KEY_ID, key.get());
                params = getMergedMap(params, awsEncryptionParameters);
            }
        }
        return params;
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponse(DBStack stack) {
        return measure(() -> environmentService.getByCrn(stack.getEnvironmentId()),
                LOGGER, "Environment properties were queried under {} ms for environment {}", stack.getEnvironmentId());
    }

    private Map<String, Object> getMergedMap(Map<String, Object> params, Map<String, Object> cloudParams) {
        params = Stream.of(params, cloudParams)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existingOne, newOne) -> existingOne));
        return params;
    }

    private Optional<AzureResourceGroup> getResourceGroupFromEnv(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceGroup);
    }

    private boolean azureEncryptionParametersPresent(DetailedEnvironmentResponse environment) {
        return getEncryptionKeyUrlFromEnv(environment) != null && getEncryptionKeyResourceGroupNameFromEnv(environment) != null;
    }

    private String getEncryptionKeyUrlFromEnv(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceEncryptionParameters)
                .map(AzureResourceEncryptionParameters::getEncryptionKeyUrl).orElse(null);
    }

    private String getEncryptionManagedIdentity(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceEncryptionParameters)
                .map(AzureResourceEncryptionParameters::getUserManagedIdentity).orElse(null);
    }

    private Optional<String> getGcpEncryptionKeyFromEnv(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getGcp)
                .map(GcpEnvironmentParameters::getGcpResourceEncryptionParameters)
                .map(GcpResourceEncryptionParameters::getEncryptionKey);
    }

    private String getEncryptionKeyResourceGroupNameFromEnv(DetailedEnvironmentResponse environment) {
        /* There would not be a case where both encryptionKeyResourceGroupName and azure Resource group is null.
         *  If multiple azure resource groups then encryptionKeyResourceGroupName is mandatorily required to enable encryption with CMK.
         *  This is already checked during environment start as part of azureEncryptionResources.
         *  At least one of --resource-group-name or --encryption-key-resource-group-name should be specified.
         * */
        String encryptionKeyResourceGroupNameFromEnv = Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceEncryptionParameters)
                .map(AzureResourceEncryptionParameters::getEncryptionKeyResourceGroupName).orElse(null);
        if (encryptionKeyResourceGroupNameFromEnv == null) {
            encryptionKeyResourceGroupNameFromEnv = Optional.ofNullable(environment)
                    .map(DetailedEnvironmentResponse::getAzure)
                    .map(AzureEnvironmentParameters::getResourceGroup)
                    .map(AzureResourceGroup::getName).orElse(null);
        }
        return encryptionKeyResourceGroupNameFromEnv;
    }

    private Optional<String> getEncryptionKeyArnFromEnv(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getAws)
                .map(AwsEnvironmentParameters::getAwsDiskEncryptionParameters)
                .map(AwsDiskEncryptionParameters::getEncryptionKeyArn);

    }

}