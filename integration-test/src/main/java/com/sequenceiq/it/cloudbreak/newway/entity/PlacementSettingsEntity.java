package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.model.v2.PlacementSettings;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class PlacementSettingsEntity extends AbstractCloudbreakEntity<PlacementSettings, PlacementSettings, PlacementSettingsEntity> {

    protected PlacementSettingsEntity(TestContext testContext) {
        super(new PlacementSettings(), testContext);
    }

    @Override
    public CloudbreakEntity valid() {
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
