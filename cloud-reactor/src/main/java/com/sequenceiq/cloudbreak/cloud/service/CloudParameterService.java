package com.sequenceiq.cloudbreak.cloud.service;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetCdpPlatformRegionsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDefaultPlatformDatabaseCapabilityRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudAccessConfigsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudGatewaysRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudIpPoolsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformDatabaseCapabilityRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformDatabaseVmTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformEncryptionKeysRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformInstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNetworksRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNoSqlTablesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformOrchestratorsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformPrivateDnsZonesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsRequestV2;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformResourceGroupsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformSecurityGroupsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformSshKeysRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVmTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineRecommendtaionRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParametersRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudDatabaseVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.DefaultPlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterResponse;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.util.PermanentlyFailedException;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@Service
public class CloudParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudParameterService.class);

    private static final String AWS_AUTH_ERROR_MESSAGE = "not authorized to perform";

    private static final String AZURE_AUTH_ERROR_MESSAGE = "does not have authorization to perform action";

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EntitlementService entitlementService;

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public PlatformVariants getPlatformVariants() {
        LOGGER.debug("Get platform variants");
        GetPlatformVariantsRequest request = new GetPlatformVariantsRequest();
        return executeRequestAndHandleErrors(request, "platform variants").getPlatformVariants();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public PlatformDisks getDiskTypes() {
        LOGGER.debug("Get platform disktypes");
        GetDiskTypesRequest request = new GetDiskTypesRequest();
        return executeRequestAndHandleErrors(request, "platform disk types").getPlatformDisks();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public PlatformOrchestrators getOrchestrators() {
        LOGGER.debug("Get platform orchestrators");
        GetPlatformOrchestratorsRequest request = new GetPlatformOrchestratorsRequest();
        return executeRequestAndHandleErrors(request, "platform orchestrators").getPlatformOrchestrators();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public Map<Platform, PlatformParameters> getPlatformParameters() {
        LOGGER.debug("Get platform parameters");
        PlatformParametersRequest request = new PlatformParametersRequest();
        return executeRequestAndHandleErrors(request, "platform parameters").getPlatformParameters();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public PlatformDatabaseCapabilities getDatabaseCapabilities(ExtendedCloudCredential cloudCredential, String region,
            String variant, Map<String, String> filters) {
        LOGGER.debug("Get database capabilities");
        GetPlatformDatabaseCapabilityRequest request = new GetPlatformDatabaseCapabilityRequest(cloudCredential, cloudCredential, variant, region, filters);
        return executeRequestAndHandleErrors(request, "database capabilities").getPlatformDatabaseCapabilities();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public DefaultPlatformDatabaseCapabilities getDefaultDatabaseCapabilities(String platform) {
        LOGGER.debug("Get default database capabilities");
        GetDefaultPlatformDatabaseCapabilityRequest request = new GetDefaultPlatformDatabaseCapabilityRequest(platform);
        return executeRequestAndHandleErrors(request, "default database capabilities").getDefaultPlatformDatabaseCapabilities();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudNetworks getCloudNetworks(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform cloudnetworks");
        GetPlatformNetworksRequest request = new GetPlatformNetworksRequest(cloudCredential, cloudCredential, variant, region, filters);
        return executeRequestAndHandleErrors(request, "networks").getCloudNetworks();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudSshKeys getCloudSshKeys(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform sshkeys");
        GetPlatformSshKeysRequest request = new GetPlatformSshKeysRequest(cloudCredential, cloudCredential, variant, region, filters);
        return executeRequestAndHandleErrors(request, "SSH keys").getCloudSshKeys();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudSecurityGroups getSecurityGroups(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform securitygroups");
        GetPlatformSecurityGroupsRequest request = new GetPlatformSecurityGroupsRequest(cloudCredential, cloudCredential, variant, region, filters);
        return executeRequestAndHandleErrors(request, "security groups").getCloudSecurityGroups();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public VmRecommendations getRecommendation(String platform) {
        LOGGER.debug("Get platform vm recommendation");
        GetVirtualMachineRecommendtaionRequest request = new GetVirtualMachineRecommendtaionRequest(platform);
        return executeRequestAndHandleErrors(request, "platform VM recommendation").getRecommendations();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudVmTypes getVmTypesV2(ExtendedCloudCredential cloudCredential, String region, String variant,
            CdpResourceType stackType, Map<String, String> filters) {
        LOGGER.debug("Get platform vmtypes");
        boolean hasEnableDistroxInstanceTypesEnabled = entitlementService.enableDistroxInstanceTypesEnabled(cloudCredential.getAccountId());
        GetPlatformVmTypesRequest request =
                new GetPlatformVmTypesRequest(cloudCredential, cloudCredential, variant, region, stackType, hasEnableDistroxInstanceTypesEnabled, filters);
        return executeRequestAndHandleErrors(request, "Vm types").getCloudVmTypes();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudDatabaseVmTypes getDatabaseVmTypes(ExtendedCloudCredential cloudCredential, String region, String variant,
            CdpResourceType stackType, Map<String, String> filters) {
        LOGGER.debug("Get platform database vmtypes");
        GetPlatformDatabaseVmTypesRequest request =
                new GetPlatformDatabaseVmTypesRequest(cloudCredential, cloudCredential, variant, region, stackType, filters);
        return executeRequestAndHandleErrors(request, "Database vm types").getCloudDatabaseVmTypes();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudRegions getRegionsV2(ExtendedCloudCredential cloudCredential, String region, String variant,
            Map<String, String> filters, boolean availabilityZonesNeeded) {
        LOGGER.debug("Get platform regions");
        GetPlatformRegionsRequestV2 request =
                new GetPlatformRegionsRequestV2(cloudCredential, cloudCredential, variant, region, filters, availabilityZonesNeeded);
        return executeRequestAndHandleErrors(request, "regions").getCloudRegions();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudGateWays getGateways(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform gateways");
        GetPlatformCloudGatewaysRequest request = new GetPlatformCloudGatewaysRequest(cloudCredential, cloudCredential, variant, region, filters);
        return executeRequestAndHandleErrors(request, "gateways").getCloudGateWays();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudIpPools getPublicIpPools(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform publicIpPools");
        GetPlatformCloudIpPoolsRequest request = new GetPlatformCloudIpPoolsRequest(cloudCredential, cloudCredential, variant, region, filters);
        return executeRequestAndHandleErrors(request, "public IP pools").getCloudIpPools();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudAccessConfigs getCloudAccessConfigs(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform accessConfigs");
        GetPlatformCloudAccessConfigsRequest request = new GetPlatformCloudAccessConfigsRequest(cloudCredential, cloudCredential, variant, region, filters);
        return executeRequestAndHandleErrors(request, "access configs").getCloudAccessConfigs();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudEncryptionKeys getCloudEncryptionKeys(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform encryptionKeys");
        GetPlatformEncryptionKeysRequest request = new GetPlatformEncryptionKeysRequest(cloudCredential, cloudCredential, variant, region, filters);
        return executeRequestAndHandleErrors(request, "encryption keys").getCloudEncryptionKeys();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public Map<String, InstanceGroupParameterResponse> getInstanceGroupParameters(ExtendedCloudCredential cloudCredential,
            Set<InstanceGroupParameterRequest> instanceGroups) {
        LOGGER.debug("Get platform instanceGroupParameters");
        GetPlatformInstanceGroupParameterRequest request = new GetPlatformInstanceGroupParameterRequest(cloudCredential, cloudCredential, instanceGroups, null);
        return executeRequestAndHandleErrors(request, "instance group parameters").getInstanceGroupParameterResponses();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudNoSqlTables getNoSqlTables(ExtendedCloudCredential cloudCredential, String region, String platformVariant, Map<String, String> filters) {
        LOGGER.debug("Get platform noSqlTables");
        GetPlatformNoSqlTablesRequest request = new GetPlatformNoSqlTablesRequest(cloudCredential, cloudCredential, platformVariant, region, null);
        return executeRequestAndHandleErrors(request, "NoSQL tables").getNoSqlTables();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudResourceGroups getResourceGroups(ExtendedCloudCredential cloudCredential, String region, String platformVariant, Map<String, String> filters) {
        LOGGER.debug("Get platform resource groups for credential: [{}]", cloudCredential.getName());
        GetPlatformResourceGroupsRequest request = new GetPlatformResourceGroupsRequest(cloudCredential, cloudCredential, platformVariant, region, null);
        return executeRequestAndHandleErrors(request, "resource group tables").getResourceGroups();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudRegions getCdpRegions(String platform, String platformVariant) {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(platform)
                .withVariant(platformVariant)
                .build();
        GetCdpPlatformRegionsRequest request = new GetCdpPlatformRegionsRequest(cloudContext);
        return executeRequestAndHandleErrors(request, "resource group tables").getCloudRegions();
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudPrivateDnsZones getPrivateDnsZones(ExtendedCloudCredential cloudCredential, String platformVariant, Map<String, String> filters) {
        LOGGER.debug("Get platform private DNS zones for credential: [{}]", cloudCredential.getName());
        GetPlatformPrivateDnsZonesRequest request = new GetPlatformPrivateDnsZonesRequest(cloudCredential, cloudCredential, platformVariant, null);
        return executeRequestAndHandleErrors(request, "private DNS zones").getPrivateDnsZones();
    }

    private <T extends CloudPlatformRequest<U>, U extends CloudPlatformResult> U executeRequestAndHandleErrors(T request, String responseName) {
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            U result = request.await();
            LOGGER.debug("{} result: {}", result.getClass().getSimpleName(), result);
            if (result.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.warn("Failed to get {}", result.getClass().getSimpleName(), result.getErrorDetails());
                throw createExceptionBasedOnErrorType(result, responseName);
            }
            if (result.getStatus().equals(EventStatus.PERMANENTLY_FAILED)) {
                LOGGER.warn("Failed to get {}", result.getClass().getSimpleName(), result.getErrorDetails());
                throw new PermanentlyFailedException(result.getStatusReason());
            }
            return result;
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing request {}", request.getClass().getSimpleName(), e);
            throw new OperationException(e);
        }
    }

    private RuntimeException createExceptionBasedOnErrorType(CloudPlatformResult result, String responseName) {
        Exception errorDetails = result.getErrorDetails();
        String errorMessage = String.format("Failed to get %s for the cloud provider: %s. %s",
                responseName, result.getStatusReason(), getCauseMessages(errorDetails));
        if (errorDetails instanceof ProviderAuthenticationFailedException
                || (errorDetails.getMessage() != null && errorDetails.getMessage().contains(AWS_AUTH_ERROR_MESSAGE))
                || (errorDetails.getMessage() != null && errorDetails.getMessage().contains(AZURE_AUTH_ERROR_MESSAGE))) {
            return new BadRequestException(errorDetails);
        } else {
            return new GetCloudParameterException(errorMessage, errorDetails);
        }
    }

    private String getCauseMessages(Exception e) {
        if (e != null) {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Causes: [").append(e.getMessage());
            if (e.getCause() != null) {
                messageBuilder.append("], [").append(e.getCause().getMessage());
            }
            return messageBuilder.append("].").toString();
        }
        return "";
    }
}
