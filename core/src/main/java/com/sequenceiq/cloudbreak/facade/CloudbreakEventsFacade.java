package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;

public interface CloudbreakEventsFacade {

    List<CloudbreakEventV4Response> retrieveEventsForWorkspace(Long workspaceId, Long since);

    Page<CloudbreakEventV4Response> retrieveEventsByStack(Long stackId, Pageable pageable);

    List<CloudbreakEventV4Response> retrieveEventsForWorkspaceByStack(Long workspaceId, String stackName);

}
