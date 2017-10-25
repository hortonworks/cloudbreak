package com.sequenceiq.cloudbreak.service.stack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudGatewaysRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudGatewaysResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudIpPoolsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudIpPoolsResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformImagesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformImagesResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformInstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformInstanceGroupParameterResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNetworksRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNetworksResult;
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
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParametersRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParametersResult;
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
import com.sequenceiq.cloudbreak.cloud.model.PlatformImages;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class CloudParameterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudParameterService.class);

    @Value("${cb.enable.custom.image:false}")
    private Boolean enableCustomImage;

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    public PlatformVariants getPlatformVariants() {
        LOGGER.debug("Get platform variants");
        GetPlatformVariantsRequest getPlatformVariantsRequest = new GetPlatformVariantsRequest();
        eventBus.notify(getPlatformVariantsRequest.selector(), eventFactory.createEvent(getPlatformVariantsRequest));
        try {
            GetPlatformVariantsResult res = getPlatformVariantsRequest.await();
            LOGGER.info("Platform variants result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform variants", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
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

    public PlatformDisks getDiskTypes() {
        LOGGER.debug("Get platform disktypes");
        GetDiskTypesRequest getDiskTypesRequest = new GetDiskTypesRequest();
        eventBus.notify(getDiskTypesRequest.selector(), eventFactory.createEvent(getDiskTypesRequest));
        try {
            GetDiskTypesResult res = getDiskTypesRequest.await();
            LOGGER.info("Platform disk types result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform disk types", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformDisks();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform disk types", e);
            throw new OperationException(e);
        }
    }

    public PlatformVirtualMachines getVmtypes(String type, Boolean extended) {
        if (extended == null) {
            extended = true;
        }
        LOGGER.debug("Get platform vm types");
        GetVirtualMachineTypesRequest getVirtualMachineTypesRequest = new GetVirtualMachineTypesRequest(type, extended);
        eventBus.notify(getVirtualMachineTypesRequest.selector(), eventFactory.createEvent(getVirtualMachineTypesRequest));
        try {
            GetVirtualMachineTypesResult res = getVirtualMachineTypesRequest.await();
            LOGGER.info("Platform vm types result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform vm types", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformVirtualMachines();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform vm types", e);
            throw new OperationException(e);
        }
    }

    public PlatformRegions getRegions() {
        LOGGER.debug("Get platform regions");
        GetPlatformRegionsRequest getPlatformRegionsRequest = new GetPlatformRegionsRequest();
        eventBus.notify(getPlatformRegionsRequest.selector(), eventFactory.createEvent(getPlatformRegionsRequest));
        try {
            GetPlatformRegionsResult res = getPlatformRegionsRequest.await();
            LOGGER.info("Platform regions result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform regions", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformRegions();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform regions", e);
            throw new OperationException(e);
        }
    }

    public PlatformOrchestrators getOrchestrators() {
        LOGGER.debug("Get platform orchestrators");
        GetPlatformOrchestratorsRequest getPlatformOrchestratorsRequest = new GetPlatformOrchestratorsRequest();
        eventBus.notify(getPlatformOrchestratorsRequest.selector(), eventFactory.createEvent(getPlatformOrchestratorsRequest));
        try {
            GetPlatformOrchestratorsResult res = getPlatformOrchestratorsRequest.await();
            LOGGER.info("Platform orchestrators result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform orchestrators", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformOrchestrators();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform orchestrators", e);
            throw new OperationException(e);
        }
    }

    public PlatformImages getImages() {
        LOGGER.debug("Get platform orchestrators");
        GetPlatformImagesRequest getPlatformImagesRequest = new GetPlatformImagesRequest();
        eventBus.notify(getPlatformImagesRequest.selector(), eventFactory.createEvent(getPlatformImagesRequest));
        try {
            GetPlatformImagesResult res = getPlatformImagesRequest.await();
            LOGGER.info("Platform images result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform images", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformImages();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform images", e);
            throw new OperationException(e);
        }
    }

    public Map<Platform, PlatformParameters> getPlatformParameters() {
        LOGGER.debug("Get platform parameters for");
        PlatformParametersRequest parametersRequest = new PlatformParametersRequest();
        eventBus.notify(parametersRequest.selector(), eventFactory.createEvent(parametersRequest));
        try {
            PlatformParametersResult res = parametersRequest.await();
            LOGGER.info("Platform parameter result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform parameters", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformParameters();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting platform parameters", e);
            throw new OperationException(e);
        }
    }

    public CloudNetworks getCloudNetworks(Credential credential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform cloudnetworks");

        ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
        GetPlatformNetworksRequest getPlatformNetworksRequest =
                new GetPlatformNetworksRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformNetworksRequest.selector(), eventFactory.createEvent(getPlatformNetworksRequest));
        try {
            GetPlatformNetworksResult res = getPlatformNetworksRequest.await();
            LOGGER.info("Platform networks types result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform networks", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getCloudNetworks();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform networks", e);
            throw new OperationException(e);
        }
    }

    public CloudSshKeys getCloudSshKeys(Credential credential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform sshkeys");
        ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
        GetPlatformSshKeysRequest getPlatformSshKeysRequest =
                new GetPlatformSshKeysRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformSshKeysRequest.selector(), eventFactory.createEvent(getPlatformSshKeysRequest));
        try {
            GetPlatformSshKeysResult res = getPlatformSshKeysRequest.await();
            LOGGER.info("Platform sshkeys types result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform sshkeys", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getCloudSshKeys();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform sshkeys", e);
            throw new OperationException(e);
        }
    }

    public CloudSecurityGroups getSecurityGroups(Credential credential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform securitygroups");

        ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
        GetPlatformSecurityGroupsRequest getPlatformSecurityGroupsRequest =
                new GetPlatformSecurityGroupsRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformSecurityGroupsRequest.selector(), eventFactory.createEvent(getPlatformSecurityGroupsRequest));
        try {
            GetPlatformSecurityGroupsResult res = getPlatformSecurityGroupsRequest.await();
            LOGGER.info("Platform securitygroups types result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform securitygroups", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
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
        specialParameters.put("enableCustomImage", enableCustomImage);
        return new SpecialParameters(specialParameters);
    }

    public VmRecommendations getRecommendation(String platform) {
        LOGGER.debug("Get platform vm recommendation");
        GetVirtualMachineRecommendtaionRequest getVirtualMachineRecommendtaionRequest = new GetVirtualMachineRecommendtaionRequest(platform);
        eventBus.notify(getVirtualMachineRecommendtaionRequest.selector(), eventFactory.createEvent(getVirtualMachineRecommendtaionRequest));
        try {
            GetVirtualMachineRecommendationResponse res = getVirtualMachineRecommendtaionRequest.await();
            LOGGER.info("Platform vm recommendation result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform vm recommendation", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getRecommendations();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform vm recommendation", e);
            throw new OperationException(e);
        }
    }

    public CloudVmTypes getVmTypesV2(Credential credential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform vmtypes");
        ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
        GetPlatformVmTypesRequest getPlatformVmTypesRequest =
                new GetPlatformVmTypesRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformVmTypesRequest.selector(), Event.wrap(getPlatformVmTypesRequest));
        try {
            GetPlatformVmTypesResult res = getPlatformVmTypesRequest.await();
            LOGGER.info("Platform vmtypes result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform vmtypes", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getCloudVmTypes();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform vmtypes", e);
            throw new OperationException(e);
        }
    }

    public CloudRegions getRegionsV2(Credential credential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform regions");
        ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
        GetPlatformRegionsRequestV2 getPlatformRegionsRequest =
                new GetPlatformRegionsRequestV2(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformRegionsRequest.selector(), Event.wrap(getPlatformRegionsRequest));
        try {
            GetPlatformRegionsResultV2 res = getPlatformRegionsRequest.await();
            LOGGER.info("Platform regions result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform regions", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getCloudRegions();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform regions", e);
            throw new OperationException(e);
        }
    }

    public CloudGateWays getGateways(Credential credential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform gateways");
        ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
        GetPlatformCloudGatewaysRequest getPlatformCloudGatewaysRequest =
                new GetPlatformCloudGatewaysRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformCloudGatewaysRequest.selector(), Event.wrap(getPlatformCloudGatewaysRequest));
        try {
            GetPlatformCloudGatewaysResult res = getPlatformCloudGatewaysRequest.await();
            LOGGER.info("Platform gateways result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform gateways", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getCloudGateWays();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform gateways", e);
            throw new OperationException(e);
        }
    }

    public CloudIpPools getPublicIpPools(Credential credential, String region, String variant, Map<String, String> filters) {
        LOGGER.debug("Get platform publicIpPools");
        ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);
        GetPlatformCloudIpPoolsRequest getPlatformCloudIpPoolsRequest =
                new GetPlatformCloudIpPoolsRequest(cloudCredential, cloudCredential, variant, region, filters);
        eventBus.notify(getPlatformCloudIpPoolsRequest.selector(), Event.wrap(getPlatformCloudIpPoolsRequest));
        try {
            GetPlatformCloudIpPoolsResult res = getPlatformCloudIpPoolsRequest.await();
            LOGGER.info("Platform publicIpPools result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform publicIpPools", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getCloudIpPools();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform publicIpPools", e);
            throw new OperationException(e);
        }
    }

    public Map<String, InstanceGroupParameterResponse> getInstanceGroupParameters(Credential credential, Set<InstanceGroupParameterRequest> instanceGroups) {
        LOGGER.debug("Get platform getInstanceGroupParameters");
        ExtendedCloudCredential cloudCredential = credentialToExtendedCloudCredentialConverter.convert(credential);

        GetPlatformInstanceGroupParameterRequest getPlatformInstanceGroupParameterRequest =
                new GetPlatformInstanceGroupParameterRequest(cloudCredential, cloudCredential, instanceGroups, null);
        eventBus.notify(getPlatformInstanceGroupParameterRequest.selector(), Event.wrap(getPlatformInstanceGroupParameterRequest));
        try {
            GetPlatformInstanceGroupParameterResult res = getPlatformInstanceGroupParameterRequest.await();
            LOGGER.info("Platform instanceGroupParameterResult result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform instanceGroupParameterResult", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getInstanceGroupParameterResponses();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform publicIpPools", e);
            throw new OperationException(e);
        }
    }

}
