package com.sequenceiq.environment.platformresource;

import static com.sequenceiq.cloudbreak.cloud.service.CloudParameterService.ACCESS_CONFIG_TYPE;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigTypeQueryParam;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToExtendedCloudCredentialConverter;

@Service
public class PlatformParameterService {

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    public PlatformResourceRequest getPlatformResourceRequest(
            String accountId,
            String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        return getPlatformResourceRequest(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                CdpResourceType.DEFAULT);
    }

    public PlatformResourceRequest getPlatformResourceRequest(
            String accountId,
            String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            AccessConfigTypeQueryParam accessConfigType) {
        return getPlatformResourceRequest(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                accessConfigType,
                CdpResourceType.DEFAULT);
    }

    //CHECKSTYLE:OFF
    public PlatformResourceRequest getPlatformResourceRequest(
            String accountId,
            String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            AccessConfigTypeQueryParam accessConfigType,
            CdpResourceType cdpResourceType) {
    //CHECKSTYLE:ON
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();

        if (!Strings.isNullOrEmpty(environmentCrn)) {
            platformResourceRequest.setCredential(credentialService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId, ENVIRONMENT));
        } else {
            throw new BadRequestException("The environmentCrn must be specified in the request");
        }

        if (!Strings.isNullOrEmpty(platformVariant)) {
            platformResourceRequest.setCloudPlatform(platformResourceRequest.getCredential().getCloudPlatform());
        } else {
            platformResourceRequest.setPlatformVariant(
                    Strings.isNullOrEmpty(platformVariant) ? platformResourceRequest.getCredential().getCloudPlatform() : platformVariant);
        }
        platformResourceRequest.setCdpResourceType(Objects.requireNonNullElse(cdpResourceType, CdpResourceType.DEFAULT));
        platformResourceRequest.setRegion(region);
        platformResourceRequest.setCloudPlatform(platformResourceRequest.getCredential().getCloudPlatform());
        if (!Strings.isNullOrEmpty(availabilityZone)) {
            platformResourceRequest.setAvailabilityZone(availabilityZone);
        }
        String accessConfigTypeString = NullUtil.getIfNotNull(accessConfigType, Enum::name);
        if (accessConfigTypeString != null) {
            platformResourceRequest.setFilters(Map.of(ACCESS_CONFIG_TYPE, accessConfigTypeString));
        }
        return platformResourceRequest;
    }

    public CloudVmTypes getVmTypesByCredential(PlatformResourceRequest request) {
        checkFieldIsNotEmpty(request.getRegion(), "region");
        return cloudParameterService.getVmTypesV2(
                extendedCloudCredentialConverter.convert(request.getCredential()),
                request.getRegion(),
                request.getPlatformVariant(),
                request.getCdpResourceType(),
                request.getFilters());
    }

    public CloudRegions getRegionsByCredential(PlatformResourceRequest request, boolean availabilityZonesNeeded) {
        return cloudParameterService.getRegionsV2(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters(), availabilityZonesNeeded);
    }

    public PlatformDisks getDiskTypes() {
        return cloudParameterService.getDiskTypes();
    }

    public CloudNetworks getCloudNetworks(PlatformResourceRequest request) {
        return cloudParameterService.getCloudNetworks(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudIpPools getIpPoolsCredentialId(PlatformResourceRequest request) {
        return cloudParameterService.getPublicIpPools(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudGateWays getGatewaysCredentialId(PlatformResourceRequest request) {
        return cloudParameterService.getGateways(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudEncryptionKeys getEncryptionKeys(PlatformResourceRequest request) {
        return cloudParameterService.getCloudEncryptionKeys(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudSecurityGroups getSecurityGroups(PlatformResourceRequest request) {
        return cloudParameterService.getSecurityGroups(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudSshKeys getCloudSshKeys(PlatformResourceRequest request) {
        return cloudParameterService.getCloudSshKeys(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudAccessConfigs getAccessConfigs(PlatformResourceRequest request) {
        return cloudParameterService.getCloudAccessConfigs(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudNoSqlTables getNoSqlTables(PlatformResourceRequest request) {
        return cloudParameterService.getNoSqlTables(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudResourceGroups getResourceGroups(PlatformResourceRequest request) {
        return cloudParameterService.getResourceGroups(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    private void checkFieldIsNotEmpty(Object field, String param) {
        if (StringUtils.isEmpty(field)) {
            throw new BadRequestException(String.format("The '%s' request body field is mandatory for recommendation creation.", param));
        }
    }

    public Map<Platform, PlatformParameters> getPlatformParameters() {
        return cloudParameterService.getPlatformParameters();
    }
}
