package com.sequenceiq.environment.environment.dto;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;
import static com.sequenceiq.environment.environment.EnvironmentDeletionType.NONE;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.DefaultComputeCluster;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto.Builder;
import com.sequenceiq.environment.environment.dto.credential.CredentialDetailsConverter;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaInstanceCountByGroupProvider;
import com.sequenceiq.environment.environment.service.recipe.EnvironmentRecipeService;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;

@Component
public class EnvironmentDtoConverter {

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap;

    private final AuthenticationDtoConverter authenticationDtoConverter;

    private final EnvironmentRecipeService environmentRecipeService;

    private final FreeIpaInstanceCountByGroupProvider ipaInstanceCountByGroupProvider;

    private final CredentialDetailsConverter credentialDetailsConverter;

    private final EnvironmentTagsDtoConverter environmentTagsDtoConverter;

    private final EncryptionProfileDtoConverter encryptionProfileDtoConverter;

    public EnvironmentDtoConverter(Map<CloudPlatform,
                    EnvironmentNetworkConverter> environmentNetworkConverterMap,
            Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap,
            AuthenticationDtoConverter authenticationDtoConverter,
            EnvironmentRecipeService environmentRecipeService,
            FreeIpaInstanceCountByGroupProvider ipaInstanceCountByGroupProvider,
            CredentialDetailsConverter credentialDetailsConverter,
            EnvironmentTagsDtoConverter environmentTagsDtoConverter,
            EncryptionProfileDtoConverter encryptionProfileDtoConverter) {
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.environmentParamsConverterMap = environmentParamsConverterMap;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.environmentRecipeService = environmentRecipeService;
        this.ipaInstanceCountByGroupProvider = ipaInstanceCountByGroupProvider;
        this.credentialDetailsConverter = credentialDetailsConverter;
        this.environmentTagsDtoConverter = environmentTagsDtoConverter;
        this.encryptionProfileDtoConverter = encryptionProfileDtoConverter;
    }

