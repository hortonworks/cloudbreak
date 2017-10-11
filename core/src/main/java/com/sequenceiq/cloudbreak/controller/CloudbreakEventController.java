package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.EventEndpoint;
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;

@Component
public class CloudbreakEventController implements EventEndpoint {

    @Autowired
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public List<CloudbreakEventsJson> get(Long since) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return cloudbreakEventsFacade.retrieveEvents(user.getUserId(), since);
    }

    @Override
    public List<CloudbreakEventsJson> getByStack(Long stackId) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return cloudbreakEventsFacade.retrieveEventsByStack(user.getUserId(), stackId);
    }

}
