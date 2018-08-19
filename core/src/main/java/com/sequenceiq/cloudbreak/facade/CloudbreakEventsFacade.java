package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.organization.Organization;

public interface CloudbreakEventsFacade {

    List<CloudbreakEventsJson> retrieveEventsForOrganiztion(Organization organization, Long since);

    List<CloudbreakEventsJson> retrieveEventsByStack(Long stackId);

}
