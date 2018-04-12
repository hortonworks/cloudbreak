package com.sequenceiq.cloudbreak.facade;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;

import java.util.List;

public interface CloudbreakEventsFacade {

    List<CloudbreakEventsJson> retrieveEvents(String user, Long since);

    List<CloudbreakEventsJson> retrieveEventsByStack(String user, Long stackId);

}
