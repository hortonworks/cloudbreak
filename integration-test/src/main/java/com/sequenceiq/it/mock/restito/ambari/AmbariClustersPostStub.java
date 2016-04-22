package com.sequenceiq.it.mock.restito.ambari;

import java.util.regex.Pattern;

import org.glassfish.grizzly.http.Method;

import com.sequenceiq.it.mock.restito.RestitoStub;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class AmbariClustersPostStub extends RestitoStub {
    public static final String PATH = "/clusters/.*";

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.POST), Condition.matchesUri(Pattern.compile(AMBARI_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        return Action.ok();
    }
}
