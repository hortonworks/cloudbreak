package com.sequenceiq.it.cloudbreak.dto.distrox.externaldatabase;

import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class DistroXExternalDatabaseTestDto extends AbstractCloudbreakTestDto<DistroXDatabaseRequest, DistroXDatabaseResponse, DistroXExternalDatabaseTestDto> {

    public DistroXExternalDatabaseTestDto(TestContext testContext) {
        super(new DistroXDatabaseRequest(), testContext);
    }

    public DistroXExternalDatabaseTestDto withAvailabilityType(DistroXDatabaseAvailabilityType databaseAvailabilityType) {
        getRequest().setAvailabilityType(databaseAvailabilityType);
        return this;
    }

    @Override
    public DistroXExternalDatabaseTestDto valid() {
        return withAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);
    }
}
