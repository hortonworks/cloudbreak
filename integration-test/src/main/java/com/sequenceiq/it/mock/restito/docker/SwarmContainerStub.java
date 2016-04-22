package com.sequenceiq.it.mock.restito.docker;

import java.util.regex.Pattern;

import org.glassfish.grizzly.http.Method;

import com.sequenceiq.it.mock.restito.RestitoStub;
import com.sequenceiq.it.mock.restito.docker.model.InspectContainerResponse;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class SwarmContainerStub extends RestitoStub {

    public static final String PATH = "/containers/.*/json";

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.GET), Condition.matchesUri(Pattern.compile(SWARM_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        return Action.composite(Action.ok(), Action.contentType("application/json"), Action.stringContent(convertToJson(getResponse())));
    }

    private InspectContainerResponse getResponse() {
        InspectContainerResponse inspectContainerResponse = new InspectContainerResponse();
        inspectContainerResponse.setId("id");
        InspectContainerResponse.ContainerState state = new InspectContainerResponse.ContainerState();
        state.setRunning(true);
        inspectContainerResponse.setState(state);
        return inspectContainerResponse;
    }
}
