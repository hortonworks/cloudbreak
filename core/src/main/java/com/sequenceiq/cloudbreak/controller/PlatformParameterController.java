package com.sequenceiq.cloudbreak.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.ConnectorEndpoint;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;
import com.sequenceiq.cloudbreak.api.model.PlatformImagesJson;
import com.sequenceiq.cloudbreak.api.model.PlatformNetworksResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformOrchestratorsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformRegionsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVirtualMachinesJson;
import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.api.model.SpecialParametersJson;
import com.sequenceiq.cloudbreak.api.model.TagSpecificationsJson;
import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImages;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

@Component
public class PlatformParameterController implements ConnectorEndpoint {

    @Autowired
    private CloudParameterService cloudParameterService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public Map<String, JsonEntity> getPlatforms(Boolean extended) {
        PlatformVariants pv = cloudParameterService.getPlatformVariants();
        PlatformDisks diskTypes = cloudParameterService.getDiskTypes();
        PlatformVirtualMachines vmtypes = cloudParameterService.getVmtypes(extended);
        PlatformRegions regions = cloudParameterService.getRegions();
        PlatformOrchestrators orchestrators = cloudParameterService.getOrchestrators();
        PlatformImages images = cloudParameterService.getImages();
        Map<Platform, PlatformParameters> platformParameters = cloudParameterService.getPlatformParameters();
        SpecialParameters specialParameters = cloudParameterService.getSpecialParameters();

        Map<String, JsonEntity> map = new HashMap<>();

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
        PlatformVirtualMachines vmtypes = cloudParameterService.getVmtypes(extended);
        return conversionService.convert(vmtypes, PlatformVirtualMachinesJson.class);
    }

    @Override
    public Collection<VmTypeJson> getVmTypeByType(String type, Boolean extended) {
        PlatformVirtualMachines vmtypes = cloudParameterService.getVmtypes(extended);
        Collection<VmTypeJson> vmTypes = conversionService.convert(vmtypes, PlatformVirtualMachinesJson.class)
                .getVirtualMachines().get(type.toUpperCase());
        return vmTypes == null ? new ArrayList<>() : vmTypes;
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
    public PlatformNetworksResponse getCloudNetworks(PlatformResourceRequestJson resourceRequestJson) {
        resourceRequestJson = prepareAccountAndOwner(resourceRequestJson, authenticatedUserService.getCbUser());
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);
        CloudNetworks cloudNetworks = cloudParameterService.getCloudNetworks(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(cloudNetworks, PlatformNetworksResponse.class);
    }

    @Override
    public PlatformSshKeysResponse getCloudSshKeys(PlatformResourceRequestJson resourceRequestJson) {
        resourceRequestJson = prepareAccountAndOwner(resourceRequestJson, authenticatedUserService.getCbUser());
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);
        CloudSshKeys cloudSshKeys = cloudParameterService.getCloudSshKeys(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(cloudSshKeys, PlatformSshKeysResponse.class);

    }

    @Override
    public PlatformSecurityGroupsResponse getSecurityGroups(PlatformResourceRequestJson resourceRequestJson) {
        resourceRequestJson = prepareAccountAndOwner(resourceRequestJson, authenticatedUserService.getCbUser());
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);
        CloudSecurityGroups securityGroups = cloudParameterService.getSecurityGroups(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(securityGroups, PlatformSecurityGroupsResponse.class);
    }

    private PlatformResourceRequestJson prepareAccountAndOwner(PlatformResourceRequestJson resourceRequestJson, IdentityUser user) {
        resourceRequestJson.setAccount(user.getAccount());
        resourceRequestJson.setOwner(user.getUserId());
        return resourceRequestJson;
    }

}
