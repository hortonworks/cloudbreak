package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.YarnStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.converter.v4.stacks.TelemetryConverter;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Base;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.AwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.tags.TagsV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Component
public class DistroXV1RequestToStackV4RequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXV1RequestToStackV4RequestConverter.class);

    private static final String MIN_RUNTIME_VERSION_FOR_DEFAULT_AWS_NATIVE = "7.3.1";

    @Inject
    private DistroXAuthenticationToStaAuthenticationConverter authenticationConverter;

    @Inject
    private DistroXImageToImageSettingsConverter imageConverter;

    @Inject
    private DistroXClusterToClusterConverter clusterConverter;

    @Inject
    private InstanceGroupV1ToInstanceGroupV4Converter instanceGroupConverter;

    @Inject
    private NetworkV1ToNetworkV4Converter networkConverter;

    @Inject
    private DistroXParameterConverter stackParameterConverter;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private SdxConverter sdxConverter;

    @Inject
    private TelemetryConverter telemetryConverter;

    @Inject
    private SdxClientService sdxClientService;

    @Inject
    private DistroXDatabaseRequestToStackDatabaseRequestConverter databaseRequestConverter;

    @Inject
    private SecurityV1RequestToSecurityV4RequestConverter securityRequestConverter;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ProviderPreferencesService providerPreferencesService;

    public StackV4Request convert(DistroXV1Request source) {
        DetailedEnvironmentResponse environment = Optional.ofNullable(environmentClientService.getByName(source.getEnvironmentName()))
                .orElseThrow(() -> new BadRequestException("No environment name provided hence unable to obtain some important data"));
        StackV4Request request = new StackV4Request();
        SdxClusterResponse sdxClusterResponse = getSdxClusterResponse(environment);
        String runtime = sdxClusterResponse != null ? sdxClusterResponse.getRuntime() : null;
        request.setName(source.getName());
        request.setType(StackType.WORKLOAD);
        request.setCloudPlatform(getCloudPlatform(environment));
        request.setEnvironmentCrn(environment.getCrn());
        request.setAuthentication(getIfNotNull(environment.getAuthentication(), authenticationConverter::convert));
        request.setImage(getIfNotNull(source.getImage(), imageConverter::convert));
        request.setCluster(getIfNotNull(source, environment, clusterConverter::convert));
        NetworkV4Request network = getNetwork(source.getNetwork(), environment, source.getInstanceGroups());
        request.setNetwork(network);
        request.setInstanceGroups(getIfNotNull(source.getInstanceGroups(), igs ->
                instanceGroupConverter.convertTo(network, igs, environment)));
        request.setAws(getIfNotNull(source.getAws(), stackParameterConverter::convert));
        request.setAzure(getIfNotNull(source.getAzure(), stackParameterConverter::convert));
        request.setGcp(getIfNotNull(source.getGcp(), stackParameterConverter::convert));
        request.setYarn(getYarnProperties(source, environment));
        request.setInputs(source.getInputs());
        request.setTags(getIfNotNull(source.getTags(), this::getTags));
        request.setPlacement(preparePlacement(environment));
        request.setSharedService(sdxConverter.getSharedService(sdxClusterResponse));
        request.setCustomDomain(null);
        request.setTimeToLive(source.getTimeToLive());
        request.setTelemetry(getTelemetryRequest(environment, sdxClusterResponse));
        request.setGatewayPort(source.getGatewayPort());
        request.setExternalDatabase(getIfNotNull(source.getExternalDatabase(), databaseRequestConverter::convert));
        request.setEnableLoadBalancer(source.isEnableLoadBalancer());
        request.setJavaVersion(source.getJavaVersion());
        request.setEnableMultiAz(source.isEnableMultiAz());
        request.setArchitecture(source.getArchitecture());
        request.setDisableDbSslEnforcement(source.isDisableDbSslEnforcement());
        request.setSecurity(securityRequestConverter.convert(source.getSecurity()));
        calculateVariant(environment, source, request, runtime);
        checkMultipleGatewayNodes(source);
        return request;
    }

    private boolean enforceAwsNativeForSingleAz(String cloudProvider, boolean govCloud) {
        return CloudPlatform.AWS.name().equals(cloudProvider)
                && entitlementService.enforceAwsNativeForSingleAzDatahubEnabled(ThreadBasedUserCrnProvider.getAccountId())
                && !govCloud;
    }

    private boolean enforceAwsNativeBasedOnRuntime(String cloudProvider, String runtime, boolean govCloud) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        return CloudPlatform.AWS.name().equals(cloudProvider)
                && runtime != null
                && versionComparator.compare(() -> runtime, () -> MIN_RUNTIME_VERSION_FOR_DEFAULT_AWS_NATIVE) >= 0
                && !govCloud;
    }

    private boolean enforceAwsBasedOnValueProvided(String cloudProvider, String variant, boolean govCloud, boolean multiAzEnabled) {
        return CloudPlatform.AWS.name().equals(cloudProvider) && variantAvailable(variant, govCloud, multiAzEnabled);
    }

    private void calculateVariant(DetailedEnvironmentResponse environment, DistroXV1Request source, StackV4Request request, String runtime) {
        Boolean govCloud = environment.getCredential().getGovCloud();
        String cloudPlatform = environment.getCloudPlatform();
        String variant = source.getVariant();
        if (enforceAwsNativeForSingleAz(cloudPlatform, govCloud)) {
            request.setVariant(AwsVariant.AWS_NATIVE_VARIANT.variant().value());
        } else if (enforceAwsNativeBasedOnRuntime(cloudPlatform, runtime, govCloud)) {
            request.setVariant(AwsVariant.AWS_NATIVE_VARIANT.variant().value());
        } else {
            request.setVariant(variant);
        }

        if (enforceAwsBasedOnValueProvided(cloudPlatform, variant, govCloud, source.isEnableMultiAz())
                && variantSupported(cloudPlatform, variant)) {
            request.setVariant(variant);
        }
    }

    private boolean variantAvailable(String variant, boolean govCloud,  boolean multiAzEnabled) {
        return StringUtils.isNotBlank(variant)
                && variant != null
                && !govCloud
                && !multiAzEnabled;
    }

    private boolean variantSupported(String cloudPlatform, String variant) {
        providerPreferencesService.cloudConstantByName(cloudPlatform).ifPresent(cloudConstant -> {
            if (!cloudConstant.hasVariants(variant)) {
                throw new BadRequestException(String.format("Variant %s is not supported for cloud platform %s. Supported Variants are: %s",
                        variant,
                        cloudPlatform,
                        String.join(", ", cloudConstant.variants())
                ));
            }
        });
        return true;
    }

    private void checkMultipleGatewayNodes(DistroXV1Request distroXV1Request) {
        if (distroXV1Request.getInstanceGroups() == null || distroXV1Request.getInstanceGroups().isEmpty()) {
            return;
        }
        distroXV1Request.getInstanceGroups().forEach(ig -> {
            if (InstanceGroupType.GATEWAY == ig.getType()) {
                if (distroXV1Request.isEnableLoadBalancer()) {
                    if (ig.getNodeCount() < 1) {
                        throw new BadRequestException("Instance group with GATEWAY type must contain at least 1 node!");
                    }
                } else {
                    if (ig.getNodeCount() != 1) {
                        throw new BadRequestException("Instance group with GATEWAY type must contain 1 node!");
                    }
                }
            }
        });

    }

    private TelemetryRequest getTelemetryRequest(DetailedEnvironmentResponse environment, SdxClusterResponse sdxClusterResponse) {
        TelemetryResponse envTelemetryResp = environment != null ? environment.getTelemetry() : null;
        return telemetryConverter.convert(environment, envTelemetryResp, sdxClusterResponse);
    }

    NetworkV4Request getNetwork(NetworkV1Request networkRequest, DetailedEnvironmentResponse environment,
                                Set<InstanceGroupV1Request> instanceGroupRequests) {
        Set<String> subnetIds = new HashSet<>();
        if (instanceGroupRequests != null) {
            subnetIds.addAll(instanceGroupRequests.stream()
                    .map(ig -> Optional.ofNullable(ig.getNetwork())
                            .map(InstanceGroupNetworkV1Base::getAws)
                            .map(InstanceGroupAwsNetworkV1Parameters::getSubnetIds)
                            .orElseGet(ArrayList::new)
                    )
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet()));
        }
        if (subnetIds.size() == 1) {
            String subnetId = subnetIds.stream().findFirst().get();
            LOGGER.info("Update the global subnet id to {}, because it is configured in instance group level as the new way", subnetId);
            if (networkRequest == null) {
                networkRequest = new NetworkV1Request();
            }
            if (networkRequest.getAws() == null) {
                networkRequest.setAws(new AwsNetworkV1Parameters());
            }
            networkRequest.getAws().setSubnetId(subnetId);
        } else {
            LOGGER.info("Use the legacy way to configure the global network.");
        }
        NetworkV4Request network = getIfNotNull(new ImmutablePair<>(networkRequest, environment), networkConverter::convertToNetworkV4Request);

        validateSubnetIds(network, environment);
        return network;
    }

    private void validateSubnetIds(NetworkV4Request network, DetailedEnvironmentResponse environment) {
        switch (environment.getCloudPlatform()) {
            case "AWS":
                validateSubnet(network, environment, network.getAws().getSubnetId());
                break;
            case "AZURE":
                validateSubnet(network, environment, network.getAzure().getSubnetId());
                break;
            case "GCP":
                validateSubnet(network, environment, network.getGcp().getSubnetId());
                break;
            default:
        }

    }

    private void validateSubnet(NetworkV4Request network, DetailedEnvironmentResponse environment, String subnetId) {
        if (subnetId != null && (environment == null || environment.getNetwork() == null || environment.getNetwork().getSubnetIds() == null
                || !environment.getNetwork().getSubnetIds().contains(subnetId))) {
            LOGGER.info("The given subnet id [{}] is not attached to the Environment [{}]. Network request: [{}]", subnetId, environment, network);
            throw new BadRequestException(String.format("The given subnet id (%s) is not attached to the Environment (%s)",
                    subnetId, environment == null ? null : environment.getName()));
        }
    }

    private YarnStackV4Parameters getYarnProperties(DistroXV1Request source, DetailedEnvironmentResponse environment) {
        YarnStackV4Parameters yarnParameters = getIfNotNull(source.getYarn(), stackParameterConverter::convert);
        if (yarnParameters == null && environment != null) {
            yarnParameters = getIfNotNull(Optional.ofNullable(environment.getNetwork())
                    .map(EnvironmentNetworkResponse::getYarn)
                    .orElse(null), stackParameterConverter::convert);
        }
        return yarnParameters;
    }

    public DistroXV1Request convert(StackV4Request source) {
        DistroXV1Request request = new DistroXV1Request();
        request.setName(source.getName());
        DetailedEnvironmentResponse env = null;
        if (source.getEnvironmentCrn() != null) {
            env = environmentClientService.getByCrn(source.getEnvironmentCrn());
            request.setEnvironmentName(env != null ? env.getName() : null);
        }
        request.setImage(getIfNotNull(source.getImage(), imageConverter::convert));
        request.setCluster(getIfNotNull(source.getCluster(), clusterConverter::convert));
        request.setNetwork(getIfNotNull(source.getNetwork(), networkConverter::convertToNetworkV1Request));
        setInstanceGroups(source, request, env);
        request.setAws(getIfNotNull(source.getAws(), stackParameterConverter::convert));
        request.setAzure(getIfNotNull(source.getAzure(), stackParameterConverter::convert));
        request.setGcp(getIfNotNull(source.getGcp(), stackParameterConverter::convert));
        request.setYarn(getIfNotNull(source.getYarn(), stackParameterConverter::convert));
        request.setInputs(source.getInputs());
        request.setTags(getIfNotNull(source.getTags(), this::getTags));
        request.setSdx(getIfNotNull(source.getSharedService(), sdxConverter::getSdx));
        request.setGatewayPort(source.getGatewayPort());
        request.setExternalDatabase(getIfNotNull(source.getExternalDatabase(), databaseRequestConverter::convert));
        request.setEnableLoadBalancer(source.isEnableLoadBalancer());
        request.setJavaVersion(source.getJavaVersion());
        request.setDisableDbSslEnforcement(source.isDisableDbSslEnforcement());

        return request;
    }

    private void setInstanceGroups(StackV4Request source, DistroXV1Request request, DetailedEnvironmentResponse env) {
        if (source.getInstanceGroups() != null) {
            request.setInstanceGroups(instanceGroupConverter.convertFrom(source.getNetwork(), source.getInstanceGroups(), env));
        }
    }

    private CloudPlatform getCloudPlatform(DetailedEnvironmentResponse environment) {
        return CloudPlatform.valueOf(environment.getCloudPlatform());
    }

    private TagsV4Request getTags(TagsV1Request source) {
        TagsV4Request response = new TagsV4Request();
        response.setApplication(source.getApplication());
        response.setUserDefined(source.getUserDefined());
        response.setDefaults(source.getDefaults());
        return response;
    }

    private TagsV1Request getTags(TagsV4Request source) {
        TagsV1Request response = new TagsV1Request();
        response.setApplication(source.getApplication());
        response.setUserDefined(source.getUserDefined());
        response.setDefaults(source.getDefaults());
        return response;
    }

    private SdxClusterResponse getSdxClusterResponse(DetailedEnvironmentResponse environment) {
        if (environment != null) {
            return sdxClientService
                    .getByEnvironmentCrn(environment.getCrn())
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private PlacementSettingsV4Request preparePlacement(DetailedEnvironmentResponse environment) {
        if (!CloudPlatform.YARN.name().equals(environment.getCloudPlatform())) {
            PlacementSettingsV4Request ret = new PlacementSettingsV4Request();
            ret.setRegion(getRegionFromEnv(environment));
            ret.setAvailabilityZone(getAvailabilityZoneFromEnv(environment));
            return ret;
        }
        return null;
    }

    private String getRegionFromEnv(DetailedEnvironmentResponse environment) {
        return environment.getRegions().getNames().stream()
                .findFirst()
                .orElse(null);
    }

    private String getAvailabilityZoneFromEnv(DetailedEnvironmentResponse environment) {
        if (environment.getNetwork() != null && environment.getNetwork().getSubnetMetas() != null) {
            return environment.getNetwork().getSubnetMetas().entrySet().stream()
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .map(CloudSubnet::getAvailabilityZone)
                    .orElse(null);
        } else {
            return null;
        }
    }
}
