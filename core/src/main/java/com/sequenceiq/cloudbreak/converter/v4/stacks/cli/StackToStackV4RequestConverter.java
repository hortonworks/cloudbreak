package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToExternalDatabaseRequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.TelemetryConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.network.InstanceGroupNetworkToInstanceGroupNetworkV4RequestConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;

@Component
public class StackToStackV4RequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackV4RequestConverter.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private TelemetryConverter telemetryConverter;

    @Inject
    private StackToExternalDatabaseRequestConverter stackToExternalDatabaseRequestConverter;

    @Inject
    private DatalakeService datalakeService;

    @Inject
    private InstanceGroupToInstanceGroupV4RequestConverter instanceGroupToInstanceGroupV4RequestConverter;

    @Inject
    private InstanceGroupNetworkToInstanceGroupNetworkV4RequestConverter instanceGroupNetworkToInstanceGroupNetworkV4RequestConverter;

    @Inject
    private NetworkToNetworkV4RequestConverter networkToNetworkV4RequestConverter;

    @Inject
    private ClusterToClusterV4RequestConverter clusterToClusterV4RequestConverter;

    @Inject
    private StackAuthenticationToStackAuthenticationV4RequestConverter stackAuthenticationToStackAuthenticationV4RequestConverter;

    public StackV4Request convert(Stack source) {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCloudPlatform(getIfNotNull(source.getCloudPlatform(), cp -> Enum.valueOf(CloudPlatform.class, cp)));
        stackV4Request.setEnvironmentCrn(source.getEnvironmentCrn());
        stackV4Request.setCustomDomain(getCustomDomainSettings(source));
        providerParameterCalculator.parse(new HashMap<>(source.getParameters()), stackV4Request);
        stackV4Request.setAuthentication(stackAuthenticationToStackAuthenticationV4RequestConverter.convert(source.getStackAuthentication()));
        stackV4Request.setNetwork(networkToNetworkV4RequestConverter.convert(source.getNetwork()));
        stackV4Request.setCluster(clusterToClusterV4RequestConverter.convert(source.getCluster()));
        stackV4Request.setExternalDatabase(getIfNotNull(source, stackToExternalDatabaseRequestConverter::convert));
        if (!source.getLoadBalancers().isEmpty()) {
            stackV4Request.setEnableLoadBalancer(true);
        }
        stackV4Request.setInstanceGroups(getInstanceGroups(source));
        prepareImage(source, stackV4Request);
        prepareTags(source, stackV4Request);
        prepareTelemetryRequest(source, stackV4Request);
        datalakeService.prepareDatalakeRequest(source, stackV4Request);
        stackV4Request.setPlacement(getPlacementSettings(source.getRegion(), source.getAvailabilityZone()));
        prepareInputs(source, stackV4Request);
        stackV4Request.setTimeToLive(getStackTimeToLive(source));
        return stackV4Request;
    }

    private void prepareTelemetryRequest(Stack source, StackV4Request stackV4Request) {
        Telemetry telemetry = componentConfigProviderService.getTelemetry(source.getId());
        if (telemetry != null) {
            TelemetryRequest telemetryRequest = telemetryConverter.convertToRequest(telemetry);
            stackV4Request.setTelemetry(telemetryRequest);
        }
    }

    private PlacementSettingsV4Request getPlacementSettings(String region, String availabilityZone) {
        PlacementSettingsV4Request ps = new PlacementSettingsV4Request();
        ps.setRegion(region);
        ps.setAvailabilityZone(availabilityZone);
        return ps;
    }

    private CustomDomainSettingsV4Request getCustomDomainSettings(Stack stack) {
        CustomDomainSettingsV4Request cd = new CustomDomainSettingsV4Request();
        cd.setDomainName(stack.getCustomDomain());
        cd.setHostname(stack.getCustomHostname());
        cd.setHostgroupNameAsHostname(stack.isHostgroupNameAsHostname());
        cd.setClusterNameAsSubdomain(stack.isClusterNameAsSubdomain());
        return cd;
    }

    private void prepareImage(Stack source, StackV4Request stackV2Request) {
        try {
            Image image = componentConfigProviderService.getImage(source.getId());
            ImageSettingsV4Request is = new ImageSettingsV4Request();
            is.setId(Strings.isNullOrEmpty(image.getImageId()) ? "" : image.getImageId());
            is.setCatalog(Strings.isNullOrEmpty(image.getImageCatalogName()) ? "" : image.getImageCatalogName());
            stackV2Request.setImage(is);
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.info(e.toString());
        }
    }

    private List<InstanceGroupV4Request> getInstanceGroups(Stack stack) {
        List<InstanceGroupV4Request> ret = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            InstanceGroupV4Request instanceGroupV4Request = instanceGroupToInstanceGroupV4RequestConverter
                    .convert(instanceGroup);
            collectInformationsFromActualHostgroup(stack.getCluster(), instanceGroup, instanceGroupV4Request);
            ret.add(instanceGroupV4Request);
        }
        return ret;
    }

    private void collectInformationsFromActualHostgroup(Cluster cluster, InstanceGroup instanceGroup, InstanceGroupV4Request instanceGroupV4Request) {
        if (cluster != null && cluster.getHostGroups() != null) {
            cluster.getHostGroups().stream()
                    .filter(hostGroup -> hostGroup.getName().equals(instanceGroup.getGroupName()))
                    .findFirst()
                    .ifPresent(hostGroup -> {
                        Set<String> recipeNames = hostGroup.getRecipes().stream().map(Recipe::getName).collect(Collectors.toSet());
                        instanceGroupV4Request.setRecipeNames(recipeNames);
                        instanceGroupV4Request.setRecoveryMode(hostGroup.getRecoveryMode());
                        if (instanceGroup.getInstanceGroupNetwork() != null) {
                            instanceGroupV4Request.setNetwork(instanceGroupNetworkToInstanceGroupNetworkV4RequestConverter
                                    .convert(instanceGroup.getInstanceGroupNetwork()));
                        }
                    });
        }
    }

    private void prepareTags(Stack source, StackV4Request stackV2Request) {
        try {
            if (source.getTags() != null && source.getTags().getValue() != null) {
                StackTags stackTags = source.getTags().get(StackTags.class);
                if (stackTags.getUserDefinedTags() != null) {
                    TagsV4Request tags = new TagsV4Request();
                    tags.setApplication(null);
                    tags.setDefaults(null);
                    tags.setUserDefined(stackTags.getUserDefinedTags());
                    stackV2Request.setTags(tags);
                }
            }
        } catch (IOException e) {
            stackV2Request.setTags(null);
        }
    }

    private Long getStackTimeToLive(Stack stack) {
        Map<String, String> params = stack.getParameters();
        Optional<String> optional = Optional.ofNullable(params.get(PlatformParametersConsts.TTL_MILLIS));
        if (optional.isPresent()) {
            return optional.map(Long::parseLong).get();
        }
        return null;
    }

    private void prepareInputs(Stack source, StackV4Request stackV2Request) {
        try {

            StackInputs stackInputs = Strings.isNullOrEmpty(source.getInputs().getValue())
                    ? new StackInputs(new HashMap<>(), new HashMap<>(), new HashMap<>()) : source.getInputs().get(StackInputs.class);
            if (stackInputs.getCustomInputs() != null) {
                stackV2Request.setInputs(stackInputs.getCustomInputs());
            }
        } catch (IOException e) {
            stackV2Request.setInputs(null);
        }
    }
}