    public EnvironmentViewDto environmentViewToViewDto(EnvironmentView environmentView) {
        EnvironmentViewDto.Builder builder = EnvironmentViewDto.builder()
                .withId(environmentView.getId())
                .withResourceCrn(environmentView.getResourceCrn())
                .withName(environmentView.getName())
                .withOriginalName(environmentView.getOriginalName())
                .withDescription(environmentView.getDescription())
                .withAccountId(environmentView.getAccountId())
                .withArchived(environmentView.isArchived())
                .withCloudPlatform(environmentView.getCloudPlatform())
                .withCredentialView(environmentView.getCredential())
                .withDeletionTimestamp(environmentView.getDeletionTimestamp())
                .withLocationDto(environmentViewToLocationDto(environmentView))
                .withRegions(environmentView.getRegionSet())
                .withTelemetry(environmentView.getTelemetry())
                .withBackup(environmentView.getBackup())
                .withEnvironmentStatus(environmentView.getStatus())
                .withCreator(environmentView.getCreator())
                .withAuthentication(authenticationDtoConverter.authenticationToDto(environmentView.getAuthentication()))
                .withFreeIpaCreation(environmentViewToFreeIpaCreationDto(environmentView))
                .withCreated(environmentView.getCreated())
                .withStatusReason(environmentView.getStatusReason())
                .withExperimentalFeatures(environmentView.getExperimentalFeaturesJson())
                .withTags(environmentView.getEnvironmentTags())
                .withSecurityAccess(environmentToSecurityAccessDto(environmentView.getCidr(), environmentView.getSecurityGroupIdForKnox(),
                        environmentView.getDefaultSecurityGroupId()))
                .withAdminGroupName(environmentView.getAdminGroupName())
                .withProxyConfig(environmentView.getProxyConfig())
                .withEnvironmentDeletionType(environmentView.getDeletionType())
                .withEnvironmentServiceVersion(environmentView.getEnvironmentServiceVersion())
                .withEnvironmentDomain(environmentView.getDomain())
                .withEnableSecretEncryption(environmentView.isEnableSecretEncryption())
                .withEnableComputeCluster(isComputeClusterEnabled(environmentView.getDefaultComputeCluster()))
                .withEnvironmentType(environmentView.getEnvironmentType())
                .withRemoteEnvironmentCrn(environmentView.getRemoteEnvironmentCrn())
                .withEncryptionProfileCrn(environmentView.getEncryptionProfileCrn());

        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environmentView.getCloudPlatform());
        doIfNotNull(environmentView.getParameters(), parameters -> builder.withParameters(
                environmentParamsConverterMap.get(cloudPlatform).convertToDto(parameters)));
        doIfNotNull(environmentView.getNetwork(), network -> builder.withNetwork(
                environmentNetworkConverterMap.get(cloudPlatform).convertToDto(network)));
        doIfNotNull(environmentView.getParentEnvironment(), parentEnvironment -> builder
                .withParentEnvironmentCrn(parentEnvironment.getResourceCrn())
                .withParentEnvironmentName(parentEnvironment.getName())
                .withParentEnvironmentCloudPlatform(parentEnvironment.getCloudPlatform()));
        return builder.build();
    }

    public EnvironmentDto environmentToDto(Environment environment) {
        EnvironmentDto.EnvironmentDtoBuilder builder = EnvironmentDto.builder()
                .withId(environment.getId())
                .withResourceCrn(environment.getResourceCrn())
                .withName(environment.getName())
                .withOriginalName(environment.getOriginalName())
                .withDescription(environment.getDescription())
                .withAccountId(environment.getAccountId())
                .withArchived(environment.isArchived())
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(environment.getCredential())
                .withDeletionTimestamp(environment.getDeletionTimestamp())
                .withLocationDto(environmentToLocationDto(environment))
                .withRegions(environment.getRegionSet())
                .withTelemetry(environment.getTelemetry())
                .withBackup(environment.getBackup())
                .withEnvironmentStatus(environment.getStatus())
                .withCreator(environment.getCreator())
                .withAuthentication(authenticationDtoConverter.authenticationToDto(environment.getAuthentication()))
                .withFreeIpaCreation(environmentToFreeIpaCreationDto(environment))
                .withCreated(environment.getCreated())
                .withStatusReason(environment.getStatusReason())
                .withExperimentalFeatures(environment.getExperimentalFeaturesJson())
                .withTags(environment.getEnvironmentTags())
                .withSecurityAccess(environmentToSecurityAccessDto(environment.getCidr(), environment.getSecurityGroupIdForKnox(),
                        environment.getDefaultSecurityGroupId()))
                .withAdminGroupName(environment.getAdminGroupName())
                .withProxyConfig(environment.getProxyConfig())
                .withEnvironmentDeletionType(environment.getDeletionType())
                .withEnvironmentServiceVersion(environment.getEnvironmentServiceVersion())
                .withEnvironmentDomain(environment.getDomain())
                .withDataServices(environment.getDataServices())
                .withEnableSecretEncryption(environment.isEnableSecretEncryption())
                .withCreatorClient(environment.getCreatorClient())
                .withEnableComputeCluster(isComputeClusterEnabled(environment.getDefaultComputeCluster()))
                .withEnvironmentType(environment.getEnvironmentType())
                .withRemoteEnvironmentCrn(environment.getRemoteEnvironmentCrn())
                .withEncryptionProfileCrn(environment.getEncryptionProfileCrn());

        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        builder.withCredentialDetails(credentialDetailsConverter.credentialToCredentialDetails(cloudPlatform, environment.getCredential()));
        doIfNotNull(environment.getParameters(), parameters -> builder.withParameters(
                environmentParamsConverterMap.get(cloudPlatform).convertToDto(parameters)));
        doIfNotNull(environment.getNetwork(), network -> builder.withNetwork(
                environmentNetworkConverterMap.get(cloudPlatform).convertToDto(network)));
        doIfNotNull(environment.getParentEnvironment(), parentEnvironment -> builder
                .withParentEnvironmentCrn(parentEnvironment.getResourceCrn())
                .withParentEnvironmentName(parentEnvironment.getName())
                .withParentEnvironmentCloudPlatform(parentEnvironment.getCloudPlatform()));
        doIfNotNull(environment.getDefaultComputeCluster(),
                defaultComputeCluster -> builder.withExternalizedComputeCluster(defaultComputeClusterToExternalizedComputeClusterDto(defaultComputeCluster)));
        return builder.build();
    }

    public Environment creationDtoToEnvironment(EnvironmentCreationDto creationDto) {
        Environment environment = new Environment();
        environment.setAccountId(creationDto.getAccountId());
        environment.setCreator(creationDto.getCreator());
        environment.setName(creationDto.getName());
        environment.setOriginalName(creationDto.getName());
        environment.setArchived(false);
        environment.setCloudPlatform(creationDto.getCloudPlatform());
        environment.setDescription(creationDto.getDescription());
        LocationDto location = creationDto.getLocation();
        environment.setLatitude(location.getLatitude());
        environment.setLongitude(location.getLongitude());
        environment.setLocation(location.getName());
        environment.setTelemetry(creationDto.getTelemetry());
        environment.setBackup(creationDto.getBackup());
        environment.setLocationDisplayName(location.getDisplayName());
        environment.setStatus(EnvironmentStatus.CREATION_INITIATED);
        environment.setStatusReason(null);
        environment.setCreateFreeIpa(creationDto.getFreeIpaCreation().isCreate());
        environment.setFreeIpaInstanceCountByGroup(creationDto.getFreeIpaCreation().getInstanceCountByGroup());
        environment.setFreeIpaLoadBalancer(creationDto.getFreeIpaCreation().getLoadBalancerType());
        environment.setFreeIpaInstanceType(creationDto.getFreeIpaCreation().getInstanceType());
        environment.setFreeIpaImageCatalog(creationDto.getFreeIpaCreation().getImageCatalog());
        environment.setFreeIpaPlatformVariant(creationDto.getFreeIpaCreation().getPlatformVariant());
        environment.setFreeIpaEnableMultiAz(creationDto.getFreeIpaCreation().isEnableMultiAz());
        environment.setDeletionType(NONE);
        environment.setFreeIpaImageId(creationDto.getFreeIpaCreation().getImageId());
        environment.setFreeIpaImageOs(creationDto.getFreeIpaCreation().getImageOs());
        if (creationDto.getFreeIpaCreation().getArchitecture() != null) {
            environment.setFreeIpaArchitecture(creationDto.getFreeIpaCreation().getArchitecture().getName());
        }
        environment.setAdminGroupName(creationDto.getAdminGroupName());
        environment.setCreated(System.currentTimeMillis());
        environment.setTags(environmentTagsDtoConverter.getTags(creationDto));
        environment.setExperimentalFeaturesJson(creationDto.getExperimentalFeatures());
        environment.setDataServices(creationDto.getDataServices());
        environment.setCreatorClient(creationDto.getCreatorClient());
        if (null != creationDto.getFreeIpaCreation().getSeLinux()) {
            environment.setSeLinux(creationDto.getFreeIpaCreation().getSeLinux());
        } else {
            environment.setSeLinux(SeLinux.PERMISSIVE);
        }
        setDefaultComputeCluster(creationDto, environment);
        setRegions(creationDto, environment);
        environment.setEnvironmentType(creationDto.getEnvironmentType());
        return environment;
    }

    public ExternalizedComputeClusterDto defaultComputeClusterToExternalizedComputeClusterDto(DefaultComputeCluster defaultComputeCluster) {
        if (defaultComputeCluster != null && defaultComputeCluster.isCreate()) {
            return ExternalizedComputeClusterDto.builder()
                    .withCreate(defaultComputeCluster.isCreate())
                    .withPrivateCluster(defaultComputeCluster.isPrivateCluster())
                    .withKubeApiAuthorizedIpRanges(defaultComputeCluster.getKubeApiAuthorizedIpRanges())
                    .withOutboundType(defaultComputeCluster.getOutboundType())
                    .withWorkerNodeSubnetIds(defaultComputeCluster.getWorkerNodeSubnetIds())
                    .build();
        } else {
            return null;
        }
    }

    private void setDefaultComputeCluster(EnvironmentCreationDto creationDto, Environment environment) {
        ExternalizedComputeClusterDto computeClusterCreation = creationDto.getExternalizedComputeCluster();
        DefaultComputeCluster defaultComputeCluster = new DefaultComputeCluster();
        if (computeClusterCreation.isCreate()) {
            defaultComputeCluster.setCreate(computeClusterCreation.isCreate());
            defaultComputeCluster.setPrivateCluster(computeClusterCreation.isPrivateCluster());
            defaultComputeCluster.setKubeApiAuthorizedIpRanges(computeClusterCreation.getKubeApiAuthorizedIpRanges());
            defaultComputeCluster.setOutboundType(computeClusterCreation.getOutboundType());
            defaultComputeCluster.setWorkerNodeSubnetIds(computeClusterCreation.getWorkerNodeSubnetIds());
        }
        environment.setDefaultComputeCluster(defaultComputeCluster);
    }

    public NetworkDto networkToNetworkDto(Environment environment) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        NetworkDto result = null;
        if (environment.getNetwork() != null) {
            result = environmentNetworkConverterMap.get(cloudPlatform).convertToDto(environment.getNetwork());
        }
        return result;
    }

    private void setRegions(EnvironmentCreationDto creationDto, Environment environment) {
        Set<Region> regions = creationDto.getRegions().stream().map(r -> {
            Region region = new Region();
            region.setName(r);
            return region;
        }).collect(Collectors.toSet());
        environment.setRegions(regions);
    }

    public LocationDto environmentToLocationDto(Environment environment) {
        return LocationDto.builder()
                .withName(environment.getLocation())
                .withDisplayName(environment.getLocationDisplayName())
                .withLongitude(environment.getLongitude())
                .withLatitude(environment.getLatitude())
                .build();
    }

    private LocationDto environmentViewToLocationDto(EnvironmentView environment) {
        return LocationDto.builder()
                .withName(environment.getLocation())
                .withDisplayName(environment.getLocationDisplayName())
                .withLongitude(environment.getLongitude())
                .withLatitude(environment.getLatitude())
                .build();
    }

    private SecurityAccessDto environmentToSecurityAccessDto(String cidr, String securityGroupIdForKnox, String defaultSecurityGroupId) {
        return SecurityAccessDto.builder()
                .withCidr(cidr)
                .withSecurityGroupIdForKnox(securityGroupIdForKnox)
                .withDefaultSecurityGroupId(defaultSecurityGroupId)
                .build();
    }

    private FreeIpaCreationDto environmentToFreeIpaCreationDto(Environment environment) {
        Integer ipaInstanceCountByGroup = Optional.ofNullable(environment.getFreeIpaInstanceCountByGroup())
                .orElse(ipaInstanceCountByGroupProvider.getDefaultInstanceCount());
        return FreeIpaCreationDto.builder(ipaInstanceCountByGroup)
                .withCreate(environment.isCreateFreeIpa())
                .withLoadBalancerType(environment.getFreeIpaLoadBalancer())
                .withInstanceType(environment.getFreeIpaInstanceType())
                .withImageCatalog(environment.getFreeIpaImageCatalog())
                .withImageId(environment.getFreeIpaImageId())
                .withImageOs(environment.getFreeIpaImageOs())
                .withArchitecture(Optional.ofNullable(environment.getFreeIpaArchitecture()).map(Architecture::fromStringWithFallback).orElse(null))
                .withEnableMultiAz(environment.isFreeIpaEnableMultiAz())
                .withRecipes(environmentRecipeService.getRecipes(environment.getId()))
                .withSeLinux(environment.getSeLinux())
                .withAws(getFreeIpaAwsParameters(environment.getCloudPlatform(), environment.getParameters()))
                .build();
    }

    private FreeIpaCreationDto environmentViewToFreeIpaCreationDto(EnvironmentView environment) {
        FreeIpaCreationDto freeIpaCreationDto = getFreeIpaCreationDto(environment.isCreateFreeIpa(), environment.getFreeIpaInstanceCountByGroup(),
                environment.getFreeIpaInstanceType(), environment.getFreeIpaImageCatalog(), environment.getFreeIpaImageId(),
                environment.getFreeIpaImageOs(), environment.isFreeIpaEnableMultiAz());
        freeIpaCreationDto.setRecipes(environment.getFreeipaRecipes());
        freeIpaCreationDto.setAws(getFreeIpaAwsParameters(environment.getCloudPlatform(), environment.getParameters()));
        return freeIpaCreationDto;
    }

    private FreeIpaCreationAwsParametersDto getFreeIpaAwsParameters(String cloudPlatform, BaseParameters parameters) {
        if (cloudPlatform.equals(CloudPlatform.AWS.name()) && Objects.nonNull(parameters)) {
            AwsParameters awsParameters = (AwsParameters) parameters;
            return FreeIpaCreationAwsParametersDto.builder()
                    .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                            .withPercentage(awsParameters.getFreeIpaSpotPercentage())
                            .withMaxPrice(awsParameters.getFreeIpaSpotMaxPrice())
                            .build())
                    .build();
        } else {
            return null;
        }
    }

    private FreeIpaCreationDto getFreeIpaCreationDto(boolean createFreeIpa, Integer freeIpaInstanceCountByGroup, String freeIpaInstanceType,
            String freeIpaImageCatalog, String freeIpaImageId, String freeIpaImageOs, boolean freeIpaEnableMultiAz) {
        Integer ipaInstanceCountByGroup = Optional.ofNullable(freeIpaInstanceCountByGroup).orElse(ipaInstanceCountByGroupProvider.getDefaultInstanceCount());
        Builder builder = FreeIpaCreationDto.builder(ipaInstanceCountByGroup)
                .withCreate(createFreeIpa);
        builder.withInstanceType(freeIpaInstanceType);
        builder.withImageCatalog(freeIpaImageCatalog);
        builder.withImageId(freeIpaImageId);
        builder.withImageOs(freeIpaImageOs);
        builder.withEnableMultiAz(freeIpaEnableMultiAz);
        return builder.build();
    }

    private boolean isComputeClusterEnabled(DefaultComputeCluster defaultComputeCluster) {
        return defaultComputeCluster != null && defaultComputeCluster.isCreate();
    }
}