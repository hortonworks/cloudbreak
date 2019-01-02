package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.placement.PlacementSettingsV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class PlacementSettingsEntity extends AbstractCloudbreakEntity<PlacementSettingsV4Request, PlacementSettingsV4Response, PlacementSettingsEntity> {

    private static final String PLACEMENT = "PLACEMENT";

    public PlacementSettingsEntity() {
        super(PLACEMENT);
    }

    protected PlacementSettingsEntity(TestContext testContext) {
        super(new PlacementSettingsV4Request(), testContext);
    }

    @Override
    public PlacementSettingsEntity valid() {
        return withRegion(getCloudProvider().region())
                .withAvailabilityZone(getCloudProvider().availabilityZone());
    }

    public PlacementSettingsEntity withRegion(String region) {
        getRequest().setRegion(region);
        return this;
    }

    public PlacementSettingsEntity withAvailabilityZone(String availabilityZone) {
        getRequest().setAvailabilityZone(availabilityZone);
        return this;
    }
}
