package com.sequenceiq.it.mock.restito.docker;

import org.glassfish.grizzly.http.Method;

import com.sequenceiq.it.mock.restito.RestitoStub;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class DockerInfoGetStub extends RestitoStub {

    public static final String PATH = "/info";

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.GET), Condition.uri(DOCKER_API_ROOT + PATH));
    }

    @Override
    public Action getAction() {
        return Action.ok();
    }

}
