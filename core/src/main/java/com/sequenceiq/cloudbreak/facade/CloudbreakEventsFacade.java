package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

public interface CloudbreakEventsFacade {

    List<CloudbreakEventV4Response> retrieveEventsForWorkspace(Workspace workspace, Long since);

    List<CloudbreakEventV4Response> retrieveEventsByStack(Long stackId);

}
