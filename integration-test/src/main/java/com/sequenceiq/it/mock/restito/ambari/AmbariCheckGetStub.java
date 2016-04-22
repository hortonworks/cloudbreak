package com.sequenceiq.it.mock.restito.ambari;

import com.sequenceiq.it.mock.restito.RestitoStub;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class AmbariCheckGetStub extends RestitoStub {

    public static final String PATH = "/check";

    @Override
    public Condition getCondition() {
        return Condition.get(AMBARI_API_ROOT + PATH);
    }

    @Override
    public Action getAction() {
        return Action.composite(Action.ok(), Action.stringContent("RUNNING"));
    }
}
