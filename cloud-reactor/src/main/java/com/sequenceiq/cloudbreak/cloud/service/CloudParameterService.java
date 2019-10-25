package com.sequenceiq.cloudbreak.cloud.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudAccessConfigsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudAccessConfigsResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudGatewaysRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudGatewaysResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudIpPoolsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudIpPoolsResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformEncryptionKeysRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformEncryptionKeysResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformInstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformInstanceGroupParameterResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNetworksRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNetworksResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNoSqlTablesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNoSqlTablesResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformOrchestratorsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformOrchestratorsResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsRequestV2;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsResultV2;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformSecurityGroupsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformSecurityGroupsResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformSshKeysRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformSshKeysResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVmTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVmTypesResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineRecommendationResponse;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineRecommendtaionRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParametersRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParametersResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterResponse;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class CloudParameterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudParameterService.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public PlatformVariants getPlatformVariants() {
        LOGGER.debug("Get platform variants");
        GetPlatformVariantsRequest getPlatformVariantsRequest = new GetPlatformVariantsRequest();
        eventBus.notify(getPlatformVariantsRequest.selector(), eventFactory.createEvent(getPlatformVariantsRequest));
        try {
            GetPlatformVariantsResult res = getPlatformVariantsRequest.await();
            LOGGER.debug("Platform variants result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform variants", res.getErrorDetails());
                throw new GetCloudParameterException(getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getPlatformVariants();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform variants", e);
            throw new OperationException(e);
        }
    }

    public String getPlatformByVariant(String requestedVariant) {
        PlatformVariants platformVariants = getPlatformVariants();
        for (Entry<Platform, Collection<Variant>> platformCollectionEntry : platformVariants.getPlatformToVariants().entrySet()) {
            for (Variant variant : platformCollectionEntry.getValue()) {
                if (variant.value().equals(requestedVariant)) {
                    return platformCollectionEntry.getKey().value();
                }
            }
        }
        return null;
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public PlatformDisks getDiskTypes() {
        LOGGER.debug("Get platform disktypes");
        GetDiskTypesRequest getDiskTypesRequest = new GetDiskTypesRequest();
        eventBus.notify(getDiskTypesRequest.selector(), eventFactory.createEvent(getDiskTypesRequest));
        try {
            GetDiskTypesResult res = getDiskTypesRequest.await();
            LOGGER.debug("Platform disk types result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform disk types", res.getErrorDetails());
                throw new GetCloudParameterException(getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getPlatformDisks();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform disk types", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public PlatformRegions getRegions() {
        LOGGER.debug("Get platform regions");
        GetPlatformRegionsRequest getPlatformRegionsRequest = new GetPlatformRegionsRequest();
        eventBus.notify(getPlatformRegionsRequest.selector(), eventFactory.createEvent(getPlatformRegionsRequest));
        try {
            GetPlatformRegionsResult res = getPlatformRegionsRequest.await();
            LOGGER.debug("Platform regions result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform regions", res.getErrorDetails());
                throw new GetCloudParameterException(getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getPlatformRegions();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform regions", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public PlatformOrchestrators getOrchestrators() {
        LOGGER.debug("Get platform orchestrators");
        GetPlatformOrchestratorsRequest getPlatformOrchestratorsRequest = new GetPlatformOrchestratorsRequest();
        eventBus.notify(getPlatformOrchestratorsRequest.selector(), eventFactory.createEvent(getPlatformOrchestratorsRequest));
        try {
            GetPlatformOrchestratorsResult res = getPlatformOrchestratorsRequest.await();
            LOGGER.debug("Platform orchestrators result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform orchestrators", res.getErrorDetails());
                throw new GetCloudParameterException(getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getPlatformOrchestrators();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform orchestrators", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public Map<Platform, PlatformParameters> getPlatformParameters() {
        LOGGER.debug("Get platform parameters for");
        PlatformParametersRequest parametersRequest = new PlatformParametersRequest();
        eventBus.notify(parametersRequest.selector(), eventFactory.createEvent(parametersRequest));
        try {
            PlatformParametersResult res = parametersRequest.await();
            LOGGER.debug("Platform parameter result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform parameters", res.getErrorDetails());
                throw new GetCloudParameterException(getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getPlatformParameters();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting platform parameters", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudNetworks getCloudNetworks(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform cloudnetworks");

        GetPlatformNetworksRequest getPlatformNetworksRequest =
                new GetPlatformNetworksRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformNetworksRequest.selector(), eventFactory.createEvent(getPlatformNetworksRequest));
        try {
            GetPlatformNetworksResult res = getPlatformNetworksRequest.await();
            LOGGER.debug("Platform networks types result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform networks", res.getErrorDetails());
                throw new GetCloudParameterException("Failed to get networks for the cloud provider. "
                        + getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getCloudNetworks();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform networks", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudSshKeys getCloudSshKeys(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform sshkeys");
        GetPlatformSshKeysRequest getPlatformSshKeysRequest =
                new GetPlatformSshKeysRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformSshKeysRequest.selector(), eventFactory.createEvent(getPlatformSshKeysRequest));
        try {
            GetPlatformSshKeysResult res = getPlatformSshKeysRequest.await();
            LOGGER.debug("Platform sshkeys types result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform sshkeys", res.getErrorDetails());
                throw new GetCloudParameterException("Failed to get SSH keys for the cloud provider. "
                        + getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getCloudSshKeys();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform sshkeys", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudSecurityGroups getSecurityGroups(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform securitygroups");
        GetPlatformSecurityGroupsRequest getPlatformSecurityGroupsRequest =
                new GetPlatformSecurityGroupsRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformSecurityGroupsRequest.selector(), eventFactory.createEvent(getPlatformSecurityGroupsRequest));
        try {
            GetPlatformSecurityGroupsResult res = getPlatformSecurityGroupsRequest.await();
            LOGGER.debug("Platform securitygroups types result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform securitygroups", res.getErrorDetails());
                throw new GetCloudParameterException("Failed to get security groups for the cloud provider. "
                        + getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getCloudSecurityGroups();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform securitygroups", e);
            throw new OperationException(e);
        }
    }

    public SpecialParameters getSpecialParameters() {
        LOGGER.debug("Get special platform parameters");
        Map<String, Boolean> specialParameters = new HashMap<>();
        return new SpecialParameters(specialParameters);
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public VmRecommendations getRecommendation(String platform) {
        LOGGER.debug("Get platform vm recommendation");
        GetVirtualMachineRecommendtaionRequest getVirtualMachineRecommendtaionRequest = new GetVirtualMachineRecommendtaionRequest(platform);
        eventBus.notify(getVirtualMachineRecommendtaionRequest.selector(), eventFactory.createEvent(getVirtualMachineRecommendtaionRequest));
        try {
            GetVirtualMachineRecommendationResponse res = getVirtualMachineRecommendtaionRequest.await();
            LOGGER.debug("Platform vm recommendation result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform vm recommendation", res.getErrorDetails());
                throw new GetCloudParameterException(getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getRecommendations();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform vm recommendation", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudVmTypes getVmTypesV2(ExtendedCloudCredential cloudCredential, String region, String variant,
        CdpResourceType stackType, Map<String, String> filters) {
        LOGGER.debug("Get platform vmtypes");
        GetPlatformVmTypesRequest getPlatformVmTypesRequest =
                new GetPlatformVmTypesRequest(cloudCredential, cloudCredential, variant, region, stackType, filters);
        eventBus.notify(getPlatformVmTypesRequest.selector(), Event.wrap(getPlatformVmTypesRequest));
        try {
            GetPlatformVmTypesResult res = getPlatformVmTypesRequest.await();
            LOGGER.debug("Platform vmtypes result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform vmtypes", res.getErrorDetails());
                throw new GetCloudParameterException("Failed to get VM types for the cloud provider. "
                        + getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getCloudVmTypes();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform vmtypes", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudRegions getRegionsV2(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform regions");
        GetPlatformRegionsRequestV2 getPlatformRegionsRequest =
                new GetPlatformRegionsRequestV2(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformRegionsRequest.selector(), Event.wrap(getPlatformRegionsRequest));
        try {
            GetPlatformRegionsResultV2 res = getPlatformRegionsRequest.await();
            LOGGER.debug("Platform regions result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform regions", res.getErrorDetails());
                throw new GetCloudParameterException("Failed to get regions from the cloud provider due to network issues or invalid credential. "
                        + getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getCloudRegions();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform regions", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudGateWays getGateways(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform gateways");
        GetPlatformCloudGatewaysRequest getPlatformCloudGatewaysRequest =
                new GetPlatformCloudGatewaysRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformCloudGatewaysRequest.selector(), Event.wrap(getPlatformCloudGatewaysRequest));
        try {
            GetPlatformCloudGatewaysResult res = getPlatformCloudGatewaysRequest.await();
            LOGGER.debug("Platform gateways result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform gateways", res.getErrorDetails());
                throw new GetCloudParameterException("Failed to get gateways for the cloud provider. "
                        + getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getCloudGateWays();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform gateways", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudIpPools getPublicIpPools(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform publicIpPools");
        GetPlatformCloudIpPoolsRequest getPlatformCloudIpPoolsRequest =
                new GetPlatformCloudIpPoolsRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformCloudIpPoolsRequest.selector(), Event.wrap(getPlatformCloudIpPoolsRequest));
        try {
            GetPlatformCloudIpPoolsResult res = getPlatformCloudIpPoolsRequest.await();
            LOGGER.debug("Platform publicIpPools result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform publicIpPools", res.getErrorDetails());
                throw new GetCloudParameterException("Failed to get public IP pools for the cloud provider. "
                        + getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getCloudIpPools();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform publicIpPools", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudAccessConfigs getCloudAccessConfigs(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform accessConfigs");
        GetPlatformCloudAccessConfigsRequest getPlatformCloudAccessConfigsRequest =
                new GetPlatformCloudAccessConfigsRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformCloudAccessConfigsRequest.selector(), Event.wrap(getPlatformCloudAccessConfigsRequest));
        try {
            GetPlatformCloudAccessConfigsResult res = getPlatformCloudAccessConfigsRequest.await();
            LOGGER.debug("Platform accessConfigs result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform accessConfigs", res.getErrorDetails());
                throw new GetCloudParameterException("Failed to get access configs for the cloud provider. "
                        + getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getCloudAccessConfigs();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform accessConfigs", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudEncryptionKeys getCloudEncryptionKeys(ExtendedCloudCredential cloudCredential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform encryptionKeys");
        GetPlatformEncryptionKeysRequest getPlatformEncryptionKeysRequest =
                new GetPlatformEncryptionKeysRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformEncryptionKeysRequest.selector(), Event.wrap(getPlatformEncryptionKeysRequest));
        try {
            GetPlatformEncryptionKeysResult res = getPlatformEncryptionKeysRequest.await();
            LOGGER.debug("Platform encryptionKeys result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform encryptionKeys", res.getErrorDetails());
                throw new GetCloudParameterException("Failed to get encryption keys for the cloud provider. "
                        + getCauseMessages(res.getErrorDetails()), res.getErrorDetails());
            }
            return res.getCloudEncryptionKeys();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform encryptionKeys", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public Map<String, InstanceGroupParameterResponse> getInstanceGroupParameters(ExtendedCloudCredential cloudCredential,
            Set<InstanceGroupParameterRequest> instanceGroups) {
        LOGGER.debug("Get platform instanceGroupParameters");

        GetPlatformInstanceGroupParameterRequest getPlatformInstanceGroupParameterRequest =
                new GetPlatformInstanceGroupParameterRequest(cloudCredential, cloudCredential, instanceGroups, null);
        eventBus.notify(getPlatformInstanceGroupParameterRequest.selector(), Event.wrap(getPlatformInstanceGroupParameterRequest));
        try {
            GetPlatformInstanceGroupParameterResult res = getPlatformInstanceGroupParameterRequest.await();
            LOGGER.debug("Platform instanceGroupParameterResult result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform instanceGroupParameterResult", res.getErrorDetails());
                throw new GetCloudParameterException(String.format("Failed to get instance group parameters for the cloud provider: %s. %s",
                        res.getStatusReason(), getCauseMessages(res.getErrorDetails())), res.getErrorDetails());
            }
            return res.getInstanceGroupParameterResponses();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform instanceGroupParameterResult", e);
            throw new OperationException(e);
        }
    }

    @Retryable(value = GetCloudParameterException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public CloudNoSqlTables getNoSqlTables(ExtendedCloudCredential cloudCredential, String region, String platformVariant, Map<String, String> filters) {
        LOGGER.debug("Get platform noSqlTables");

        GetPlatformNoSqlTablesRequest request = new GetPlatformNoSqlTablesRequest(cloudCredential, cloudCredential, platformVariant, region, null);
        eventBus.notify(request.selector(), Event.wrap(request));
        try {
            GetPlatformNoSqlTablesResult result = request.await();
            LOGGER.debug("Platform NoSqlTablesResult result: {}", result);
            if (result.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to get platform NoSqlTablesResult", result.getErrorDetails());
                throw new GetCloudParameterException(String.format("Failed to get NoSQL tables for the cloud provider: %s. %s",
                        result.getStatusReason(), getCauseMessages(result.getErrorDetails())), result.getErrorDetails());
            }
            return result.getNoSqlTables();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform NoSqlTablesResult", e);
            throw new OperationException(e);
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
