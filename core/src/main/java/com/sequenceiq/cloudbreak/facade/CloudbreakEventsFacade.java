package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.model.CloudbreakEventsJson;

public interface CloudbreakEventsFacade {

    List<CloudbreakEventsJson> retrieveEvents(String user, Long since);

}
