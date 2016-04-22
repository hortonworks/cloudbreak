package com.sequenceiq.it.mock.restito.ambari;

import com.sequenceiq.it.mock.restito.RestitoStub;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class AmbariAdminPutStub extends RestitoStub {
    public static final String PATH = "/users/admin";

    @Override
    public Condition getCondition() {
        return Condition.put(AMBARI_API_ROOT + PATH);
    }

    @Override
    public Action getAction() {
        return Action.ok();
    }
}
