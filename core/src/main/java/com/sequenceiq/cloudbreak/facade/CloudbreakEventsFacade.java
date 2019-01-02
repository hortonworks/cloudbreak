package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;

public interface CloudbreakEventsFacade {

    List<CloudbreakEventV4Response> retrieveEventsForWorkspace(Long workspaceId, Long since);

    List<CloudbreakEventV4Response> retrieveEventsByStack(Long stackId);

    List<CloudbreakEventV4Response> retrieveEventsForWorkspaceByStack(Long workspaceId, String stackName);

}
