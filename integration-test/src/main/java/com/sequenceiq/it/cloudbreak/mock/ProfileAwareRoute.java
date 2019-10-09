package com.sequenceiq.it.cloudbreak.mock;

import java.util.Set;

import com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock;
import com.sequenceiq.it.cloudbreak.spark.StatefulRoute;

import spark.Request;
import spark.Response;
import spark.Route;

public class ProfileAwareRoute implements StatefulRoute {

    private final Route happyResponseHandler;

    private final StatefulRoute statefulHappyResponseHandler;

    private final Set<String> activeProfiles;

    private int callCounter;

    public ProfileAwareRoute(Route happyResponseHandler, Set<String> activeProfiles) {
        this.happyResponseHandler = happyResponseHandler;
        statefulHappyResponseHandler = null;
        this.activeProfiles = activeProfiles;
    }

    public ProfileAwareRoute(StatefulRoute statefulHappyResponseHandler, Set<String> activeProfiles) {
        happyResponseHandler = null;
        this.statefulHappyResponseHandler = statefulHappyResponseHandler;
        this.activeProfiles = activeProfiles;
    }

    @Override
    public Object handle(Request request, Response response, DefaultModel model) throws Exception {
        if (activeProfiles.contains(ClouderaManagerMock.PROFILE_RETURN_HTTP_500) && callCounter == 0) {
            response.body("Mocked HTTP 500.");
            response.status(500);
            callCounter++;
            return response;
        } else {
            callCounter++;
            return happyResponseHandler == null ? statefulHappyResponseHandler.handle(request, response, model)
                    : happyResponseHandler.handle(request, response);
        }
    }
}
