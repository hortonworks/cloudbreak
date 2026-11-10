package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerRequest;

final class ChangeInstanceTypeHandlerTestSupport {

    static final Long RESOURCE_ID = 1L;

    static final String CURRENT_INSTANCE_TYPE = "db.m5.large";

    static final String TARGET_INSTANCE_TYPE = "db.m5.xlarge";

    static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:tenant:environment:env";

    static final String REGION = "us-west-1";

    static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential("id", "name", "account");

    static final DatabaseStack DATABASE_STACK = new DatabaseStack(null, DatabaseServer.builder().withServerId("dbserver").build(), Map.of(), "");

    private ChangeInstanceTypeHandlerTestSupport() {
    }

    static CloudContext cloudContext(String platform) {
        return CloudContext.Builder.builder()
                .withId(RESOURCE_ID)
                .withPlatform(platform)
                .withVariant(platform)
                .withLocation(location(region(REGION), availabilityZone("az")))
                .build();
    }

    static DatabaseServerInstanceTypeState state(String instanceType, boolean modificationInProgress) {
        return new DatabaseServerInstanceTypeState(instanceType, 100L, false, modificationInProgress);
    }

    static HandlerEvent<ChangeInstanceTypeHandlerRequest> handlerEvent(String selector, CloudContext cloudContext,
            DatabaseServerInstanceTypeState liveState) {
        ChangeInstanceTypeHandlerRequest request = new ChangeInstanceTypeHandlerRequest(selector, cloudContext, CLOUD_CREDENTIAL, DATABASE_STACK,
                ENVIRONMENT_CRN, TARGET_INSTANCE_TYPE, liveState);
        return new HandlerEvent<>(Event.wrap(request));
    }
}
