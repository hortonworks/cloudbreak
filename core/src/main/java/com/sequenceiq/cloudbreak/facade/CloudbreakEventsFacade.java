package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.api.model.event.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

public interface CloudbreakEventsFacade {

    List<CloudbreakEventsJson> retrieveEventsForWorkspace(Workspace workspace, Long since);

    List<CloudbreakEventsJson> retrieveEventsByStack(Long stackId);

}
