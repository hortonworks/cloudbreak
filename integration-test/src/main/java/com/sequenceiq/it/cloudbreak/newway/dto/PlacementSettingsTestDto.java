package com.sequenceiq.it.cloudbreak.newway.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.placement.PlacementSettingsV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class PlacementSettingsTestDto extends AbstractCloudbreakTestDto<PlacementSettingsV4Request, PlacementSettingsV4Response, PlacementSettingsTestDto> {

    private static final String PLACEMENT = "PLACEMENT";

    public PlacementSettingsTestDto() {
        super(PLACEMENT);
    }

    protected PlacementSettingsTestDto(TestContext testContext) {
        super(new PlacementSettingsV4Request(), testContext);
    }

    @Override
    public PlacementSettingsTestDto valid() {
        return getCloudProvider().placement(this);
    }

    public PlacementSettingsTestDto withRegion(String region) {
        getRequest().setRegion(region);
        return this;
    }

    public PlacementSettingsTestDto withAvailabilityZone(String availabilityZone) {
        getRequest().setAvailabilityZone(availabilityZone);
        return this;
    }
}
