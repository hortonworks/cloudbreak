package com.sequenceiq.environment.platformresource;

import java.util.Map;

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
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.common.api.type.CdpResourceType;
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
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        return getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                CdpResourceType.DEFAULT);
    }

    public PlatformResourceRequest getPlatformResourceRequest(
            String accountId,
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            CdpResourceType cdpResourceType) {

        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();

        if (!Strings.isNullOrEmpty(credentialName)) {
            platformResourceRequest.setCredential(credentialService.getByNameForAccountId(credentialName, accountId));
        } else if (!Strings.isNullOrEmpty(credentialCrn)) {
            platformResourceRequest.setCredential(credentialService.getByCrnForAccountId(credentialCrn, accountId));
        } else {
            throw new BadRequestException("The credentialId or the credentialName must be specified in the request");
        }

        if (!Strings.isNullOrEmpty(platformVariant)) {
            platformResourceRequest.setCloudPlatform(platformResourceRequest.getCredential().getCloudPlatform());
        } else {
            platformResourceRequest.setPlatformVariant(
                    Strings.isNullOrEmpty(platformVariant) ? platformResourceRequest.getCredential().getCloudPlatform() : platformVariant);
        }
        if (cdpResourceType == null) {
            platformResourceRequest.setCdpResourceType(CdpResourceType.DEFAULT);
        } else {
            platformResourceRequest.setCdpResourceType(cdpResourceType);
        }
        platformResourceRequest.setRegion(region);
        platformResourceRequest.setCloudPlatform(platformResourceRequest.getCredential().getCloudPlatform());
        if (!Strings.isNullOrEmpty(availabilityZone)) {
            platformResourceRequest.setAvailabilityZone(availabilityZone);
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

    public CloudRegions getRegionsByCredential(PlatformResourceRequest request) {
        return cloudParameterService.getRegionsV2(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
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

    private void checkFieldIsNotEmpty(Object field, String param) {
        if (StringUtils.isEmpty(field)) {
            throw new BadRequestException(String.format("The '%s' request body field is mandatory for recommendation creation.", param));
        }
    }

    public Map<Platform, PlatformParameters> getPlatformParameters() {
        return cloudParameterService.getPlatformParameters();
    }
}
