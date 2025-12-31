package com.sequenceiq.environment.platformresource;

import static com.sequenceiq.cloudbreak.cloud.CloudParameterConst.ACCESS_CONFIG_TYPE;
import static com.sequenceiq.cloudbreak.cloud.CloudParameterConst.DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.cloud.CloudParameterConst.SHARED_PROJECT_ID;
import static com.sequenceiq.common.model.CredentialType.AUDIT;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetCdpPlatformRegionsRequest;
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
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigTypeQueryParam;
import com.sequenceiq.environment.credential.domain.Credential;
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

    public GetCdpPlatformRegionsRequest getCdpPlatformRegionsRequestV2(String cloudPlatform, String platformVariant) {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(cloudPlatform)
                .withVariant(platformVariant)
                .build();
        return new GetCdpPlatformRegionsRequest(cloudContext);
    }

    public PlatformResourceRequest getPlatformResourceRequestByEnvironment(
            String accountId,
            String environmentCrn,
            String platformVariant,
            String sharedProjectId) {
        return getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                null,
                platformVariant,
                null,
                sharedProjectId,
                null,
                null,
                CdpResourceType.DEFAULT);
    }

    public PlatformResourceRequest getPlatformResourceRequest(
            String accountId,
            String credentialName,
            String credentialCrn,
            String platformVariant,
            CdpResourceType cdpResourceType) {
        return getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                null,
                platformVariant,
                null,
                null,
                new HashMap<>(),
                null,
                null,
                cdpResourceType);
    }

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
                null,
                new HashMap<>(),
                null,
                null,
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
        return getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                new HashMap<>(),
                null,
                null,
                cdpResourceType);
    }

    public PlatformResourceRequest getPlatformResourceRequest(
            String accountId,
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            String sharedProjectId) {
        return getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                sharedProjectId,
                new HashMap<>(),
                null,
                null,
                CdpResourceType.DEFAULT);
    }

    public PlatformResourceRequest getPlatformResourceRequestByEnvironment(
            String accountId,
            String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            String sharedProjectId,
            DatabaseCapabilityType databaseType) {
        return getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                sharedProjectId,
                null,
                databaseType,
                CdpResourceType.DEFAULT);
    }

    public PlatformResourceRequest getPlatformResourceRequest(
            String accountId,
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            String sharedProjectId,
            AccessConfigTypeQueryParam accessConfigType) {
        return getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                sharedProjectId,
                new HashMap<>(),
                accessConfigType,
                null,
                CdpResourceType.DEFAULT);
    }

    public PlatformResourceRequest getPlatformResourceRequestByEnvironment(
            String accountId,
            String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            String sharedProjectId,
            AccessConfigTypeQueryParam accessConfigType) {
        return getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                sharedProjectId,
                accessConfigType,
                null,
                CdpResourceType.DEFAULT);
    }

    public PlatformResourceRequest getPlatformResourceRequestByEnvironment(
            String accountId,
            String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            String sharedProjectId,
            AccessConfigTypeQueryParam accessConfigType,
            DatabaseCapabilityType databaseType,
            CdpResourceType cdpResourceType) {
        String credentialCrn = credentialService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId, ENVIRONMENT).getResourceCrn();
        return getPlatformResourceRequest(
                accountId,
                null,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                sharedProjectId,
                new HashMap<>(),
                accessConfigType,
                databaseType,
                cdpResourceType);
    }

    public PlatformResourceRequest getPlatformResourceRequestByEnvironmentForVerticalScaling(
            String accountId,
            String environmentCrn,
            String region,
            CdpResourceType cdpResourceType) {
        Credential credential = credentialService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId, ENVIRONMENT);
        return getPlatformResourceRequest(
                accountId,
                null,
                credential.getResourceCrn(),
                region,
                credential.getCloudPlatform(),
                null,
                null,
                new HashMap<>(),
                null,
                null,
                cdpResourceType == null ? CdpResourceType.DEFAULT : cdpResourceType);
    }

    public PlatformResourceRequest getPlatformResourceRequest(
            String accountId,
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            String sharedProjectId,
            Map<String, String> filter,
            AccessConfigTypeQueryParam accessConfigType,
            DatabaseCapabilityType databaseType,
            CdpResourceType cdpResourceType) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();

        if (!Strings.isNullOrEmpty(credentialName)) {
            Optional<Credential> credential = credentialService.getOptionalByNameForAccountId(credentialName, accountId, ENVIRONMENT);
            if (credential.isEmpty()) {
                credential = credentialService.getOptionalByNameForAccountId(credentialName, accountId, AUDIT);
            }
            platformResourceRequest.setCredential(credentialService.extractCredential(credential, credentialName));
        } else if (!Strings.isNullOrEmpty(credentialCrn)) {
            Optional<Credential> credential = credentialService.getOptionalByCrnForAccountId(credentialCrn, accountId, ENVIRONMENT);
            if (credential.isEmpty()) {
                credential = credentialService.getOptionalByCrnForAccountId(credentialCrn, accountId, AUDIT);
            }
            platformResourceRequest.setCredential(credentialService.extractCredential(credential, credentialCrn));
        } else {
            throw new BadRequestException("The credentialCrn or the credentialName must be specified in the request");
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
        if (filter != null) {
            initFilterMap(platformResourceRequest);
            for (Map.Entry<String, String> entry : filter.entrySet()) {
                platformResourceRequest.getFilters().put(entry.getKey(), entry.getValue());
            }
        }
        String accessConfigTypeString = NullUtil.getIfNotNull(accessConfigType, Enum::name);
        if (accessConfigTypeString != null) {
            initFilterMap(platformResourceRequest);
            platformResourceRequest.getFilters().put(ACCESS_CONFIG_TYPE, accessConfigTypeString);
        }

        String databaseTypeString = databaseType == null ? null : databaseType.name();
        if (databaseTypeString != null) {
            initFilterMap(platformResourceRequest);
            platformResourceRequest.getFilters().put(DATABASE_TYPE, databaseTypeString);
        }

        if (!Strings.isNullOrEmpty(sharedProjectId)) {
            initFilterMap(platformResourceRequest);
            platformResourceRequest.getFilters().put(SHARED_PROJECT_ID, sharedProjectId);
        }
        return platformResourceRequest;
    }

    public PlatformResourceRequest getRequirements(
            String accountId,
            String credentialName,
            String credentialCrn,
            String region) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        if (!Strings.isNullOrEmpty(credentialName)) {
            Optional<Credential> credential = credentialService.getOptionalByNameForAccountId(credentialName, accountId, ENVIRONMENT);
            if (credential.isEmpty()) {
                credential = credentialService.getOptionalByNameForAccountId(credentialName, accountId, AUDIT);
            }
            platformResourceRequest.setCredential(credentialService.extractCredential(credential, credentialName));
        } else if (!Strings.isNullOrEmpty(credentialCrn)) {
            Optional<Credential> credential = credentialService.getOptionalByCrnForAccountId(credentialCrn, accountId, ENVIRONMENT);
            if (credential.isEmpty()) {
                credential = credentialService.getOptionalByCrnForAccountId(credentialCrn, accountId, AUDIT);
            }
            platformResourceRequest.setCredential(credentialService.extractCredential(credential, credentialCrn));
        } else {
            throw new BadRequestException("The credentialCrn or the credentialName must be specified in the request");
        }
        platformResourceRequest.setRegion(region);
        platformResourceRequest.setCloudPlatform(platformResourceRequest.getCredential().getCloudPlatform());
        platformResourceRequest.setCdpResourceType(CdpResourceType.DEFAULT);
        platformResourceRequest.setFilters(Map.of());
        return platformResourceRequest;
    }

    public void initFilterMap(PlatformResourceRequest platformResourceRequest) {
        if (platformResourceRequest.getFilters() == null) {
            platformResourceRequest.setFilters(new HashMap<>());
        }
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

    public CloudDatabaseVmTypes getDatabaseVmTypesByCredential(PlatformResourceRequest request) {
        checkFieldIsNotEmpty(request.getRegion(), "region");
        return cloudParameterService.getDatabaseVmTypes(
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

    public CloudRegions getCdpRegions(GetCdpPlatformRegionsRequest request) {
        return cloudParameterService.getCdpRegions(
                request.getCloudContext().getPlatform().getValue(),
                request.getCloudContext().getVariant().getValue());
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

    public PlatformDatabaseCapabilities getDatabaseCapabilities(PlatformResourceRequest request) {
        return cloudParameterService.getDatabaseCapabilities(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudResourceGroups getResourceGroups(PlatformResourceRequest request) {
        return cloudParameterService.getResourceGroups(extendedCloudCredentialConverter.convert(request.getCredential()), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudPrivateDnsZones getPrivateDnsZones(PlatformResourceRequest request) {
        return cloudParameterService.getPrivateDnsZones(extendedCloudCredentialConverter.convert(request.getCredential()),
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

    public String getCredentialCrnByName(String credentialName) {
        return credentialService.getResourceCrnByResourceName(credentialName);
    }
}
