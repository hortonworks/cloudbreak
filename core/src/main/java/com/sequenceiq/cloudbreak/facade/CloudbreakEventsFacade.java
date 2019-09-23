package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

public interface CloudbreakEventsFacade {

    List<CloudbreakEventsJson> retrieveEventsForWorkspace(Workspace workspace, Long since);

    Page<CloudbreakEventsJson> retrieveEventsByStack(Long stackId, Pageable pageable);

}
