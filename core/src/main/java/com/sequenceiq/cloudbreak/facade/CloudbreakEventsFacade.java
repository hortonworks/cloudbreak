package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;

public interface CloudbreakEventsFacade {

    List<CloudbreakEventsJson> retrieveEvents(String user, Long since);

    List<CloudbreakEventsJson> retrieveEventsByStack(IdentityUser owner, Long stackId);

}
