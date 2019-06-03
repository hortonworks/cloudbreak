package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNullF;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.environment.Region;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.sharedservice.SdxV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.tags.TagsV1Request;

@Component
public class DistroXV1RequestToStackV4RequestConverter {

    @Inject
    private DistroXEnvironmentV1ToEnvironmentSettingsConverter environmentConverter;

    @Inject
    private DistroXAuthenticationToStaAuthenticationConverter authenticationConverter;

    @Inject
    private DistroXImageToImageSettingsConverter imageConverter;

    @Inject
    private DistroXClusterToClusterConverter clusterConverter;

    @Inject
    private DistroXTelemetryToTelemetryV4Converter telemetryConverter;

    @Inject
    private InstanceGroupV1ToInstanceGroupV4Converter instanceGroupConverter;

    @Inject
    private NetworkV1ToNetworkV4Converter networkConverter;

    @Inject
    private DistroXParameterConverter stackParameterConverter;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private WorkspaceService workspaceService;

    public StackV4Request convert(DistroXV1Request source) {
        StackV4Request request = new StackV4Request();
        request.setName(source.getName());
        request.setType(StackType.WORKLOAD);
        request.setCloudPlatform(getCloudPlatform(source.getEnvironment().getName()));

        request.setEnvironment(environmentConverter.convert(source.getEnvironment()));
        ifNotNull(authenticationConverter.convert(source.getAuthentication()), request::setAuthentication);
        request.setImage(ifNotNullF(source.getImage(), imageConverter::convert));
        request.setTelemetry(ifNotNullF(source.getTelemetry(), telemetryConverter::convert));
        request.setCluster(ifNotNullF(source.getCluster(), clusterConverter::convert));
        request.setInstanceGroups(ifNotNullF(source.getInstanceGroups(), instanceGroupConverter::convert));
        request.setNetwork(ifNotNullF(source.getNetwork(), networkConverter::convert));
        request.setAws(ifNotNullF(source.getAws(), stackParameterConverter::convert));
        request.setAzure(ifNotNullF(source.getAzure(), stackParameterConverter::convert));

        request.setPlacement(getPlacement(source.getEnvironment().getName()));
        request.setInputs(source.getInputs());
        request.setTags(ifNotNullF(source.getTags(), this::getTags));
        request.setSharedService(ifNotNullF(source.getSdx(), this::getSharedService));
        request.setCustomDomain(null);
        request.setGatewayPort(null);
        request.setGcp(null);
        request.setMock(null);
        request.setOpenstack(null);
        request.setYarn(null);
        return request;
    }

    private CloudPlatform getCloudPlatform(String environmentName) {
        return CloudPlatform.valueOf(environmentService.get(environmentName, workspaceService.getForCurrentUser().getId()).getCloudPlatform());
    }

    private TagsV4Request getTags(TagsV1Request source) {
        TagsV4Request response = new TagsV4Request();
        response.setApplication(source.getApplication());
        response.setUserDefined(source.getUserDefined());
        response.setDefaults(source.getDefaults());
        return response;
    }

    private SharedServiceV4Request getSharedService(SdxV1Request sdx) {
        SharedServiceV4Request sharedServiceV4Request = new SharedServiceV4Request();
        sharedServiceV4Request.setDatalakeName(sdx.getName());
        return sharedServiceV4Request;
    }

    private PlacementSettingsV4Request getPlacement(String name) {
        Environment environment = environmentService.getByNameForWorkspace(name, workspaceService.getForCurrentUser());
        PlacementSettingsV4Request placementSettings = new PlacementSettingsV4Request();
        String region = environment.getRegionSet().stream().findFirst().map(Region::getName).orElse(null);
        placementSettings.setRegion(region);
        placementSettings.setAvailabilityZone(region + 'a');
        return placementSettings;
    }
}
