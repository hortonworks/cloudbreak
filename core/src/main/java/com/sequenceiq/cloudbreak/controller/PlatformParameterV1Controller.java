package com.sequenceiq.cloudbreak.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ConnectorV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;
import com.sequenceiq.cloudbreak.api.model.PlatformGatewaysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformImagesJson;
import com.sequenceiq.cloudbreak.api.model.PlatformIpPoolsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformNetworkResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformNetworksResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformOrchestratorsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformRegionsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformSecurityGroupResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeyResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVirtualMachinesJson;
import com.sequenceiq.cloudbreak.api.model.RecommendationRequestJson;
import com.sequenceiq.cloudbreak.api.model.RecommendationResponse;
import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.api.model.SpecialParametersJson;
import com.sequenceiq.cloudbreak.api.model.TagSpecificationsJson;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImages;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.CloudResourceAdvisor;

@Component
public class PlatformParameterV1Controller implements ConnectorV1Endpoint {

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private CloudResourceAdvisor cloudResourceAdvisor;

    @Override
    public Map<String, Object> getPlatforms(Boolean extended) {
        PlatformVariants pv = cloudParameterService.getPlatformVariants();
        PlatformDisks diskTypes = cloudParameterService.getDiskTypes();
        PlatformVirtualMachines vmtypes = cloudParameterService.getVmtypes(null, extended);
        PlatformRegions regions = cloudParameterService.getRegions();
        PlatformOrchestrators orchestrators = cloudParameterService.getOrchestrators();
        PlatformImages images = cloudParameterService.getImages();
        Map<Platform, PlatformParameters> platformParameters = cloudParameterService.getPlatformParameters();
        SpecialParameters specialParameters = cloudParameterService.getSpecialParameters();

        Map<String, Object> map = new HashMap<>();

        map.put("variants", conversionService.convert(pv, PlatformVariantsJson.class));
        map.put("disks", conversionService.convert(diskTypes, PlatformDisksJson.class));
        map.put("virtualMachines", conversionService.convert(vmtypes, PlatformVirtualMachinesJson.class));
        map.put("regions", conversionService.convert(regions, PlatformRegionsJson.class));
        map.put("orchestrators", conversionService.convert(orchestrators, PlatformOrchestratorsJson.class));
        map.put("images", conversionService.convert(images, PlatformImagesJson.class));
        map.put("tagspecifications", conversionService.convert(platformParameters, TagSpecificationsJson.class));
        map.put("specialParameters", conversionService.convert(specialParameters, SpecialParametersJson.class));

        return map;
    }

    @Override
    public PlatformVariantsJson getPlatformVariants() {
        PlatformVariants pv = cloudParameterService.getPlatformVariants();
        return conversionService.convert(pv, PlatformVariantsJson.class);
    }

    @Override
    public Collection<String> getPlatformVariantByType(String type) {
        PlatformVariants pv = cloudParameterService.getPlatformVariants();
        Collection<String> strings = conversionService.convert(pv, PlatformVariantsJson.class).getPlatformToVariants().get(type.toUpperCase());
        return strings == null ? new ArrayList<>() : strings;
    }

    @Override
    public PlatformDisksJson getDisktypes() {
        PlatformDisks dts = cloudParameterService.getDiskTypes();
        return conversionService.convert(dts, PlatformDisksJson.class);
    }

    @Override
    public Collection<String> getDisktypeByType(String type) {
        PlatformDisks diskTypes = cloudParameterService.getDiskTypes();
        Collection<String> strings = conversionService.convert(diskTypes, PlatformDisksJson.class)
                .getDiskTypes().get(type.toUpperCase());
        return strings == null ? new ArrayList<>() : strings;
    }

    @Override
    public PlatformOrchestratorsJson getOrchestratortypes() {
        PlatformOrchestrators orchestrators = cloudParameterService.getOrchestrators();
        return conversionService.convert(orchestrators, PlatformOrchestratorsJson.class);
    }

    @Override
    public Collection<String> getOchestratorsByType(String type) {
        PlatformOrchestrators orchestrators = cloudParameterService.getOrchestrators();
        Collection<String> strings = conversionService.convert(orchestrators, PlatformOrchestratorsJson.class)
                .getOrchestrators().get(type.toUpperCase());
        return strings == null ? new ArrayList<>() : strings;
    }

    @Override
    public PlatformVirtualMachinesJson getVmTypes(Boolean extended) {
        PlatformVirtualMachines vmtypes = cloudParameterService.getVmtypes(null, extended);
        return conversionService.convert(vmtypes, PlatformVirtualMachinesJson.class);
    }

    @Override
    public PlatformVirtualMachinesJson getVmTypes(String type, Boolean extended) {
        PlatformVirtualMachines vmtypes = cloudParameterService.getVmtypes(type.toUpperCase(), extended);
        return conversionService.convert(vmtypes, PlatformVirtualMachinesJson.class);
    }

    @Override
    public PlatformRegionsJson getRegions() {
        PlatformRegions pv = cloudParameterService.getRegions();
        return conversionService.convert(pv, PlatformRegionsJson.class);
    }

