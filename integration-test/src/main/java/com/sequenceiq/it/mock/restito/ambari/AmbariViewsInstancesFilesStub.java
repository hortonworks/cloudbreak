package com.sequenceiq.it.mock.restito.ambari;

import java.util.regex.Pattern;

import org.glassfish.grizzly.http.Method;

import com.sequenceiq.it.mock.restito.RestitoStub;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class AmbariViewsInstancesFilesStub extends RestitoStub {
    public static final String PATH = "/views/.*/versions/1.0.0/instances/.*";

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.POST), Condition.matchesUri(Pattern.compile(AMBARI_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        return Action.ok();
    }
}