    @Override
    public Collection<String> getRegionRByType(String type) {
        PlatformRegions pv = cloudParameterService.getRegions();
        Collection<String> regions = conversionService.convert(pv, PlatformRegionsJson.class)
                .getRegions().get(type.toUpperCase());
        return regions == null ? new ArrayList<>() : regions;
    }

    @Override
    public Map<String, Collection<String>> getRegionAvByType(String type) {
        PlatformRegions pv = cloudParameterService.getRegions();
        Map<String, Collection<String>> azs = conversionService.convert(pv, PlatformRegionsJson.class)
                .getAvailabilityZones().get(type.toUpperCase());
        return azs == null ? new HashMap<>() : azs;
    }

    @Override
    public Map<String, String> getImagesByType(String type) {
        PlatformImages pv = cloudParameterService.getImages();
        Map<String, String> images = conversionService.convert(pv, PlatformImagesJson.class)
                .getImages().get(type.toUpperCase());
        return images == null ? new HashMap<>() : images;
    }

    @Override
    public PlatformImagesJson getImages() {
        PlatformImages pv = cloudParameterService.getImages();
        return conversionService.convert(pv, PlatformImagesJson.class);
    }

    @Override
    public TagSpecificationsJson getTagSpecifications() {
        Map<Platform, PlatformParameters> platformParameters = cloudParameterService.getPlatformParameters();
        return conversionService.convert(platformParameters, TagSpecificationsJson.class);
    }

    @Override
    public Map<String, Boolean> getSpecialProperties() {
        return cloudParameterService.getSpecialParameters().getSpecialParameters();
    }

    @Override
    public RecommendationResponse createRecommendation(RecommendationRequestJson recommendationRequestJson) {
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        PlatformResourceRequest resourceRequest = conversionService.convert(recommendationRequestJson, PlatformResourceRequest.class);
        if (recommendationRequestJson.getBlueprintId() == null && Strings.isNullOrEmpty(recommendationRequestJson.getBlueprintName())) {
            fieldIsNotEmpty(recommendationRequestJson.getBlueprintId(), "blueprintId");
        }
        fieldIsNotEmpty(resourceRequest.getRegion(), "region");
        fieldIsNotEmpty(resourceRequest.getAvailabilityZone(), "availabilityZone");
        PlatformRecommendation recommendedVms =
                cloudResourceAdvisor.createForBlueprint(recommendationRequestJson.getBlueprintName(), recommendationRequestJson.getBlueprintId(),
                        resourceRequest, cbUser);
        return conversionService.convert(recommendedVms, RecommendationResponse.class);
    }

    @Override
    public Map<String, Set<PlatformNetworkResponse>> getCloudNetworks(PlatformResourceRequestJson resourceRequestJson) {
        resourceRequestJson = prepareAccountAndOwner(resourceRequestJson, authenticatedUserService.getCbUser());
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);
        CloudNetworks cloudNetworks = cloudParameterService.getCloudNetworks(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(cloudNetworks, PlatformNetworksResponse.class).getNetworks();
    }

    @Override
    public Map<String, Set<PlatformSshKeyResponse>> getCloudSshKeys(PlatformResourceRequestJson resourceRequestJson) {
        resourceRequestJson = prepareAccountAndOwner(resourceRequestJson, authenticatedUserService.getCbUser());
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);
        CloudSshKeys cloudSshKeys = cloudParameterService.getCloudSshKeys(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(cloudSshKeys, PlatformSshKeysResponse.class).getSshKeys();
    }

    @Override
    public Map<String, Set<PlatformSecurityGroupResponse>> getSecurityGroups(PlatformResourceRequestJson resourceRequestJson) {
        resourceRequestJson = prepareAccountAndOwner(resourceRequestJson, authenticatedUserService.getCbUser());
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);
        CloudSecurityGroups securityGroups = cloudParameterService.getSecurityGroups(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(securityGroups, PlatformSecurityGroupsResponse.class).getSecurityGroups();
    }

    @Override
    public PlatformGatewaysResponse getGatewaysCredentialId(PlatformResourceRequestJson resourceRequestJson) {
        resourceRequestJson = prepareAccountAndOwner(resourceRequestJson, authenticatedUserService.getCbUser());
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);

        CloudGateWays cloudGateWays = cloudParameterService.getGateways(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(cloudGateWays, PlatformGatewaysResponse.class);
    }

    @Override
    public PlatformIpPoolsResponse getIpPoolsCredentialId(PlatformResourceRequestJson resourceRequestJson) {
        resourceRequestJson = prepareAccountAndOwner(resourceRequestJson, authenticatedUserService.getCbUser());
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);

        CloudIpPools cloudIpPools = cloudParameterService.getPublicIpPools(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(cloudIpPools, PlatformIpPoolsResponse.class);
    }

    private PlatformResourceRequestJson prepareAccountAndOwner(PlatformResourceRequestJson resourceRequestJson, IdentityUser user) {
        resourceRequestJson.setAccount(user.getAccount());
        resourceRequestJson.setOwner(user.getUserId());
        return resourceRequestJson;
    }

    private void fieldIsNotEmpty(Object field, String fieldName) {
        if (StringUtils.isEmpty(field)) {
            throw new BadRequestException(String.format("The '%s' request body field is mandatory for recommendation creation.", fieldName));
        }
    }
}
